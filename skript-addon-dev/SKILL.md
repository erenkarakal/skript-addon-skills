---
name: skript-addon-dev
description: Write, review, and debug Java addons for Skript 2.17+ on Paper. Covers expressions, property expressions, effects, async effects, effect sections, section expressions, conditions, events, structures, custom types, language files, comparators, converters, functions, syntax registration, Gradle Kotlin DSL builds, plugin.yml, Skript's unit test system, and API version pitfalls. Use when creating a Skript addon, adding a syntax element to an existing addon, localizing an addon, writing functions or tests for an addon, or fixing addon compile/registration errors.
---

# Skript Addon Development (2.17+)

You are writing a **Java** Paper plugin that extends Skript. This is not about writing `.sk` scripts.

Everything here targets **Skript 2.17+**. Code written for 2.6–2.8 uses different registration classes and will not compile.

## Step 0 — Establish the target

Before writing or editing code, confirm:

1. **Skript version** — must be 2.17 or newer. If the user targets 2.16 or older, this skill does not cover it.
2. **Paper/Minecraft version** — decides the `paper-api` coordinate and `api-version`.
3. **Java version** — 2.17.x is built for **Java 25**. Targeting 21 produces classes the server cannot load.
4. **Build tool** — Gradle with the **Kotlin DSL** (`build.gradle.kts`).
5. **Package + addon ID** — e.g. `com.example.myaddon`, ID `my-addon` (kebab-case).

If any of these is unknown, ask rather than assuming a default.

## Hard requirements

- Skript and Paper are `compileOnly`; they must not be shaded.
- Register in `onEnable`, after checking Skript is present and new enough.
- `Skript.instance().registerAddon(YourMainClass.class, "your-addon-id")`.
- Implement `AddonModule`; put **types/comparators/converters in `init`**, **syntax in `load`**.
- Register syntax through `SyntaxRegistry` (`addon.syntaxRegistry()`), not the legacy static `Skript.registerExpression(...)` helpers.
- Every syntax class: doc annotations, a correct `toString`, and null-safe handling of optional expressions.
- **Never throw an exception.** In a syntax `init`, report with `Skript.error()`/`Skript.warning()` and `return false`. At runtime, stop silently: return `null`/an empty array, `false`, or no-op. The same rule applies in `load` and function bodies (`02`, `13`, `15`).
- A function registers through `Functions.register(...)` in `load`, with a globally unique name (`13`).
- Any `AsyncEffect` **must** call `getParser().setHasDelayBefore(Kleenean.TRUE)` in `init`.
- A `SectionExpression` overrides the **six-argument** `init`; the four-argument one is `final`.
- An `EffectSection` registers under `SECTION`, implements the **six-argument** `init`, and checks `hasSection()` instead of `sectionNode != null`.
- A type's runtime name comes from the `types.<codename>` language key, not from `ClassInfo.name(...)`.
- Use signatures from the pinned version. If a class or method is not covered here, read it from the jar or javadocs.
- Finish when `gradlew build` passes and the element has a test (`16-unit-testing.md`).

## Workflow

1. Read `references/00-version-and-toolchain.md` and confirm the pinned versions.
2. For a new project, copy `references/templates/` as the skeleton (it compiles as-is). Read `references/01-project-bootstrap.md`.
3. Read `references/02-registration-lifecycle.md` before touching registration.
4. Pick the element kind and read its reference file. Copy the matching template class and adapt it.
5. If scripts need a reusable operation with typed arguments, read `references/13-functions.md`. Functions have no pattern and no event; register them in `load`.
6. Add doc annotations (`references/14-documentation-annotations.md`).
7. If the element owns a block, read `references/12-effect-sections.md` for an effect with an optional body, or `references/10-expression-sections.md` for an expression with a body. If it does slow work, read `references/11-async-effects.md` first and decide whether `AsyncEffect` is the right base class.
8. Add test scripts under `src/test/scripts/` and wire up `skript-test-action` (`references/16-unit-testing.md`). A passing build does not show whether the syntax parses.
9. Re-read `references/15-pitfalls.md` against your code.
10. Build and verify (`references/17-verification.md`). Report the actual compile output.

## Reference map

| File | Use it for |
| --- | --- |
| `references/00-version-and-toolchain.md` | Version matrix, repos, Java/Paper, API generation split |
| `references/01-project-bootstrap.md` | New project: tree, `build.gradle.kts`, `plugin.yml`, main class |
| `references/02-registration-lifecycle.md` | `SkriptAddon`, `AddonModule`, `SyntaxRegistry`, phases, origins |
| `references/03-expressions.md` | `SimpleExpression`, `get`/`init`/`toString`, change support |
| `references/04-property-expressions.md` | `PropertyExpression` / `SimplePropertyExpression`, `infoBuilder` |
| `references/05-effects.md` | `Effect`, `execute`, `error`, delayed effects |
| `references/06-conditions.md` | `Condition` vs `PropertyCondition`, `PropertyType` |
| `references/07-events.md` | `SkriptEvent`, `BukkitSyntaxInfos.Event`, event values |
| `references/08-structures.md` | `Structure`, phases, `EntryValidator`, node types |
| `references/09-types-comparators-converters.md` | `ClassInfo`, `Parser`, language files and type names, `Comparators`, `Converters` |
| `references/10-expression-sections.md` | `SectionExpression` / `ExpressionSection`: expressions that own a block |
| `references/11-async-effects.md` | `AsyncEffect`: off-thread execution, delay reporting, thread-safety rules |
| `references/12-effect-sections.md` | `EffectSection`: one syntax as both effect and section, `hasSection`, inline vs detached body, `walk` contract |
| `references/13-functions.md` | `DefaultFunction`, parameters, optional arguments, plural returns, runtime errors |
| `references/14-documentation-annotations.md` | `@Name`, `@Description`, `@Example`, `@Since`, `@Keywords` |
| `references/15-pitfalls.md` | Known traps and checklist |
| `references/16-unit-testing.md` | Skript's test system + `SkriptLang/skript-test-action` CI workflow |
| `references/17-verification.md` | Build, artifact inspection, test matrix, runtime check |

## Templates (all compile against 2.17.1)

| Path | Element |
| --- | --- |
| `references/templates/build.gradle.kts` | Gradle Kotlin DSL, Java 25, pinned deps |
| `references/templates/.github/workflows/build.yml` | GitHub Actions: build + `skript-test-action` |
| `references/templates/settings.gradle.kts` | Root project name |
| `references/templates/src/main/resources/plugin.yml` | `depend: [Skript]`, tokenized version |
| `references/templates/src/main/resources/lang/default.lang` | `types.<code>` display names |
| `references/templates/src/main/java/.../ExampleAddon.java` | Main class + `AddonModule` |
| `references/templates/src/main/java/.../Elements.java` | Explicit syntax + function registration list |
| `references/templates/src/main/java/.../Types.java` | `ClassInfo` + comparator + converter |
| `references/templates/src/main/java/.../elements/functions/ExampleFunction.java` | `DefaultFunction` with optional argument and plural return |
| `references/templates/src/main/java/.../elements/expressions/ExprExample.java` | `SimpleExpression` |
| `references/templates/src/main/java/.../elements/expressions/ExprPropertyExample.java` | `SimplePropertyExpression` |
| `references/templates/src/main/java/.../elements/expressions/ExprSecExample.java` | `SectionExpression` |
| `references/templates/src/main/java/.../elements/effects/EffExample.java` | `Effect` with optional argument |
| `references/templates/src/main/java/.../elements/effects/EffAsyncExample.java` | `AsyncEffect` with mandatory delay reporting |
| `references/templates/src/main/java/.../elements/effects/EffSecExample.java` | `EffectSection` with detached body + custom event value |
| `references/templates/src/main/java/.../elements/conditions/CondExample.java` | `PropertyCondition` |
| `references/templates/src/main/java/.../elements/events/EvtExample.java` | `SkriptEvent` via `BukkitSyntaxInfos` |
| `references/templates/src/main/java/.../elements/structures/StructExample.java` | `Structure` |
| `references/templates/src/test/scripts/example-addon.sk` | Test script for `skript-test-action` |

## Failure playbook

- **Compile error on an API call** → the signature is wrong. Open `references/00-version-and-toolchain.md`, then read the signature from the Skript jar (`javap`) or javadocs for the pinned version.
- **Function call does not parse** → the name is misspelled, the parameter list does not match, or the function was never registered in `load`. Names are global; check for a collision (`13`).
- **Section expression does not compile** → you overrode the four-argument `init`; it is `final`. Use the six-argument version.
- **Effect section acts as a plain effect and ignores the block** → it is registered under `EFFECT`, you overrode the four-argument `init`, or you checked `sectionNode != null` instead of `hasSection()`.
- **Syntax never parses** → check registration key (`EXPRESSION`, `SECTION`, `EFFECT`, `CONDITION`, `STRUCTURE`, `BukkitSyntaxInfos.Event.KEY`), the pattern, and that `load` actually ran. Add a `test` block and let the harness tell you.
- **Class not found / LinkageError at runtime** → Skript or Paper got shaded, or versions disagree.
- **Duplicated syntax after `/reload`** → Java registrations cannot be reloaded; restart the server.
- **Code after an async effect behaves oddly** → `setHasDelayBefore(Kleenean.TRUE)` is missing.
- **A function always returns nothing** → a required argument evaluated to nothing, the lambda returned `null`, or the declared return class is not the class actually returned (`13`).
- **A stack trace names your addon and the rest of the script stopped** → you threw. Report parse errors with `Skript.error()`/`Skript.warning()` and `return false`; stop silently at runtime (`02`).
- **Test never runs / not collected** → test mode replaces normal scripts; the addon must be installed as a plugin, and test names must be unique.
- **Messages show `types.mytype` instead of a name** → the language key is missing. Add `types.mytype` to `default.lang` and call `addon.localizer().setSourceDirectories("lang", null)` in `init`.

## Scope

In scope: everything needed to build, register, test, and verify a Skript addon.
Out of scope: writing Skript scripts, Bukkit internals unrelated to Skript, and Skript versions below 2.17.
