# Effects

Base class: `ch.njol.skript.lang.Effect`.

Template: `references/templates/src/main/java/com/example/exampleaddon/elements/effects/EffExample.java`

## Skeleton

```java
public class EffFoo extends Effect {

    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.EFFECT,
            SyntaxInfo.builder(EffFoo.class)
                .supplier(EffFoo::new)
                .addPatterns("foo %string%", "foo %string% to %players%")
                .build()
        );
    }

    private Expression<String> message;
    private @Nullable Expression<Player> targets;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        message = (Expression<String>) exprs[0];
        targets = (Expression<Player>) exprs[1];   // null when the optional part is absent
        return true;
    }

    @Override
    protected void execute(Event event) {
        String message = this.message.getSingle(event);
        if (message == null)
            return;
        // ...
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append("foo", message);
        if (targets != null)
            builder.append("to", targets);
        return builder.toString();
    }
}
```

## Rules

- `init` returns `false` on a parse error, after calling `Skript.error(...)` or `Skript.warning(...)`. Returning `false` silently gives the user "unable to parse" with no reason.
- `execute` has no return value. For runtime problems, stop silently. If the failure deserves a log, call `error("...")` or `warning("...")`; they report against the current script without throwing.
- Optional expressions (`%-type%` or a `[...]` block around a typed part) are `null` in `exprs`. Null-check every one; this is the most common `NullPointerException` in addons.
- Do not cache the `Event` in a field.
- Read values with `getSingle` for scalar syntax and `getArray` for list syntax, matching the pattern.
- `toString` should rebuild valid syntax with `SyntaxStringBuilder`, not concatenate debug text.

## Delayed effects

`isDelayed` tells you whether the statement runs after a `wait`. Effects that touch event state should usually reject delayed use:

```java
if (isDelayed == Kleenean.TRUE) {
    Skript.error("This effect cannot be delayed");
    return false;
}
```

For effects that intentionally schedule work, use `Delay`/`Bukkit.getScheduler()` deliberately and document it. Do not silently schedule on every call.

## Effects that must run inside a section/event

If the effect only makes sense inside certain events, check the parser:

```java
if (!getParser().isCurrentEvent(PlayerJoinEvent.class)) {
    Skript.error("This effect can only be used in a join event");
    return false;
}
```

## Timing

Effects run on the main server thread. If you need async work, schedule it explicitly and copy any Bukkit values you need first. Never touch Bukkit state from an async thread.

If the effect's whole purpose is slow off-thread work with plain inputs, use `AsyncEffect` — but read `11-async-effects.md` first. It pauses the trigger, runs `execute` off-thread, resumes on the next tick, and **requires** `getParser().setHasDelayBefore(Kleenean.TRUE)` in `init`. It is not a fire-and-forget scheduler.

If the effect may also take a code block (`foo:` with a body), it is an `EffectSection`, not an `Effect`. Read `12-effect-sections.md` before changing the base class.

## Anti-patterns

- Doing the work in `init` instead of `execute`.
- Using `Skript.error(...)` for a per-execution runtime message; `error(...)` inside the effect is the right scope.
- Accepting `%objects%` and blindly casting; the parser guarantees the declared type only if your pattern declares it.
