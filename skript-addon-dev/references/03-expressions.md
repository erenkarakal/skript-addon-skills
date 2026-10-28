# Expressions

Base class: `ch.njol.skript.lang.util.SimpleExpression<T>` for values that do not come from another expression.
Use `PropertyExpression` instead when the syntax is "property of X" — see `04-property-expressions.md`.

Template: `references/templates/src/main/java/com/example/exampleaddon/elements/expressions/ExprExample.java`

## Skeleton

```java
public class ExprFoo extends SimpleExpression<Integer> {

    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.EXPRESSION,
            SyntaxInfo.Expression.builder(ExprFoo.class, Integer.class)
                .supplier(ExprFoo::new)
                .addPattern("foo of %player%")
                .build()
        );
    }

    private Expression<Player> player;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        player = (Expression<Player>) exprs[0];
        return true;
    }

    @Override
    protected Integer[] get(Event event) {
        Player player = this.player.getSingle(event);
        if (player == null)
            return new Integer[0];
        return new Integer[]{player.getFoodLevel()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "foo of " + player.toString(event, debug);
    }
}
```

## The three phases (do not conflate)

| Method | When | What belongs there |
| --- | --- | --- |
| `init(Expression<?>[], int, Kleenean, ParseResult)` | Script load | Store expressions, read parse tags, validate |
| `get(Event)` | Runtime | Read current values |
| `toString(Event, boolean)` | Diagnostics | Re-parseable syntax |

Never capture the `Event`, a `Player`, or a `Trigger` in a field during `init`. `init` runs once per script load; `get` runs per execution.

## Patterns and Java types

| Pattern | Java side |
| --- | --- |
| `%player%` | `Expression<Player>` (may be a list) |
| `%-world%` | `Expression<World>`, **null** when omitted |
| `%players%` | `Expression<Player>` returning multiple values |
| `%*string%` | `Literal<String>` (literal only) |
| `%string%` | `Expression<String>` |

- A `-` before the type means **single, optional**. The array entry is `null` when omitted — always null-check.
- `getSingle(event)` on a list expression silently returns the first element. If the syntax is `%type%` (list), use `getArray(event)`.
- `isSingle()` must match the pattern and the declared return type. Returning `true` from `isSingle()` while `get()` returns several values produces confusing runtime behaviour.

## Return type

`getReturnType()` is used by the parser to decide whether your expression fits a slot. `SyntaxInfo.Expression.builder(Class, ReturnType)` also carries it, and the two must agree. Declare the narrowest real type (`Integer`, not `Object`).

## Optional / absent values

- Return an empty array, not an array containing `null`. Null elements are rejected by `PropertyExpression` and cause `NullPointerException`s in list handling.
- For "no value" semantics, `getSingle` may return `null`; for lists return `new T[0]`.

## Change support (`set`, `add`, `remove`, `delete`, `reset`)

A read-only expression needs nothing. To support modification:

```java
@Override
public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
    return switch (mode) {
        case SET, ADD, REMOVE, DELETE, RESET -> new Class[]{Integer.class};
        default -> null;
    };
}

@Override
public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
    int value = delta != null ? (Integer) delta[0] : 0;
    // apply
}
```

Returning `null` from `acceptChange` means "read-only"; Skript reports a clear parse error instead of failing at runtime. Do not accept a mode you do not implement.

## Delayed expressions

`isDelayed` is a `Kleenean`: `TRUE` means the expression is used in a delayed context (e.g. after `wait`). If you read event state, either reject delayed use in `init` or implement `setTime`/delay handling. Ignoring this produces wrong values after `wait`.

## Debugging

`debug` in `toString` requests a verbose representation. Keep `toString(event, false)` valid script syntax even when `event` is `null`.
