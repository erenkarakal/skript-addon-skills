# Property expressions

Use these when the syntax is `property of %type%` / `%type%'s property`.

Template: `references/templates/src/main/java/com/example/exampleaddon/elements/expressions/ExprPropertyExample.java`

## Skeleton

```java
public class ExprFoo extends SimplePropertyExpression<Player, String> {

    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.EXPRESSION,
            infoBuilder(ExprFoo.class, String.class, "foo", "players", false)
                .supplier(ExprFoo::new)
                .build()
        );
    }

    @Override
    public @Nullable String convert(Player player) {
        return player.getName();
    }

    @Override
    protected String getPropertyName() {
        return "foo";
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }
}
```

`infoBuilder` is a static helper on `PropertyExpression`, inherited by subclasses. Signature:

```java
infoBuilder(Class<E> expressionClass, Class<T> returnType,
            String property, String fromType, boolean isDefault)
```

It generates two patterns:

- `"[the] " + property + " of %fromType%"`
- `"%fromType%'[s] " + property`

With `isDefault = true`, the `of ...` part becomes optional (`[of %type%]`) so a default expression can fill it.

Rules for `fromType`:

- Pass the **plural** Skript type name (`players`, `strings`, `worldguardregions`), because the expression may receive multiple objects.
- It must be a type that actually exists, or the pattern never parses. Custom types must be registered in `init`, before `load`.

Properties are registered at `PropertyExpression.DEFAULT_PRIORITY`, which sits below `PATTERN_MATCHES_EVERYTHING` and above `COMBINED`. Leave the priority alone unless you have a measured conflict.

## Implement only what you need

| Base | You implement |
| --- | --- |
| `SimplePropertyExpression<F, T>` | `convert(F)`, `getPropertyName()`, `getReturnType()` |
| `PropertyExpression<F, T>` | `get(Event, F[])`, `getPropertyName()`, `getReturnType()` |

`SimplePropertyExpression` also implements `Converter<F, T>`, so a `null` return from `convert` means "this source had no value". Returning `null` for an element is handled by the converter; returning a null **array** is not allowed.

`getPropertyName()` feeds the generated `toString` (`"<property> of <expr>"`). Do not return debug junk.

## Change support

Property expressions commonly support `set`/`add`/`remove`. Override `acceptChange`/`change` in the same way as a normal expression (see `03-expressions.md`), and read the source with `getExpr().getArray(event)`.

## When not to use a property expression

2.17 also has a **type-level property system**: `ClassInfo.property(Property, description, addon, handler)` with handlers such as `ExpressionPropertyHandler`, `ContainsHandler`, and the `org.skriptlang.skript.lang.properties` package.

Use it when the property is intrinsic to the type and should be available wherever that type appears, rather than being an expression you own. `skript-worldguard` registers `Property.NAME` and `Property.CONTAINS` this way on its region `ClassInfo`.

Do not build a property expression and a type-level property for the same thing — you will create two competing syntaxes.

## Pitfalls

- Pattern says `%players%` but the Java type is `Expression<Player>`: correct. Pattern says `%player%` (single) but you call `getArray`: also fine at runtime, but `isSingle` will be derived from the source expression.
- Overriding `isSingle()` in a property expression is almost always wrong; `PropertyExpression` derives it from the source expression.
- Registering a property expression in `load` while its `fromType` is registered in `load` too → parse failures. Types go in `init`.
