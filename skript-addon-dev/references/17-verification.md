# Verification

An addon is verified by a successful build **and** a test run. Compilation only proves that the API calls type-check; it does not show whether the syntax parses.

The order is: compile → inspect the artifact → run Skript's test suite → optionally check on a real server.

## 1. Compile

```bash
# Windows
gradlew.bat build

# Unix
./gradlew build
```

Requirements:

- `BUILD SUCCESSFUL`
- the jar exists under `build/libs/`
- no deprecation warnings from *your* code (warnings from Skript itself are acceptable)

If compilation fails:

1. Read the actual error: missing symbol vs wrong signature vs type mismatch.
2. If a method signature is wrong, **read the signature** from the pinned jar or javadocs and use that.
3. Change only what the error requires.

`javap` is the fastest source of truth:

```bash
javap -classpath path/to/Skript-2.17.1.jar ch.njol.skript.lang.util.SimpleExpression
javap -classpath path/to/Skript-2.17.1.jar ch.njol.skript.expressions.base.SectionExpression
```

## 2. Inspect the artifact

- `plugin.yml` is present in the jar and `version` was replaced.
- The jar contains **only** your classes and your own resources (`plugin.yml`, `lang/default.lang`) — no `ch/njol/skript`, no `org/bukkit`, no `io/papermc`.
- `main` in `plugin.yml` matches the real class.

```bash
unzip -l build/libs/your-addon-1.0.0.jar
```

## 3. Run Skript's test system

**`SkriptLang/skript-test-action` runs Skript's test system in CI.** It boots an actual Paper server with your jar installed, loads your test scripts, and fails the build on parse errors or failed assertions.

<https://github.com/SkriptLang/skript-test-action>

```yaml
      - run: ./gradlew build

      - name: Run Skript tests
        uses: SkriptLang/skript-test-action@v1.3
        with:
          test_script_directory: src/test/scripts
          skript_repo_ref: 2.17.1        # pin to what you compile against
          extra_plugins_directory: build/libs
```

`run_vanilla_tests` defaults to `true`. It catches a pattern that silently outranks vanilla syntax.

A passing run proves:

- the addon enables against a real Skript build
- every covered syntax parses
- your assertions about behaviour hold
- you did not break vanilla syntax

See `16-unit-testing.md` for how to write the test scripts, what `assert`/`parse:`/`has delay before` do, and what the harness cannot test (delays, players, async results).

## 4. Manual server check (when CI is not available)

You need a Paper server matching your `paper-api`, plus the matching Skript release. Place your jar and Skript in `plugins/`, start the server **fresh** (no `/reload`), and check the console for:

- `[Skript] ... loaded` and your addon enabled
- no `Unable to parse` for your syntax
- no `NoSuchMethodError` / `NoClassDefFoundError` / `LinkageError`

Then load a script that uses every element via `/skript reload <file>` — this reloads *scripts*, not Java.

A registered syntax that never parses produces no error until a script uses it. This check covers that case without the test harness.

## 5. Manual checklist

- [ ] Every syntax class has `@Name`, `@Description`, `@Example`, `@Since`
- [ ] Event docs are on the `BukkitSyntaxInfos.Event` builder
- [ ] Every optional expression is null-checked
- [ ] `toString` produces valid syntax and survives a `null` event
- [ ] `getSingle`/`getArray` matches the pattern (single vs list)
- [ ] `init` failures call `Skript.error(...)` or `Skript.warning(...)` before `return false`
- [ ] No `Event`/`Player` stored in fields at parse time
- [ ] Types registered in `init`, syntax in `load`
- [ ] Section expressions override the six-argument `init`
- [ ] Effect sections register under `SECTION`, implement the six-argument `init`, and use `hasSection()`
- [ ] `walk` returns `super.walk(event, true|false)`, and `run(Event)` is not overridden
- [ ] `AsyncEffect` calls `setHasDelayBefore(Kleenean.TRUE)`
- [ ] Async `execute` touches no Bukkit state
- [ ] Version gate present in `onEnable`
- [ ] `depend: [Skript]` in `plugin.yml`
- [ ] Skript/Paper are `compileOnly` and absent from the jar
- [ ] Test scripts exist for every syntax element; examples that need a live player are the documented exception
- [ ] Custom types have a `types.<code>` entry in `lang/default.lang`, and `setSourceDirectories` is called once
- [ ] README/docs tell users to restart, not reload

## 6. What "verified" means in this skill

The Java templates and `build.gradle.kts` were compiled and packaged with:

- Skript `2.17.1`
- `io.papermc.paper:paper-api:26.3.build.226-stable`
- Java 25 toolchain
- Gradle `9.8.0`

The addon test cases under `references/templates/src/test/scripts/` were executed with `quickTest` against Skript 2.17.1 on Paper 26.3 with Java 25, and all of them passed. Skript's own vanilla and example scripts were not exercised in that local run; the GitHub Action runs them alongside the addon tests. A test run is reported only when it was actually executed.
