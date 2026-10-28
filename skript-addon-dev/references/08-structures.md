# Structures

Base class: `org.skriptlang.skript.lang.structure.Structure`.

Structures are top-level script elements (like `options`, `command`, `function`). Use one only when the syntax owns a block or a set of entries; for everything else, use an effect.

Template: `references/templates/src/main/java/com/example/exampleaddon/elements/structures/StructExample.java`

## Skeleton

```java
public class StructFoo extends Structure {

    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.STRUCTURE,
            SyntaxInfo.Structure.builder(StructFoo.class)
                .supplier(StructFoo::new)
                .addPattern("foo section")
                .nodeType(SyntaxInfo.Structure.NodeType.SECTION)
                .build()
        );
    }

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult,
            @Nullable EntryContainer entryContainer) {
        return true;
    }

    @Override
    public boolean load() {
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "foo section";
    }
}
```

## Lifecycle

| Phase | Method | Use |
| --- | --- | --- |
| Init | `init(Literal<?>[], int, ParseResult, EntryContainer)` | Read arguments, tags, entries |
| Pre-load | `preLoad()` | Prepare before other structures load |
| Load | `load()` | Main work |
| Post-load | `postLoad()` | Work that needs other structures loaded |
| Unload | `unload()`, `postUnload()` | Cleanup on script unload |

`load()` is abstract and must return `true` on success. Return `false` **after** printing an error, otherwise the failure is silent.

## The body is not parsed for you

A `SECTION` node gives you an `EntryContainer` (and, if you ask for it, a `SectionNode`). The child lines are **not** parsed, executed, or validated unless you do it yourself with `loadCode(...)`.

That means invalid syntax inside a structure body passes silently, and a structure that ignores its body does nothing at all. If the structure represents executable code, load it explicitly (see `10-expression-sections.md` for the `loadCode`/`runSection` pattern).

## Node types

```java
.nodeType(SyntaxInfo.Structure.NodeType.SECTION)   // requires a "name:" block
.nodeType(SyntaxInfo.Structure.NodeType.SIMPLE)    // single line
.nodeType(SyntaxInfo.Structure.NodeType.BOTH)
```

Default is `SECTION`. Choosing the wrong one means the structure silently never parses.

## Entries

If the structure has named entries, register an `EntryValidator` on the builder:

```java
.entryValidator(EntryValidator.builder()
    .addEntryData(new LiteralEntryData<>("key", "default", true, String.class))
    .build())
```

- `LiteralEntryData<T>` parses the value through `Classes.parse` and can yield `null`. For message or variable entries use `VariableStringEntryData`.
- With a validator, `entryContainer` is non-null and already validated.
- Without one, `entryContainer` is `null` unless the structure is a section node built with `EntryContainer.withoutValidator(...)`.
- Do not use `Structure#getEntryContainer()`; it is deprecated and throws for simple structures. Keep the reference passed to `init` instead.

## Priority

`getPriority()` controls load order across all structures (lower loads first). Default is `Structure.DEFAULT_PRIORITY` (1000). Only override if you genuinely depend on order.

## Pitfalls

- `init` returning `true` while a required entry is missing: validate and return `false`.
- Building the whole feature into a structure when an effect/condition would do. Structures cannot be used inside triggers.
- Forgetting `toString`; it is used in error messages.
- Section-only structure + user writes a single line → parse error with no obvious cause.
