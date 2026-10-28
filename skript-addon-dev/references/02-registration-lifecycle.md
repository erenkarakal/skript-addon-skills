# Registration and lifecycle

## The four moving parts

| Type | Role |
| --- | --- |
| `ch.njol.skript.Skript` | The plugin. Static entry point: `Skript.instance()`, `Skript.getVersion()` |
| `org.skriptlang.skript.Skript` | API interface. `registerAddon(Class, String)`, `addon(String)`, `addons()` |
| `SkriptAddon` | Your addon handle. `syntaxRegistry()`, `localizer()`, `loadModules(...)`, `name()` |
| `AddonModule` | Loading unit. `canLoad` → `init` → `load` |

## Registration flow

```java
// onEnable, after the Skript presence + version checks
SkriptAddon addon = Skript.instance().registerAddon(MyAddon.class, "my-addon");
addon.loadModules(this);
```

`registerAddon` takes the **source class** (usually your main class) and the addon name. The name is used for origins and documentation; use the same kebab-case ID as the plugin.

Do **not** use `Skript.registerAddon(JavaPlugin)` — deprecated.

## init vs load

```java
@Override
public void init(SkriptAddon addon) {
    // Types, comparators, converters, serializers: anything syntax may reference while parsing.
    Types.register();
}

@Override
public void load(SkriptAddon addon) {
    // Syntax and function registration only. Elements pulls the syntax registry
    // (and, if needed, the event value registry) off the addon.
    Elements.register(addon);
}
```

`init` always runs before `load`, across all modules. A syntax element that uses a custom type will fail at parse time if the type was registered in `load`, because scripts may parse between the two phases.

## Registering syntax

Registration keys:

| Element | Key |
| --- | --- |
| Expression | `SyntaxRegistry.EXPRESSION` |
| Effect | `SyntaxRegistry.EFFECT` |
| Condition | `SyntaxRegistry.CONDITION` |
| Structure | `SyntaxRegistry.STRUCTURE` |
| Section (body-only) | `SyntaxRegistry.SECTION` |
| Effect section (effect + optional body) | `SyntaxRegistry.SECTION` |
| Event | `BukkitSyntaxInfos.Event.KEY` |

A `SectionExpression` is an **expression**, not a `Section`. Register it under `EXPRESSION`; Skript wraps it internally. See `10-expression-sections.md`.

An `EffectSection` is a `Section` that may also be used without a body. Register it under `SECTION`, not `EFFECT`, and implement the six-argument `init` (the four-argument one sets `hasSection()` for you). See `12-effect-sections.md`.

Pattern per element type:

```java
// Expression
registry.register(
    SyntaxRegistry.EXPRESSION,
    SyntaxInfo.Expression.builder(ExprFoo.class, String.class)
        .supplier(ExprFoo::new)
        .addPattern("foo of %player%")
        .build()
);

// Effect / Condition
registry.register(
    SyntaxRegistry.EFFECT,
    SyntaxInfo.builder(EffFoo.class)
        .supplier(EffFoo::new)
        .addPatterns("do foo", "do foo to %players%")
        .build()
);

// Event
registry.register(
    BukkitSyntaxInfos.Event.KEY,
    BukkitSyntaxInfos.Event.builder(EvtFoo.class, "Foo")
        .supplier(EvtFoo::new)
        .addEvent(PlayerJoinEvent.class)
        .addPatterns("foo")
        .addSince("1.0")
        .build()
);
```

Registration is global and permanent for the server's lifetime.

Functions do **not** go through `SyntaxRegistry`. Register them with `Functions.register(...)` in the same `load` phase. See `13-functions.md`.

## Origins

For a single-module addon, registering straight into `addon.syntaxRegistry()` is fine. For multi-module addons, use the origin-aware helpers so documentation and diagnostics attribute syntax correctly:

```java
moduleRegistry(addon)                       // registry that applies this module's origin
register(addon, ExprFoo::register, EffBar::register)   // helper that does the above
```

`SyntaxRegistry.withOrigin(registry, origin)` exists if you need it manually.

## What must be registered where

| Thing | Phase | API |
| --- | --- | --- |
| `ClassInfo` (types) | init | `Classes.registerClass(new ClassInfo<>(...))` |
| Comparators | init | `Comparators.registerComparator(...)` |
| Converters | init | `Converters.registerConverter(...)` |
| Event values (`event-...`) | load or init | `addon.registry(EventValueRegistry.class).register(EventValue.builder(...).build())` |
| Functions | load | `Functions.register(DefaultFunction.builder(...).build(...))` |
| Syntax | load | `addon.syntaxRegistry().register(...)` |

## Never throw

An exception that leaves addon code is not a normal error path. It becomes a server-side stack trace and aborts whatever Skript was doing.

| Where | What happens | What to do |
| --- | --- | --- |
| `AddonModule.init` / `load` | propagates out of `loadModules` and `onEnable`; later modules never run and the plugin may be disabled | log with `Skript.error`/`Skript.warning` (or the plugin logger) and skip the failing registration |
| syntax `init` (parse time) | the script line fails with an internal error instead of a clean message | `Skript.error("...")` or `Skript.warning("...")`, then `return false` |
| runtime (`get`, `execute`, `check`, function lambda) | `TriggerItem.run` catches it, prints a stack trace, and stops the rest of the user's trigger | return `null`, an empty array, `false`, or no-op |

- `Skript.error`/`Skript.warning` are parse-time reporting. Use them in syntax `init` and in registration code.
- At runtime, stop silently. `Effect`, `Condition`, and `SimpleExpression` also expose instance `error(...)`/`warning(...)`, which log against the current line without throwing. Use them only when the failure deserves a log, never as control flow.
- Catch checked exceptions from I/O, reflection, and third-party plugins at your boundary, log them, and return a safe value.
- Do not use `Objects.requireNonNull` or `Preconditions.checkNotNull` on values that came from a script or another plugin. Validate and return.
- `Event#getHandlers()` must return a `HandlerList`. Return a shared static instance; do not throw from it.

## Timing and reload behaviour

- Registration must happen during your `onEnable`. Skript loads scripts after plugins enable.
- Java registrations **cannot be undone** by `/skript reload` or a plugin reload. Reloading leaves stale or duplicate syntax.
- If you change registration code, restart the server. Say so explicitly when handing work back.
- Registration closes once Skript stops accepting it. Legacy entry points (`Classes`, `Comparators`, `Converters`, `Functions`, and `Skript.register*`) call `Skript.checkAcceptRegistrations()` and throw `SkriptAPIException("Registration can only be done during plugin initialization")`.
- `syntaxRegistry().register(...)` has no such guard. A late call succeeds but cannot affect scripts that were already parsed, so the syntax appears broken until a full restart.

## Reflective registration (large addons)

`org.skriptlang.skript.util.ClassLoader` scans a package and can invoke a `register(SyntaxRegistry)` method per class. It is what `skript-worldguard` does. Rules:

- Import `org.skriptlang.skript.util.ClassLoader`, not `java.lang.ClassLoader` — the names collide.
- Every syntax class must expose `public static void register(SyntaxRegistry registry)`.
- A reflection failure is silent unless you log it. Log and fail loudly.
- For small and medium addons, an explicit `Elements.register(addon)` list is safer: a missing element becomes a compile error instead of a runtime gap.
