# Events

Base class: `ch.njol.skript.lang.SkriptEvent`.
Registration: `BukkitSyntaxInfos.Event`, **not** `SyntaxInfo.builder(...)` — events need Bukkit event classes and their documentation inline.

Template: `references/templates/src/main/java/com/example/exampleaddon/elements/events/EvtExample.java`

## Skeleton

```java
public class EvtFoo extends SkriptEvent {

    public static void register(SyntaxRegistry registry) {
        registry.register(
            BukkitSyntaxInfos.Event.KEY,
            BukkitSyntaxInfos.Event.builder(EvtFoo.class, "Foo")
                .supplier(EvtFoo::new)
                .addEvent(PlayerJoinEvent.class)
                .addPatterns("foo")
                .addDescription("Called when ...")
                .addExample("on foo:\n\tsend \"hi\"")
                .addSince("1.0")
                .build()
        );
    }

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return event instanceof PlayerJoinEvent;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append("foo");
        return builder.toString();
    }
}
```

## Notes

- `init` receives `Literal<?>[]`, not `Expression<?>[]`: event arguments are parsed at script load before variables exist.
- `check(Event)` runs for every matching Bukkit event. Return `false` to filter (e.g. "on region enter" vs "on region leave" share one Bukkit event class).
- `addEvent(...)` can be called several times, or use `addEvents(...)`.
- Documentation lives in the builder (`addDescription`, `addExample`, `addSince`, `addKeywords`, `addRequiredPlugin`), not in `@Name`/`@Example` annotations. The builder name is the user-visible event name.
- Default `listeningBehavior` is `UNCANCELLED`. Use `ListeningBehavior.ANY` if you need cancelled events too. Check `SkriptEvent.ListeningBehavior` before relying on it.

## Event values (`event-player`, `event-world`, ...)

Register the values your event exposes in the same `register` method, using the 2.17 registry.
`register` needs the addon (not just the syntax registry) so it can reach `EventValueRegistry`:

```java
public static void register(SyntaxRegistry registry, EventValueRegistry eventValues) {
    // ... syntax registration ...

    eventValues.register(
        EventValue.builder(FooEvent.class, Player.class)
            .patterns("[the] event-player")
            .getter(FooEvent::getPlayer)
            .build()
    );
    eventValues.register(
        EventValue.builder(FooEvent.class, World.class)
            .getter(e -> e.getPlayer().getWorld())
            .build()
    );
}
```

```java
// In the module's load phase:
Elements.register(addon);   // passes addon.syntaxRegistry() and addon.registry(EventValueRegistry.class)
```

Omit `.patterns(...)` to get Skript's default name for the value class (`event-player` for `Player`,
`event-world` for `World`, ...). Add explicit patterns when the default is ambiguous or when you want
an extra name. Then the value resolves inside your event without an explicit expression.

`EventValues.registerEventValue(...)` still compiles in 2.17 but is deprecated for removal and emits a
build warning. Values are looked up by the **Bukkit event class**, which is why a custom
`RegionEnterLeaveEvent` works.

## Optional event arguments

Event patterns are parsed strictly: a typed argument that the user may omit must be nullable in the pattern. The helper is `BukkitSyntaxInfos.fixPattern(String)`, which rewrites every `%type%` to `%-type%`. Apply it to patterns you build from user-supplied fragments, or write `%-type%` directly.

Example from `skript-worldguard`:

```java
String regionPattern = "[the] [worldguard] region[s] [named] %*strings% [(in|of) [[the] world] %-*string%]";
```

## Do not

- Do not register events via `SyntaxInfo.builder(...)`; they will not be associated with Bukkit events.
- Do not call `Skript.registerEvent(...)` in new code; it is the legacy path.
- Do not do work in `check`; it runs on every event instance. Do the work in the script trigger.
