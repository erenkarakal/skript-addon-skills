# Section expressions

A **section expression** is an expression that can own a code block:

```skript
set {_bar} to a boss bar:
    set title of event-bossbar to "Hello"
    set color of event-bossbar to red
```

The body is *not* a normal trigger: it belongs to the expression, and the expression decides whether and when to run it.

Use this when the syntax naturally reads as "produce a value, configured by a block". Do **not** use it for things that are statements — that is an `Effect` or an `EffectSection`.

Template: `references/templates/src/main/java/com/example/exampleaddon/elements/expressions/ExprSecExample.java`
Canonical real implementation: Skript's own `ExprSecCreateBossBar` (2.17.1).

## Skeleton

```java
public class ExprSecFoo extends SectionExpression<Foo> {

    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.EXPRESSION,          // NOT SECTION
            SyntaxInfo.Expression.builder(ExprSecFoo.class, Foo.class)
                .supplier(ExprSecFoo::new)
                .addPattern("a new foo")
                .build()
        );
    }

    @Override
    public boolean init(Expression<?>[] expressions, int pattern, Kleenean delayed,
            ParseResult result, @Nullable SectionNode node,
            @Nullable List<TriggerItem> triggerItems) {
        if (node != null)
            loadCode(node);                     // or loadOptionalCode(node)
        return true;
    }

    @Override
    protected Foo @Nullable [] get(Event event) {
        if (!runSection(event))
            return new Foo[0];
        return new Foo[]{new Foo()};
    }

    @Override
    public boolean isSingle() { return true; }

    @Override
    public Class<? extends Foo> getReturnType() { return Foo.class; }

    @Override
    public String toString(@Nullable Event event, boolean debug) { return "a new foo"; }
}
```

## The `init` you must implement

`SectionExpression` declares:

```java
public abstract boolean init(Expression<?>[] expressions, int pattern, Kleenean delayed,
        ParseResult result, @Nullable SectionNode node, @Nullable List<TriggerItem> triggerItems);
```

and marks the ordinary four-argument `init` as **`final`**. Overriding the four-argument version will not compile.

- `node` is `null` when the expression was **not** used to start a section (for example `set {_x} to a new foo` without a colon). Handle it.
- `triggerItems` is the preceding trigger code; it is only non-null together with `node`.
- `delayed` is `Kleenean.TRUE` if the expression is used after a `wait`/delay.

## Registration key

`SyntaxRegistry.EXPRESSION`. `SyntaxRegistry.SECTION` is for `Section` subclasses (`ch.njol.skript.lang.Section`), not for section expressions. The `SectionExpression` internally creates an `ExpressionSection` wrapper to claim the block; you never register that class.

## `isSectionOnly()`

| Return | Behaviour |
| --- | --- |
| `true` | The body is mandatory. Skript errors with "This expression requires a section." when the user omits it. |
| `false` (default) | Usable both bare and with a body. `node` is null in the bare form. |

Prefer `true` unless the bare form is genuinely meaningful (Skript's boss bar expression allows it, because a bar without configuration is valid).

## Limiting the section

| Method | Effect |
| --- | --- |
| `loadCode(SectionNode)` | Parses the body and links it to this expression. Does **not** touch `hasDelayBefore`. |
| `loadOptionalCode(SectionNode)` | Same, but sets `hasDelayBefore` to `UNKNOWN` if the body may delay — use it when the body can run zero or more times. |
| `setTriggerItems(List<TriggerItem>)` | Use a pre-parsed item list instead of a node. |
| `runSection(Event)` | Executes the body. Returns `false` if an exception was thrown. |
| `getAsSection()` | Exposes the internal `Section` wrapper. Rarely needed. |

Call `loadCode`/`loadOptionalCode` in `init`, and `runSection` in `get` (or later, from your own event).

## Recipe: build an object, then let the body configure it

The body usually needs access to the object being built. Use a dedicated `Event` subclass plus an event value:

```java
public class CreateFooEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Foo foo;
    CreateFooEvent(Foo foo) { this.foo = foo; }
    public Foo getFoo() { return foo; }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
```

The event is never dispatched by Bukkit; it only carries values into the body.

Register the event value so the body can say `event-foo` (2.17 API; `EventValues.registerEventValue` is deprecated):

```java
eventValues.register(
    EventValue.builder(CreateFooEvent.class, Foo.class)
        .patterns("[the] event-foo")
        .getter(CreateFooEvent::getFoo)
        .build()
);
```

`eventValues` is `addon.registry(EventValueRegistry.class)`. See `12-effect-sections.md` for how to thread it through registration.

Load the body with the custom event in scope, then run it with the caller's local variables carried over:

```java
private @Nullable Trigger trigger;

@Override
public boolean init(Expression<?>[] expressions, int pattern, Kleenean delayed,
        ParseResult result, @Nullable SectionNode node, @Nullable List<TriggerItem> triggerItems) {
    if (node == null)
        return true;

    trigger = SectionUtils.loadLinkedCode("create foo", (beforeLoading, afterLoading) ->
        loadCode(node, "create foo", beforeLoading, afterLoading, CreateFooEvent.class));
    return trigger != null;
}

@Override
protected Foo[] get(Event event) {
    Foo foo = new Foo();
    if (trigger != null) {
        CreateFooEvent fooEvent = new CreateFooEvent(foo);
        Variables.withLocalVariables(event, fooEvent, () -> TriggerItem.walk(trigger, fooEvent));
    }
    return new Foo[]{foo};
}
```

Why each piece:

- `SectionUtils.loadLinkedCode` shares type hints with the enclosing code and **rejects delays** inside the body. Use `loadDelayableLinkedCode` only if you can actually handle a delayed body.
- Passing `beforeLoading`/`afterLoading` through is mandatory; `loadLinkedCode` relies on them.
- `Variables.withLocalVariables(outer, inner, runnable)` makes the caller's `{_local}` variables visible in the body. Without it, the body sees none of them.
- `HandlerList` must be returned but must never be used: this event is never fired through Bukkit. Never call `Bukkit.callEvent` on it.

If your body only produces side effects and needs no outer values, the simple `loadCode(node)` + `runSection(event)` form from the template is enough.

## Where a section expression can appear

Skript only lets it claim the block when:

1. the line is **not** already a `Section` or `EffectSection`, and
2. the line does not already contain another section expression.

Consequences:

- `set {_x} to a new foo:` works (`set` is an effect).
- `if a new foo: ...` does not — `if` is a section itself.
- Two section expressions on one line is an error; you cannot test which one wins.

## Delays

The body runs wherever you call `runSection`. If the body contains `wait`, the surrounding trigger must already know it can be delayed:

- `loadCode` does not propagate this. If the body can delay, the caller's `hasDelayBefore` is wrong.
- `loadOptionalCode` sets it to `UNKNOWN`, which is the safe answer for "may or may not run, may or may not delay".
- `SectionUtils.loadLinkedCode` forbids delays outright and reports a parse error instead — the safer default.

## Pitfalls

- Overriding the four-argument `init` → compile error (`final` in the superclass).
- Registering under `SECTION` → never parses as an expression.
- Ignoring `node == null` → `NullPointerException` at parse time when the user omits the body.
- Forgetting the custom event value → `event-foo` is unavailable in the body.
- Forgetting `Variables.withLocalVariables` → body cannot read the caller's locals.
- Running the body while the outer trigger is mid-execution and the body delays → ordering bugs.
- Calling `runSection` from `init` → runs at parse time, on the wrong thread, before the value exists.
- The body is not parsed at all until you call `loadCode`; invalid syntax inside it silently passes.
