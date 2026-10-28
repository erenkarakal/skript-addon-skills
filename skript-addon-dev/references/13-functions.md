# Functions

Base API: `org.skriptlang.skript.common.function.DefaultFunction`.

Registry: `ch.njol.skript.lang.function.Functions.register(...)`.

Template: `references/templates/src/main/java/com/example/exampleaddon/elements/functions/ExampleFunction.java`

`SimpleJavaFunction` and `JavaFunction` are deprecated for removal since 2.13. Use `DefaultFunction` for new code.

## When a function is the right element

A function is called as `name(arguments)` inside any expression. Use one when scripts need a reusable operation with typed arguments and a return value.

| Need | Element |
| --- | --- |
| Read a property of something (`food level of %player%`) | expression, `03`/`04` |
| Do something (`announce %string%`) | effect, `05` |
| Check something (`%player% is flying`) | condition, `06` |
| Compute a value from arguments (`example_slug("Hello")`) | function, this file |

Functions have **no pattern**, no `Event`, and no parse-time context. Everything they need must arrive as an argument. A function cannot read `player`, `event-block`, or any other event value.

## Skeleton

```java
public final class ExampleFunction {

    public static void register(SkriptAddon addon) {
        Functions.register(DefaultFunction.builder(addon, "example_slug", String.class)
            .description("Turns text into a lowercase slug separated by hyphens.")
            .examples("example_slug(\"Hello, World!\") = \"hello-world\"")
            .since("1.0")
            .parameter("text", String.class)
            .build(args -> {
                String text = args.get("text");
                if (text == null)
                    return null;
                return text.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-|-$", "");
            }));
    }

}
```

Register it from `Elements.register(addon)` in the module's `load` phase.

## Signature

```java
DefaultFunction.builder(
    SkriptAddon addon,
    String name,
    Class<T> returnType
)
```

| Builder call | Meaning |
| --- | --- |
| `.parameter(name, type, modifiers...)` | Append a parameter, in call order |
| `.build(args -> value)` | Supply the implementation and finish the function |
| `.description(String...)` | Documentation: what it does |
| `.examples(String...)` | Documentation: valid Skript calls |
| `.since(String...)` | Documentation: version the function appeared in |
| `.keywords(String...)` | Documentation: search keywords |
| `.requires(String...)` | Documentation: plugin dependencies |
| `.contract(Contract)` | Make return plurality/type depend on the arguments |

The builder methods return the builder; only `build` returns a `DefaultFunction`.

## Parameters

```java
.parameter("text", String.class)
.parameter("times", Number.class, Modifier.OPTIONAL)
.parameter("targets", Player[].class)
.parameter("count", Number.class, Modifier.ranged(1, 10))
```

A parameter is **single or plural based on its class**, not on a separate flag:

| Type | Skript argument | Value from `args.get(...)` |
| --- | --- | --- |
| `String.class` | `%string%` (single) | `String` |
| `String[].class` | `%strings%` (plural) | `String[]` |
| `Player.class` | `%player%` | `Player` |
| `Player[].class` | `%players%` | `Player[]` |

Modifiers:

- `Modifier.OPTIONAL` — the argument may be omitted. `args.get(name)` returns `null`; use `getOrDefault`.
- `Modifier.KEYED` — accepts keyed values (`{_map::*}`); the value arrives as `KeyedValue[]`.
- `Modifier.ranged(min, max)` — numeric bounds, checked before the lambda runs.

Parameter names must be unique. They are used by `args.get(name)`, in documentation, and in error messages.

### Reading arguments

```java
String text = args.get("text");
Number times = args.getOrDefault("times", 1);
String fallback = args.getOrDefault("label", () -> "none");
Set<String> names = args.names();
```

`getOrDefault` treats a present-but-`null` value the same as an absent one.

### Numeric arguments

Skript numbers are `Long` or `Double`. Declare `Number.class`, not `Integer.class` or `Long.class`, unless you are certain of the literal type. Convert with `intValue()`, `doubleValue()`, and validate the result.

## Return values

The return class controls plurality:

| Return class | Skript result | Lambda returns |
| --- | --- | --- |
| `String.class` | single value | `String` |
| `String[].class` | list of values | `String[]` |
| `ExampleType.class` | single custom type | `ExampleType` |

Returning `null` means "no value"; Skript sees an empty result, not an error. For a plural function, prefer `new String[0]` over `null` when the empty case is normal.

Do not return an array containing `null` elements. Skript strips them, and converters may reject them.

If the result's plurality depends on the arguments, use `.contract(...)`:

```java
.contract(new Contract() {
    @Override
    public boolean isSingle(Expression<?>... arguments) {
        return arguments[0].isSingle();
    }

    @Override
    public Class<?> getReturnType(Expression<?>... arguments) {
        return String.class;
    }
})
```

## Registering

```java
@Override
public void load(SkriptAddon addon) {
    Elements.register(addon);
}
```

`Functions.register` must run during the addon's `load` phase, before Skript parses scripts. It is not reversible: `/skript reload` does not remove Java functions, and re-registering the same name and parameter list throws `SkriptAPIException`.

Rules:

- Function names must match `[\p{IsAlphabetic}_][\p{IsAlphabetic}\d_]*`.
- Names are **global**. The addon source is recorded for diagnostics, not used as a namespace. Prefix names (`example_slug`) to avoid collisions with Skript, other addons, and user scripts.
- Overloads with the same name and different parameter lists are allowed, but they make documentation and error messages ambiguous. Prefer distinct names.
- A duplicate name and parameter list is a startup error. It cannot be handled as a user-facing parse error.

## Calling from Skript

```skript
set {_slug} to example_slug("Hello, World!")
set {_parts::*} to example_repeat("ab", 3)
set {_parts::*} to example_repeat("ab")
```

The last call uses the default in `getOrDefault("times", 1)`.

## Errors and exceptions

Functions run inside the caller's trigger. A thrown exception propagates to `TriggerItem.run`, which logs a stack trace and aborts the rest of that script trigger. The function is not a safe place to let exceptions escape.

- **Never throw.** Return `null` or an empty array to stop.
- Validate argument values (`null`, negative counts, out-of-range numbers) and clamp, default, or return empty.
- Catch checked exceptions from I/O, reflection, or third-party APIs at the function boundary, log them, and return a safe value.
- `DefaultFunction` has no parse phase. There is no `init` to report a `Skript.error` from. If a call is invalid, Skript reports it before the lambda runs: a required single parameter that evaluates to nothing makes the function return no value without invoking the lambda.
- To log a runtime problem, `Skript.warning("...")` is preferable to `Skript.error`, which reads like a parse failure. Both only log; neither aborts.

See `02-registration-lifecycle.md` for the phase-by-phase rule.

## Pitfalls

- Declaring `Integer.class` or `Long.class` for a numeric argument. Skript passes `Long`/`Double`; use `Number.class`.
- Forgetting `Modifier.OPTIONAL`, then calling `args.get` on an omitted argument in a required slot. The lambda is not called when a required argument is missing, but optional handling must still be explicit.
- Declaring `String.class` and returning `String[]`, or the reverse. Plurality comes from the return class.
- Assuming the function sees the caller's event. It does not; pass the values as parameters.
- Registering in a phase where the parameter types do not exist yet. A custom type used in a parameter must be registered in `init`; functions are safest in `load`.
- Unprefixed names. A second addon with the same name and parameters prevents registration.
- Throwing for invalid input. Return no value and let the script continue.

Related: `02-registration-lifecycle.md` (phases and error handling), `09-types-comparators-converters.md` (custom types usable as parameters and returns), `14-documentation-annotations.md` (documentation style), `16-unit-testing.md` (testing function calls).
