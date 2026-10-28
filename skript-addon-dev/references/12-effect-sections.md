# Effect sections

An **effect section** is one syntax that works in two shapes:

```skript
example section "hello"

example section "hello":
    send event-string to console
```

That is exactly what Skript's `spawn` does (`spawn a zombie` / `spawn a zombie: ...`), and `wait` does too. Use one when the body is optional and configures, wraps, or reacts to the effect.

| Kind | Base class | Registered under |
| --- | --- | --- |
| Effect with optional body | `EffectSection` | `SyntaxRegistry.SECTION` |
| Body-only statement | `Section` | `SyntaxRegistry.SECTION` |
| Expression with optional body | `SectionExpression` | `SyntaxRegistry.EXPRESSION` |

Template: `references/templates/src/main/java/com/example/exampleaddon/elements/effects/EffSecExample.java`
Canonical real implementations: `EffSecSpawn` (detached body + custom event) and `Delay` (detached body, delays allowed) in 2.17.1.

## Skeleton

```java
public class EffSecFoo extends EffectSection {

    public static void register(SyntaxRegistry registry, EventValueRegistry eventValues) {
        registry.register(
            SyntaxRegistry.SECTION,                     // NOT EFFECT
            SyntaxInfo.builder(EffSecFoo.class)
                .supplier(EffSecFoo::new)
                .addPattern("foo %string%")
                .build()
        );
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
            ParseResult parseResult, @Nullable SectionNode sectionNode,
            @Nullable List<TriggerItem> triggerItems) {
        if (hasSection()) {
            assert sectionNode != null;
            // link or load the body here; return false on a parse error
        }
        return true;
    }

    @Override
    protected @Nullable TriggerItem walk(Event event) {
        // do the effect, optionally run the body
        return super.walk(event, false);   // continue with the next statement
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) { ... }
}
```

## The `init` you must implement

`EffectSection` declares both signatures, exactly like `Section`:

```java
// 4-arg: do not override ("should not be overridden unless you know what you are doing")
@Override
public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed,
        ParseResult parseResult);

// 6-arg: abstract, this is the one you implement
public abstract boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed,
        ParseResult parseResult, @Nullable SectionNode sectionNode, @Nullable List<TriggerItem> triggerItems);
```

The 4-arg version is **not** `final`, but it is what sets `hasSection()` before calling your 6-arg `init`. Overriding it silently breaks `hasSection()` and can steal the section node from an enclosing element. Do not touch it.

In the 6-arg `init`:

- Prefer `hasSection()` over `sectionNode != null`. It is the documented contract, and it is already correct if a future API changes how the node is claimed.
- When `hasSection()` is true, `sectionNode` and `triggerItems` are non-null (the annotation is `@Nullable` because the no-body form passes `null`).
- `isDelayed` is `Kleenean.TRUE` when used after a `wait`. Only reject it if your syntax genuinely cannot follow a delay.

## Two ways to run the body

### Inline body (same event)

The body is parsed **into this section**, so it runs as part of the section's own trigger chain:

```java
@Override
public boolean init(Expression<?>[] exprs, ..., @Nullable SectionNode sectionNode, ...) {
    if (hasSection())
        loadCode(sectionNode);   // sets first/last on this section
    return true;
}

@Override
protected @Nullable TriggerItem walk(Event event) {
    // ... do the effect ...
    return super.walk(event, true);   // run the body now
}
```

- `loadCode(SectionNode)` also calls `setTriggerItems(...)`, which wires `last.setNext(getNext())`, so the body falls through to the next statement naturally.
- `super.walk(event, true)` returns `first` (or `getNext()` when the body is empty), so execution continues correctly either way.
- Best for "run this code right here, in my event context". A `delay` inside the body behaves like any other delay in the surrounding trigger.
- Limitation: the body sees the **caller's** event values only. You cannot inject new ones.

### Detached body (own event)

The body is parsed into a separate `Trigger` with its own event type, so you can expose values such as `event-string`:

```java
private @Nullable Trigger trigger;

@Override
public boolean init(Expression<?>[] exprs, ..., @Nullable SectionNode sectionNode, ...) {
    if (hasSection()) {
        trigger = SectionUtils.loadLinkedCode("foo", (beforeLoading, afterLoading) ->
            loadCode(sectionNode, "foo", beforeLoading, afterLoading, FooEvent.class));
        return trigger != null;   // false = error already reported
    }
    return true;
}

@Override
protected @Nullable TriggerItem walk(Event event) {
    if (trigger != null) {
        FooEvent fooEvent = new FooEvent(...);
        // outer event's locals -> fooEvent, run body, copy locals back
        Variables.withLocalVariables(event, fooEvent, () -> TriggerItem.walk(trigger, fooEvent));
    }
    return super.walk(event, false);
}
```

- The 5-arg `loadCode(node, name, beforeLoading, afterLoading, events...)` returns the `Trigger`; pass the custom event class as the vararg so the body is parsed under it.
- `SectionUtils.loadLinkedCode` shares type hints with the surrounding code and **rejects delays** in the body. Use `SectionUtils.loadDelayableLinkedCode` only if you actually handle a delayed body (Skript's `Delay` does).
- Passing `beforeLoading`/`afterLoading` through is mandatory; `loadLinkedCode` uses them to save/restore type hints and to detect delays. If your helper cannot pass them, it is the wrong helper.
- `Variables.withLocalVariables(outer, inner, runnable)` makes the caller's `{_local}` variables visible in the body and copies changes back. Without it the body cannot see the caller's locals.
- Best for "configure this object/context before I commit it" — the reason `spawn` runs the body *before* the entity is added to the world.

If the body both needs custom values and must run inline, use the detached form; it gives you control over *when* it runs. There is no way to inject event values into an inline body.

## The custom event

```java
public static class FooEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Foo foo;
    public FooEvent(Foo foo) { this.foo = foo; }
    public Foo getFoo() { return foo; }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
```

Register the value with the 2.17 registry, not the deprecated `EventValues` facade:

```java
eventValues.register(
    EventValue.builder(FooEvent.class, Foo.class)
        .patterns("[the] event-foo")
        .getter(FooEvent::getFoo)
        .build()
);
```

Get the registry from the addon (`addon.registry(EventValueRegistry.class)`) and thread it through your registration method. `EventValues.registerEventValue(...)` still works in 2.17 but is deprecated and emits a build warning.

## The `walk` contract

`TriggerSection#run(Event)` is **final** and throws `UnsupportedOperationException`. A section is never "run"; it is walked. Overriding `run` does not compile.

`super.walk(event, boolean run)` does two things: calls `debug(event, run)` and returns `run ? first : getNext()`.

| You return | Meaning |
| --- | --- |
| `super.walk(event, true)` | Execute the inline body now, then continue with the next statement. |
| `super.walk(event, false)` | Continue with the next statement; do not execute the body (you already ran it, or there is none). |
| `null` | Stop the trigger here — nothing after this section runs. |

A detached body cannot be returned — `TriggerItem.walk(trigger, event)` returns a `boolean` — so run it inside `walk` and then return `super.walk(event, false)`.

Rules:

- If you return the result of `super.walk(...)`, do **not** call `debug` yourself — it already did.
- If you return `getNext()` directly, call `debug(event, false)` first, otherwise `/skript debug` output is wrong.
- Never return `first` directly; the boolean parameter is the supported way to run an inline body.

## Delays

| Situation | What happens |
| --- | --- |
| Inline body with `wait` | Propagates as a normal delay of the containing trigger. Correct by default. |
| `loadLinkedCode` body with `wait` | Parse error: "Delays can't be used within a 'foo' section". |
| `loadDelayableLinkedCode` body with `wait` | Allowed. The caller must handle resumption of the returned trigger (Skript's `Delay` schedules it). |
| Your effect itself takes time | See `11-async-effects.md`; an `EffectSection` is not a substitute for an `AsyncEffect`. |

Do not set `getParser().setHasDelayBefore(Kleenean.TRUE)` merely because the syntax has a body. A section does not delay by itself: its body already reports anything it does. Set it only in a construct that pauses execution (like `wait`).

## Parsing an effect section from another element

If your element needs to consume an effect section nested inside its own syntax, use `EffectSection.parse(...)` rather than `Section.parse(...)`. It restricts parsing to `EffectSection` implementations and takes care of the "this line is a valid statement but cannot function as a section" error. Ordinary scripts never need this.

## Pitfalls

- Registering under `EFFECT` → the colon form never parses. `EffectSection` goes under `SECTION`.
- Using `SyntaxInfo.Expression.builder(...)` → type mismatch, it is not a `Section`.
- Overriding the 4-arg `init` → `hasSection()` is never set, sections are silently treated as plain effects.
- Checking `sectionNode != null` instead of `hasSection()` → works today, breaks if Skript changes node claiming.
- Forgetting `return trigger != null` after `loadLinkedCode` → a body with a parse error (such as a delay) still registers a half-built element.
- Forgetting `Variables.withLocalVariables` around a detached body → the body loses the caller's locals.
- Calling `TriggerItem.walk(trigger, event)` with the **outer** event rather than the custom one → `event-foo` resolves to null.
- Return a static `HandlerList` from `getHandlers()`; the event is only a value carrier, so never call `Bukkit.getPluginManager().callEvent(...)` on it.
- Running the body twice (both `walk(event, true)` and a manual `TriggerItem.walk`) → duplicate side effects that are hard to spot in small tests.

Related: `10-expression-sections.md` (expressions that own a block), `11-async-effects.md` (off-thread effects), `16-unit-testing.md` (testing sections and delay contracts).
