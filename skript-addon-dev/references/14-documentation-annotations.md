# Documentation annotations

These are not decoration: Skript's documentation generator reads them, and users see the result. Missing or lazy annotations make the addon look unfinished.

## Class annotations (expressions, effects, conditions, structures)

```java
@Name("Foo Bar")
@Description({
    "First line.",
    "Second line."
})
@Example("send \"%foo bar%\" to player")
@Example("""
    on join:
        send "%foo bar%"
    """)
@Since("1.0")
@Keywords({"foo", "bar"})
@RequiredPlugins("WorldGuard 7")
@Events({"join"})
public class ExprFooBar extends SimpleExpression<String> { ... }
```

| Annotation | Notes |
| --- | --- |
| `@Name` | Short human name, Title Case. Used as the docs heading |
| `@Description` | `String[]`; one array element per paragraph |
| `@Example` | **Repeatable**; add one per example. Use text blocks for multi-line |
| `@Since` | Version of *your addon*. Use `"1.0"`, or `"1.0, 1.2 (syntax rework)"` |
| `@Keywords` | Search terms; add synonyms users are likely to type |
| `@RequiredPlugins` | Only if the element needs another plugin |
| `@Events` | Events the syntax is only valid in |
| `@DocumentationId` | Only to pin a stable docs anchor; usually omit |

Prefer repeated `@Example` over `@Examples`. One annotation per example reads better in source, diffs cleanly when an example changes, and is what Skript's own source uses almost everywhere.

`@Examples` still exists in 2.17 as a separate `String[]` annotation, not an alias. It is read by the docs generator, but it cannot be combined with `@Example` on the same class: only one is rendered, and the HTML and JSON generators pick different winners. Do not mix them.

## Events are different

`SkriptEvent` documentation comes from the `BukkitSyntaxInfos.Event` builder, not annotations:

```java
BukkitSyntaxInfos.Event.builder(EvtFoo.class, "Foo")
    .addDescription("Called when ...")
    .addExample("on foo:\n\tsend \"hi\"")
    .addSince("1.0")
    .addKeywords("foo", "bar")
    .addRequiredPlugin("WorldGuard 7")
```

## Quality bar

- An example must actually parse. Prefer a complete `on ...:` or `command ...:` block over a fragment.
- Describe behaviour, not implementation ("Sends a message to the given players", not "Calls Player#sendMessage").
- Document edge cases the user will hit: what happens with no target, what happens outside an event, what the default is.
- `@Since` should match the addon version you ship, not the Skript version.

## Localization (optional, but see types)

`addon.localizer().setSourceDirectories("lang", null)` lets you ship `lang/default.lang` and use `m.` message keys in errors. Translation is optional; `default.lang` also supplies the `types.<codename>` display names. See `09-types-comparators-converters.md` for the key format and noun syntax. If you use the localizer, use it for all user-facing error text rather than mixing localized and hard-coded strings.
