# Unit testing with Skript's test system

Skript ships a test harness that boots a real Paper server with your addon installed, loads `.sk` test scripts instead of normal scripts, and reports pass/fail per test case. `SkriptLang/skript-test-action` runs that harness in GitHub Actions with no manual setup.

Compiling only proves the API calls type-check; it does not check patterns, registration keys, or runtime behaviour.

Repository: <https://github.com/SkriptLang/skript-test-action>

## 1. Add the GitHub Action

The template includes this workflow at `.github/workflows/build.yml`.

```yaml
# .github/workflows/build.yml
name: Build

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '25'

      - uses: gradle/actions/setup-gradle@v4

      - name: Make the Gradle wrapper executable
        run: chmod +x gradlew

      - run: ./gradlew clean build

      - name: Run Skript tests
        uses: SkriptLang/skript-test-action@v1.3
        with:
          test_script_directory: src/test/scripts
          skript_repo_ref: 2.17.1
          extra_plugins_directory: build/libs
```

### Inputs

| Input | Required | Notes |
| --- | --- | --- |
| `test_script_directory` | yes | Your `.sk` test files, relative to the repo root |
| `skript_repo_ref` | no | Tag/branch/commit of Skript to test against. Set it to your compile target; if omitted, the latest master is used. |
| `extra_plugins_directory` | no | Directory whose **every file** is copied into the test server's `plugins/` |
| `run_vanilla_tests` | no | Defaults to `true`. |
| `skript_repo_url` | no | Only for forks. |

The action is a **Docker** action, so the job must run on Linux (`ubuntu-latest` or similar). It will not run on Windows or macOS runners.

### What the action does

1. Clones `SkriptLang/Skript` with submodules.
2. Checks out `skript_repo_ref`.
3. If `run_vanilla_tests` is false, deletes Skript's own tests.
4. Adds every file in `extra_plugins_directory` as a resource targeting `plugins/<filename>` in **every** environment JSON under `src/test/skript/environments/`.
5. Copies `test_script_directory` to `src/test/skript/tests/custom/`.
6. Runs `./gradlew quickTest`.

Consequences:

- `extra_plugins_directory: build/libs` will also install `-sources.jar`, `-javadoc.jar`, or a shadow `-all.jar` if your build produces them. Use a dedicated output directory containing only the plugin jar.
- Your jar must already be built. Run `./gradlew build` (or `jar`) in a previous step.
- `run_vanilla_tests: true` runs Skript's own suite. A new pattern with the wrong priority can silently steal vanilla syntax; the vanilla suite detects that.

## 2. Write test scripts

Put files under `src/test/scripts/`. They are plain Skript, but only `test` blocks are executed as tests.

A minimal file:

```skript
test "myaddon - custom type parses":
	set {_type} to my:hello
	assert {_type} is set with "custom type literal did not parse"
	assert "%{_type}%" is "hello" with "custom type did not stringify"
```

### `test "name":`

Registered as the event `test %string% [when <.+>]` on Skript's `SkriptTestEvent`. Each block is one test case.

- Test **names must be unique** — the tracker keys results by name.
- `test "name" when <condition>:` gates the test at load time. Used for version-specific tests:

```skript
test "myaddon - new api" when running minecraft "1.21.5":
```

- Tests are **not isolated**. The whole suite runs in one server, in one world, in one event. Variables, entities, and blocks persist between test cases. Clean up what you spawn.
- Test mode **replaces** normal scripts: Skript loads `-examples` and the test directory, nothing else. Your addon is still a normal plugin and must be in `plugins/`.

### `assert`

```skript
assert <condition> with "failure message"
assert <condition> with "message", expected <value>, and received <value>
assert unsafely <condition> with "message"
assert <condition> to fail with "message"
```

- `assert` only exists in test mode.
- A failing assert records the message plus the script file and line.
- `unsafely` allows an assertion **after a delay**. Without the tag, a delayed assertion is a parse error.
- `to fail` inverts the check: the assertion passes when the condition does not parse or does not evaluate to true.
- Prefer a specific message and the `expected ... received ...` form; a bare "Assertion failed" needs a second run to diagnose.

### `parse:` — assert that code does **not** parse

```skript
test "myaddon - rejects a non-player":
	parse:
		set {_x} to my food level of 42
		assert last parse logs is set with "expected a type error"
```

`parse` is a section that parses its body and captures the logs. `last parse logs` then exposes them. Use it for:

- type errors from your patterns
- invalid optional combinations
- `toString` round-trips that should be rejected

You can also assert the exact message, but that couples your test to Skript's wording; prefer `is set` unless the message itself is the contract.

To test a **structure** or an **event**, use the test runner's top-level `parse` structure. Its `code:` block must contain exactly one structure, and its `results:` variable receives the parse logs:

```skript
parse:
	results: {myaddon::parse::event::*}
	code:
		on my event:
			stop

test "myaddon - event parses":
	assert {myaddon::parse::event::*} is not set with "event did not parse"
	clear {myaddon::parse::event::*}
```

The `parse:` section runs inside a trigger; the `parse:` structure is top-level and loads at script load time.

### `has delay before`

`has delay before is true|false|unknown` is a **parse-time** condition. It is the clean way to test that an `AsyncEffect` or a section correctly propagates delays:

```skript
test "myaddon - async effect sets hasDelayBefore":
	parse:
		my async effect "test"
	assert unsafely last parse logs is not set with "async effect did not parse"
	assert unsafely has delay before is true with "async effect did not set hasDelayBefore"
```

`parse:` parses the effect without running it, so the assertion itself still executes; the async continuation would not run before the test server shuts down. The `unsafely` tag is required because the parser treats everything after the `parse:` body as delayed.

### `catch runtime errors:` (experimental)

```skript
using error catching

test "myaddon - runtime error is reported":
	catch runtime errors:
		my effect that errors
	assert last caught runtime errors is set with "expected a runtime error"
```

Requires the `error catching` experimental feature, enabled per script with `using error catching`. `catch runtime errors:` and `last caught runtime errors` are marked experimental in 2.17; do not build required tests on them without pinning the Skript version.

### Available event values in a test

`SkriptTestEvent` provides:

| Value | Source |
| --- | --- |
| `event-block` | `SkriptJUnitTest.getBlock()` |
| `event-location` | `SkriptJUnitTest.getTestLocation()` |
| `event-world` | `SkriptJUnitTest.getTestWorld()` |

There is **no player**. `player`, `event-player`, and anything requiring a `PlayerEvent` will not parse. Spawn entities instead:

```skript
test "myaddon - entity behaviour":
	spawn a pig at event-location:
		set {_entity} to the event-entity
	assert {_entity} is set with "spawn failed"
	delete the entity within {_entity}
```

An element that falls back to `player` when an optional argument is omitted has the same problem: the fallback is resolved at parse time. Test the explicit-target form instead.

### Delays and async work are not awaited

The runner fires **one** `SkriptTestEvent`, then shuts the server down on the next tick. `wait` and `AsyncEffect` continuations are scheduled for later ticks and may never run.

So:

- Do not assert results that appear after `wait`.
- Do not assert the output of an `AsyncEffect`.
- Do assert the parse-time contract (`has delay before is true`) and test the synchronous parts.
- If you genuinely need a delayed assertion, `assert unsafely` stops the parse error but does **not** make the runner wait. Treat such a test as unreliable.

## 3. Running the harness locally

If you want the same run without CI:

```bash
git clone --recurse-submodules https://github.com/SkriptLang/Skript.git
cd Skript
git checkout 2.17.1
git submodule update --recursive

# your tests
mkdir -p src/test/skript/tests/custom
cp -r ../my-addon/src/test/scripts/. src/test/skript/tests/custom/

# your addon jar: add it to every environment you will run
# src/test/skript/environments/java25/paper-26.3.json
#   "resources": [
#     {"source": "/abs/path/my-addon.jar", "target": "plugins/my-addon.jar"}
#   ]

./gradlew quickTest
```

`quickTest` = the latest Java + Minecraft environment (`java25/paper-26.3` for 2.17.1). Other tasks:

| Task | Purpose |
| --- | --- |
| `quickTest` | One environment, latest Java/MC |
| `skriptTest` | All environments |
| `skriptTestJava25` | All Java 25 environments |
| `skriptTestDev` | Interactive dev mode (`devMode`, `debug`, stdin) |
| `customTest` | `-PtestEnvs=java25/paper-26.3 -Ptimeout=...` |

Test-mode system properties, if you are debugging the harness itself:

```
-Dskript.testing.enabled=true
-Dskript.testing.dir=<tests directory>
-Dskript.testing.devMode=true
-Dskript.testing.junit=false
-Dskript.testing.results=test_results.json
-Dskript.testing.verbosity=<level>
```

Results are written as JSON (`succeeded` / `failed` lists). A failed test also appears in the server console with its message and line.

## 4. What to test for an addon

Minimum viable suite:

1. **Every syntax element parses** in its documented form — one line each.
2. **Every `@Example` parses.** Examples are user-visible documentation. An example that needs a live player cannot run here; see "Available event values" above.
3. **Type errors are rejected**, via `parse:`.
4. **Optional arguments** work both present and absent (the `null` branch is where addons crash) — unless the absent form resolves an event value the test event does not provide.
5. **`toString`** is exercised indirectly by parse failures and by error messages.
6. **Custom types** round-trip: literal → value → string.
7. **Delays**: if you have an `AsyncEffect` or a section that can delay, assert `has delay before`.

Do not try to unit-test Bukkit itself. Test your parsing, your filtering, and your conversion logic.

## 5. If the action is not an option

At minimum, run these manually before shipping:

- fresh Paper server, matching Skript release, your jar in `plugins/`
- clean start (no `/reload`)
- load a script that uses every element
- check the console for `Unable to parse`, `NoSuchMethodError`, `LinkageError`

The action automates that loop and runs it on every push.
