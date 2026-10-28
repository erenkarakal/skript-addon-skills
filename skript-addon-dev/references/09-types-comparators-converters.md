# Types, comparators, converters

Custom types turn your Java objects into first-class Skript values: `%mytype%` in patterns, literals in scripts, variables, comparison and conversion.

Template: `references/templates/src/main/java/com/example/exampleaddon/Types.java`

## Register in `init`, never `load`

Parsing a script may need the type. If the type is registered after syntax, patterns using `%mytype%` fail to parse.

```java
@Override
public void init(SkriptAddon addon) {
    Types.register();
}
```

## ClassInfo

```java
Classes.registerClass(new ClassInfo<>(MyType.class, "mytype")
    .user("my ?types?")
    .name("My Type")
    .description("An example type.")
    .examples("my type")
    .since("1.0")
    .parser(new Parser<>() {
        @Override
        public @Nullable MyType parse(String input, ParseContext context) {
            return input.startsWith("my:") ? new MyType(input.substring(3)) : null;
        }

        @Override
        public String toString(MyType value, int flags) {
            return value.id();
        }

        @Override
        public String toVariableNameString(MyType value) {
            return "mytype:" + value.id();
        }
    }));
```

- The second constructor argument is the internal code name (`mytype`), used in variable serialization and patterns. Keep it lowercase, no spaces.
- `.user(...)` registers user-facing names/patterns for the type (regex-ish, `?` = optional space).
- `parse` must return `null` for input it does not own. Never throw; an exception breaks parsing for unrelated scripts.
- `toString` is what users see in messages; `toVariableNameString` is the stable representation used when storing in variables. They are different jobs.
- Add `.serializer(...)` only if the type must survive restarts in variables. Without one, values may not persist.
- For enums, `EnumClassInfo` removes most of the boilerplate.

## Display names and language files

`new ClassInfo<>(MyType.class, "mytype")` does **not** take a display name. It builds
`new Noun("types." + codeName)`, so the name users see in errors and messages comes from the global
language key `types.mytype`. `.name("My Type")` only sets the documentation name; it has no effect at
runtime.

With no language file, that key is missing and messages print `types.mytype` instead of a name. That is
not fatal, but it is user-visible.

### Wiring it up

```java
@Override
public void init(SkriptAddon addon) {
    // "lang" is the folder inside the jar. null = no user-editable translations on disk.
    addon.localizer().setSourceDirectories("lang", null);
    Types.register();
}
```

- `setSourceDirectories` may be called **once**; a second call throws `SkriptAPIException`.
- It immediately calls `Language.loadDefault(addon)` and fails with
  `IllegalStateException: <addon> is missing the required default.lang file!` if the folder has no
  `default.lang`.
- Files are read from the addon's source class (the class passed to `registerAddon`), so
  `src/main/resources/lang/default.lang` ends up in the jar as `/lang/default.lang`.
  See `references/templates/src/main/resources/lang/default.lang` for a working file.
- The second argument is a path on disk (for example `getDataFolder() + "/lang"`). If set, a file with
  the same name there overrides the jar copy, which lets server owners translate without rebuilding.
  Leave it `null` unless you want that.

### `default.lang`

```lang
# Optional; Skript only warns if it is missing.
version: 2.17.1   # the Skript version you built against

types:
	mytype: my type¦s @a
	entity: entit¦y¦ies @an
```

- `default.lang` is required. `english.lang` is optional and only needed for overrides; locale files
  (`german.lang`, `turkish.lang`, ...) follow the same flattened format.
- Sections are flattened with `.`: `types:` + `mytype:` becomes the key `types.mytype`.
  Keys are case-insensitive (everything is lowercased).
- `ClassInfo` code names must match `(?:any-)?[a-z0-9]+` (lowercase letters and digits only), so
  `types.<code>` is always a simple key.

### Noun format

```
singular¦plural@gender
common¦singular-suffix¦plural-suffix@gender
```

`¦` is U+00A6 (BROKEN BAR), not `|`.

| Value | Result |
| --- | --- |
| `my type¦s @a` | singular "my type", plural "my types", gender `a` |
| `entit¦y¦ies @an` | singular "entity", plural "entities", gender `an` |
| `thing¦s @x` | plural-only noun |
| `thing¦s @-` | no gender (no article) |

Gender decides which indefinite article Skript inserts in messages (`a` / `an`), and `x` marks a
plural noun. Wrong gender reads as broken English but does not break parsing.

### Global keys

Language entries are merged into one global map, so a key you define can silently overwrite Skript's
or another addon's entry. `types.<code>` is safe because the code name is yours; for anything else,
prefix keys with your addon name (`example-addon.message.foo`). A missing key renders as the key
itself.

Language files only affect text. They do **not** localize syntax patterns — patterns are fixed Java
strings.

## Comparators

```java
Comparators.registerComparator(MyType.class, String.class,
    (Comparator<MyType, String>) (value, string) ->
        value.id().equals(string) ? Relation.EQUAL : Relation.NOT_EQUAL);
```

- `ch.njol.skript.registrations.Comparators` also exists as a legacy entry point; prefer `org.skriptlang.skript.lang.comparator.Comparators`.
- `Relation` covers `EQUAL`, `NOT_EQUAL`, and ordering values. Ordering values only work if the comparator reports `supportsOrdering() == true`; implement `Comparator` as a class if you need that.
- Register comparisons in both directions if users can write either order, or rely on Skript's inverse handling (`supportsInversion()` defaults to `true`).
- A missing comparator produces a parse error at the comparison site, not at registration.

## Converters

```java
Converters.registerConverter(MyType.class, String.class, MyType::id);
```

- Converters let your type be used where another is expected (e.g. `%string%`).
- Register the narrowest sensible pair. Chained converters are expensive and can become ambiguous.
- The converter must not return `null` for a non-null input.

## Changers

If the type itself can be modified (`delete {_x}`, `add ... to ...`), register a `Changer` through `ClassInfo.changer(...)`. This is separate from an expression's `acceptChange`. Use it for container-like types.

## Ordering rules

1. Language files (`addon.localizer().setSourceDirectories(...)`), if you ship them.
2. `ClassInfo`.
3. Serializers, comparators, converters immediately after.
4. Syntax that uses the type after that (i.e. in `load`).

## Common mistakes

- Registering the same code name twice → `Classes.registerClass` throws `IllegalArgumentException` (also for a class registered twice).
- Using a space or uppercase in the code name.
- Returning `null` from `toString`.
- Forgetting `toVariableNameString`, so variables round-trip to something unparseable.
- Registering the type in `load` → `%mytype%` cannot be parsed.
