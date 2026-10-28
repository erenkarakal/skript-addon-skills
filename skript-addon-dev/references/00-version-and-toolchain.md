# Version and toolchain

## Verified matrix

Values below come from the Skript `2.17.1` tag.

| Item | Value | Source |
| --- | --- | --- |
| Skript version | `2.17.1` | `gradle.properties` (`version=`) |
| Target Java | **25** | Skript build: `latestJava = java25`, `testEnvJavaVersion=25` |
| Test environment | `java25/paper-26.3` | `gradle.properties` |
| Paper API | `io.papermc.paper:paper-api:26.3.build.+` | Skript `build.gradle` |
| Skript `api-version` | `1.21.5` | Skript `src/main/resources/plugin.yml` |
| Skript Maven repo | `https://repo.skriptlang.org/releases` | Skript README |
| Coordinate | `com.github.SkriptLang:Skript:<tag>` | Skript README |
| Paper repo | `https://repo.papermc.io/repository/maven-public/` | Skript `build.gradle` |

Skript is **not on Maven Central**. Add `repo.skriptlang.org` **last** in the repository list. If Gradle cannot resolve Skript's transitive dependencies, set `isTransitive = false` on the Skript dependency (the Skript README documents this workaround).

Pin concrete versions in a real addon. `26.3.build.+` is what Skript itself uses, but a published addon should pin an exact build so the build is reproducible.

## Two API generations exist in the jar

2.17 ships both, and mixing them is the main source of broken addon code.

| Modern (use this) | Legacy (avoid for new code) |
| --- | --- |
| `org.skriptlang.skript.registration.SyntaxRegistry` | `Skript.registerExpression/Effect/Condition/Structure` |
| `SyntaxInfo.builder(...)` | `PropertyExpression.register(...)` (deprecated since 2.12 for the 5-argument form, 2.14 for the 4-argument form) |
| `SyntaxInfo.Expression.builder(...)` | `ExpressionType.PROPERTY` |
| `BukkitSyntaxInfos.Event.builder(...)` | `Skript.registerEvent(...)` |
| `Skript.instance().registerAddon(Class, String)` | `Skript.registerAddon(JavaPlugin)` (deprecated) |
| `SyntaxInfo.Structure.builder(...)` | `Skript.registerStructure(...)` |

The syntax element base classes themselves (`Expression`, `SimpleExpression`, `Effect`, `Condition`, `SkriptEvent`) still live in `ch.njol.skript.lang`. That is current, not legacy — do not "fix" those imports to `org.skriptlang`.

Modern packages that matter:

```
org.skriptlang.skript.Skript                      (interface: registerAddon, addon)
org.skriptlang.skript.addon.SkriptAddon
org.skriptlang.skript.addon.AddonModule
org.skriptlang.skript.registration.SyntaxRegistry
org.skriptlang.skript.registration.SyntaxInfo
org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos
org.skriptlang.skript.lang.comparator.Comparators
org.skriptlang.skript.lang.converter.Converters
org.skriptlang.skript.lang.structure.Structure
org.skriptlang.skript.lang.entry.EntryValidator
org.skriptlang.skript.util.ClassLoader           (shadows java.lang.ClassLoader)
```

## Version sources

`SkriptLang/skript-worldguard` is an architectural reference, but its build file targets **Skript 2.14.0-pre1 / Java 21 / spigot-api 1.19.4**. Concrete consequences:

- Its main class implements `AddonModule` **without** `name()`. In 2.17 `AddonModule.name()` is abstract, so that class does not compile against 2.17 as-is.
- It uses `implementation`, not `compileOnly`, and does not use the Kotlin DSL.

Use it for structure and idioms; take versions from `SkriptLang/Skript`.

## If the user is on an older Skript

This skill covers 2.17+. For older versions, point the user to a separate API reference (the class layout and registration flow differ, especially pre-2.12).

## Checking a signature instead of guessing

The pinned jar is the source of truth. With the jar on disk:

```bash
javap -classpath Skript-2.17.1.jar ch.njol.skript.lang.Expression
javap -classpath Skript-2.17.1.jar org.skriptlang.skript.registration.SyntaxInfo\$Expression
```

Javadocs: <https://docs.skriptlang.org/javadocs/> (select the matching version).
Docs site: <https://docs.skriptlang.org/>.
`beta-docs.skriptlang.org` is incomplete; prefer the javadocs and the source tag.
