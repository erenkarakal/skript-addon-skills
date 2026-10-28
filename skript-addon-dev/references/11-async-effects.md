# Async effects

`ch.njol.skript.util.AsyncEffect` runs `execute(Event)` **off the main thread** and then resumes the trigger on the main thread, as if a delay had been inserted.

Template: `references/templates/src/main/java/com/example/exampleaddon/elements/effects/EffAsyncExample.java`
Real example: Skript's `EffLoadServerIcon` (2.17.1).

## Skeleton

```java
public class EffFooAsync extends AsyncEffect {

    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.EFFECT,
            SyntaxInfo.builder(EffFooAsync.class)
                .supplier(EffFooAsync::new)
                .addPattern("foo %string% asynchronously")
                .build()
        );
    }

    private Expression<String> input;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        getParser().setHasDelayBefore(Kleenean.TRUE);   // mandatory
        input = (Expression<String>) exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        // Async thread. No Bukkit state.
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "foo " + input.toString(event, debug) + " asynchronously";
    }
}
```

## The one mandatory line

```java
getParser().setHasDelayBefore(Kleenean.TRUE);
```

Skript's own Javadoc says so, and the class breaks without it: the parser believes the next statement runs immediately, so it may reorder, merge, or incorrectly reject code after your effect. This is not optional and not a warning.

## What Skript does

1. `walk(event)` removes the event's local variables from the thread-local store and backs them up.
2. Schedules `execute(event)` on an **async** scheduler task.
3. On that async thread it marks the event as delayed and restores the local variables.
4. Calls `execute(event)`.
5. If there is a next item, schedules `TriggerItem.walk(getNext(), event)` back on the **main** thread; otherwise removes the local variables.

So `execute` *can* read local variables: they are restored for that event on the async thread. It must not assume anything else.

## Thread safety rules

Assume nothing is thread-safe.

Safe in `execute`:

- plain Java work: file I/O, HTTP, database, hashing, math, serialization
- reading `String`/`Number`/`Boolean` expressions and literals
- mutating your own concurrent/thread-confined data structures
- logging through `java.util.logging`

Not safe in `execute`:

- calling Bukkit API (entities, blocks, worlds, inventories, schedulers)
- mutating `ItemStack`s, `Location`-attached state, or chat components in place
- reading Skript variables through APIs that touch Bukkit
- firing events, sending messages, spawning entities

If you need main-thread state, the options are:

1. **Read only immutable values** (`%string%`, `%number%`, `%file%`) and do the slow work async.
2. **Do not use `AsyncEffect`.** Use a normal `Effect`, snapshot what you need on the main thread, schedule the heavy work yourself, and schedule the continuation back with `Bukkit.getScheduler().runTask(...)`. You then also own the delay semantics — set `hasDelayBefore` yourself.

`AsyncEffect` is the right tool for "slow thing with plain inputs". It is the wrong tool for "read a player's inventory, then write it back".

## Delays and ordering

- The effect itself counts as a delay for everything after it.
- An `AsyncEffect` may be used after a `wait`. It adds its own tick and resumes the trigger; the event is already marked delayed, and marking it again is a no-op. Skript's own `EffLoadServerIcon` accepts delayed use.
- Reject delayed use only when the effect reads state that the delay invalidates (event locals it did not snapshot, a block or entity captured before the `wait`):

```java
if (isDelayed == Kleenean.TRUE) {
    Skript.error("This effect cannot be used after a delay");
    return false;
}
```

- `execute` gets no return value. Look failures up in your plugin logger; you cannot "return failure" to the trigger. Avoid `error(...)` unless you have checked that it attributes correctly from an async thread.
- The resumed trigger runs on the next main-thread tick, so code after the effect observes the world one tick later.

## Reporting multi-step or progress work

Because the trigger is paused until `execute` returns, a long `execute` blocks **that trigger**, not the server — but it also blocks nothing else, since it is async. Do not use `AsyncEffect` as a fire-and-forget scheduler: it deliberately serialises the script's continuation after the work completes.

If you want fire-and-forget, use a normal effect and `runTaskAsynchronously` yourself.

## Pitfalls

- Missing `setHasDelayBefore(Kleenean.TRUE)` → parser mis-orders subsequent statements.
- Touching Bukkit in `execute` → intermittent `IllegalStateException`, corrupted world state, or silent data loss. It will not fail loudly on a test server with one player.
- Throwing from `execute` — the scheduler logs it and the script cannot catch it. Log and return.
- Caching an `Entity`/`Player` and using it off-thread later. Copy an ID or a plain value instead.
- Forgetting that the continuation runs a tick later; assertions immediately after the effect see the pre-work state.
- Reload/disable during `execute`: the plugin may be disabled when the continuation runs. Check `Skript.getInstance().isEnabled()` (as `AsyncEffect` itself does) or guard against your plugin being disabled.
- Testing async behaviour in a Skript test script: the runner shuts down one tick after the test event, so delayed code may never run. Assert the parse-time contract (`has delay before is true`) instead — see `16-unit-testing.md`.
