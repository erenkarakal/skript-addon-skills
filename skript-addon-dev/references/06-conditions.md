# Conditions

Two options:

| Base | Use when |
| --- | --- |
| `ch.njol.skript.conditions.base.PropertyCondition<T>` | The condition is "X is <property>" (most cases) |
| `ch.njol.skript.lang.Condition` | Arbitrary logic, multiple expressions, custom negation |

Template: `references/templates/src/main/java/com/example/exampleaddon/elements/conditions/CondExample.java`

## PropertyCondition

```java
public class CondFoo extends PropertyCondition<Player> {

    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.CONDITION,
            infoBuilder(CondFoo.class, PropertyType.BE, "foo", "players")
                .supplier(CondFoo::new)
                .build()
        );
    }

    @Override
    public boolean check(Player player) {
        return player.isFlying();
    }

    @Override
    protected String getPropertyName() {
        return "foo";
    }
}
```

`infoBuilder` signature: `(Class, PropertyType, String property, String type)`.

`PropertyType` controls the generated patterns:

| Value | Meaning | Example |
| --- | --- | --- |
| `PropertyType.BE` | "is / are <property>" | `player is flying` |
| `PropertyType.HAVE` | "has / have <property>" | `player has permission` |
| `PropertyType.CAN` | "can <property>" | `player can fly` |
| `PropertyType.WILL` | "will <property>" | `player will fly` |

Each type also generates the matching negated pattern (`isn't`, `can't`, `doesn't have`, `won't`). Pass the plural type name for `type`; a `%` in it throws `SkriptAPIException`.

## Plain Condition

```java
public class CondBar extends Condition {

    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.CONDITION,
            SyntaxInfo.builder(CondBar.class)
                .supplier(CondBar::new)
                .addPattern("bar %number% is above %number%")
                .build()
        );
    }

    private Expression<Number> value, threshold;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        value = (Expression<Number>) exprs[0];
        threshold = (Expression<Number>) exprs[1];
        return true;
    }

    @Override
    public boolean check(Event event) {
        Number value = this.value.getSingle(event);
        Number threshold = this.threshold.getSingle(event);
        if (value == null || threshold == null)
            return false;
        return value.doubleValue() > threshold.doubleValue();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "bar " + value.toString(event, debug) + " is above " + threshold.toString(event, debug);
    }
}
```

## Negation

`PropertyCondition` handles negation itself: `infoBuilder` registers both forms, `init` calls `setNegated(matchedPattern == 1)`, and `check(Event)` delegates to `expr.check(event, this, isNegated())`. `check(T)` must answer only the positive form.

A plain `Condition` gets none of that. `Condition#run` returns `check(event)` unchanged, so negation is entirely up to you. If you register negated patterns, call `setNegated(...)` in `init` and apply `isNegated()` yourself, usually as `isNegated() ^ result`. Otherwise register only the positive pattern. `setNegated` is `protected final`; `isNegated()` is `public final`.

## Rules

- Return `false`, not `null`, for "unknown". Conditions are boolean.
- `PropertyCondition` already implements `toString`; do not override it unless the property name is misleading.
- `getPropertyName()` is user-visible in errors and docs. Keep it the same as the registered property string.
- Register in `load`; the type in `type` must already exist (registered in `init`).
