# Pitfalls

Check the code against this list.

## Version and API drift

- **Do not copy `skript-worldguard` versions.** It targets Skript 2.14.0-pre1, Java 21, spigot-api 1.19.4, Groovy DSL, `implementation`.
- **`AddonModule.name()` is abstract in 2.17.** `skript-worldguard`'s main class omits it because it targets 2.14. Against 2.17 that class does not compile.
- **Legacy registration still exists but is not the point.** `Skript.registerExpression(...)`, `Skript.registerEvent(...)`, `PropertyExpression.register(...)` all still compile. Using them produces code that fights the rest of the 2.17 API and loses origins.
- **`PropertyExpression.register(...)` is deprecated in two waves.** The 5-argument `register(SyntaxRegistry, ...)` overload is `@Deprecated(since = "2.12", forRemoval = true)`; the 4-argument overload is `@Deprecated(since = "2.14", forRemoval = true)`. Use `infoBuilder(...)` + `syntaxRegistry().register(...)`.
- **`Structure#getEntryContainer()` is deprecated** and throws for simple structures. Keep the `EntryContainer` passed to `init`.
- **Java version.** 2.17 is Java 25. Targeting 21 gives `UnsupportedClassVersionError` on the server or a toolchain build failure, depending on where it breaks.
- **`api-version`.** Omitting it or using a stale one fails plugin load on modern Paper.

## Build

- **Skript must be `compileOnly`.** Bundling it duplicates classes, breaks `instanceof`, and violates GPLv3. Same for Paper/Bukkit.
- **`repo.skriptlang.org` must be last.** Skript is not on Maven Central.
- **Transitive resolution.** If Skript's dependencies fail to resolve, use `isTransitive = false` rather than adding random versions.
- **Do not shade `skript-reflect`, `Vault`, `WorldGuard`, or any other plugin.** Depend on them at runtime.
- **Do not commit `build/` or `.gradle/`.** A distributed skill or template repo should not carry build output.

## Registration and lifecycle

- Registering after `onEnable`, or from an async task → legacy registries throw `SkriptAPIException`; `syntaxRegistry().register(...)` accepts it but the syntax is too late for parsed scripts.
- Calling `registerAddon` twice for the same addon → duplicate origins and confusing diagnostics.
- Registering types in `load` instead of `init` → syntax using `%mytype%` never parses.
- Registering syntax for an optional plugin without `canLoad`/a guard → `NoClassDefFoundError` at enable.
- Expecting `/skript reload` to pick up Java changes → it cannot; restart the server.
- Using `org.skriptlang.skript.util.ClassLoader` without noticing it shadows `java.lang.ClassLoader`.

## Error handling

- **Throwing from `AddonModule.init`/`load`.** The exception propagates out of `loadModules`, later modules never run, and the plugin can be left half-registered. Log and skip the failing registration.
- **Throwing from a syntax `init`.** Use `Skript.error("...")` or `Skript.warning("...")`, then `return false`. An exception becomes an internal stack trace instead of a script error.
- **Throwing at runtime.** `TriggerItem.run` catches it, prints a stack trace, and stops the rest of the user's trigger. Return `null`, an empty array, `false`, or no-op.
- **`Objects.requireNonNull`/`Preconditions.checkNotNull` on script values.** They throw. Validate and return a safe value instead.
- **Throwing from `Event#getHandlers()`.** Return a shared static `HandlerList`. The event value registry does not need it to throw.
- **Using exceptions for control flow.** A missing player, an empty list, or an unsupported state is normal input, not an exceptional condition.

## Expressions and effects

- **Optional expressions are `null` in `exprs`.** `%-world%` or `[to %players%]` absent → `exprs[i] == null`. Null-check every optional.
- **`getSingle` on a list silently returns the first value.** Use `getArray` when the pattern is a list type.
- **`isSingle()` vs return type vs pattern.** All three must agree or the parser will place your expression where it does not fit.
- **Returning `null` arrays.** Return `new T[0]`, never `null`. Null elements are also rejected in property expressions.
- **Caching `Event`, `Player`, or `Trigger` fields.** Capture at runtime, not at parse time.
- **`toString` that is not re-parseable.** It appears in errors and debug output; build it with `SyntaxStringBuilder`.
- **Doing work in `init`.** `init` is parse time; side effects belong in `get`/`execute`.
- **Returning `false` from `init` without calling `Skript.error(...)`.** The user gets a bare "unable to parse".
- **Ignoring `isDelayed`.** Event-based expressions used after `wait` return stale values.

## Section expressions

- **Overriding the four-argument `init`.** `SectionExpression` marks it `final`. Implement `init(Expression<?>[], int, Kleenean, ParseResult, @Nullable SectionNode, @Nullable List<TriggerItem>)`.
- **Registering under `SyntaxRegistry.SECTION`.** Section expressions are expressions. Use `SyntaxRegistry.EXPRESSION`.
- **Assuming `node` is non-null.** A non-section-only expression can be used bare, and then `node` and `triggerItems` are both null.
- **Forgetting the custom event value.** If the body is meant to configure the produced object, register an `EventValue` on your custom `Event` subclass, or the body has nothing to talk to.
- **Forgetting `Variables.withLocalVariables(...)`.** The body is parsed/executed in a different event context and will not see the caller's `{_local}` variables.
- **Ignoring delays in the body.** `loadCode` does not propagate `hasDelayBefore`; use `loadOptionalCode` or `SectionUtils.loadLinkedCode` (which rejects delays outright).
- **Trying to use one on a section line.** A section expression cannot claim a block on a line that is already a `Section`/`EffectSection`, and only one section expression may claim a block per line.

## Effect sections

- **Registering under `SyntaxRegistry.EFFECT`.** `EffectSection` is a `Section`. Use `SyntaxRegistry.SECTION`; under `EFFECT` the colon form never parses.
- **Overriding the four-argument `init`.** `EffectSection` uses it to set `hasSection()` before delegating. Implement the six-argument version and leave the four-argument one alone.
- **Checking `sectionNode != null` instead of `hasSection()`.** The node is `@Nullable` for a reason; `hasSection()` is the contract and survives API changes.
- **Forgetting `return trigger != null`.** A body with a parse error (for example a `delay` under `loadLinkedCode`) leaves you with a half-built element that still runs.
- **Overriding `run(Event)`.** It is `final` and throws. Sections are walked: return `super.walk(event, true)` to run an inline body, `super.walk(event, false)` to continue past it.
- **Calling `debug` after `super.walk(...)`.** `super.walk` already did. Only call `debug(event, false)` yourself when you return something other than its result.
- **Forgetting `Variables.withLocalVariables(...)` around a detached body.** The body runs under its own event and otherwise cannot see the caller's `{_local}` variables.
- **Walking the detached trigger with the outer event.** Pass the custom event instance, or your event values resolve to `null`.
- **Using `loadLinkedCode` for a body that must `wait`.** It rejects delays by design; use `loadDelayableLinkedCode` and take responsibility for resuming the trigger.
- **Calling `Bukkit.callEvent` on the synthetic event.** It only exists to carry event values into the body; never dispatch it.

## Async effects

- **Missing `getParser().setHasDelayBefore(Kleenean.TRUE)`.** The parser then believes the next statement runs immediately. This is the single documented requirement of `AsyncEffect`.
- **Touching Bukkit in `execute`.** It runs off the main thread. Entities, worlds, inventories, and events are not safe. The failure is intermittent and often invisible on a test server.
- **Expecting `execute` to throw into the script.** It cannot. Log and stop; never throw from the async body.
- **Assuming locals are gone.** Skript restores local variables for the event on the async thread before calling `execute`, so reading them works — but anything they contain may still be Bukkit state.
- **Fire-and-forget expectations.** `AsyncEffect` pauses the trigger until the work completes, then resumes on the next main-thread tick. If you want fire-and-forget, use a normal `Effect` and schedule it yourself.
- **Forgetting the plugin may be disabled by the time the continuation runs.** Guard for it.

## Testing

- **Skipping tests because the build passed.** Compilation says nothing about whether your pattern parses.
- **Testing delayed behaviour in a `test` block.** The runner fires one event and shuts down the next tick; `wait` and async continuations may never run. Assert `has delay before` instead.
- **Expecting a `Player`.** `SkriptTestEvent` has no player. Spawn an entity or gate the test.
- **Duplicate test names.** Results are keyed by name, so duplicates overwrite each other.
- **Assuming isolation.** One server, one world, one event. Clean up entities and variables you create.
- **Using `build/libs` as `extra_plugins_directory` without checking.** Every file in that directory is installed as a plugin, including `-sources.jar` and shadow jars.
- **Not pinning `skript_repo_ref`.** Omitted, the action tests against master, not your compile target.
- **Turning off `run_vanilla_tests`.** It is what catches your pattern stealing vanilla syntax.

## Patterns

- `[...]` means optional; `(a|b)` means alternatives; `%type%` is a list, `%-type%` is a single optional. Never emit the brackets literally in generated *script* code — but they are required in Java pattern strings.
- A pattern that ends up matching almost everything (`<.+>`, adjacent expressions) needs `priority(SyntaxInfo.PATTERN_MATCHES_EVERYTHING)`, or it will steal other syntax.
- Custom type names in patterns must exist at parse time.
- Duplicate patterns across addons are resolved by priority and registration order — diagnose, do not "fix" by renaming randomly.

## Events

- Events registered with `SyntaxInfo.builder(...)` are not wired to Bukkit events. Use `BukkitSyntaxInfos.Event`.
- `init` takes `Literal<?>[]`; expressions are not available there.
- Heavy work in `check(...)` runs for every event instance.
- Missing `EventValues.registerEventValue(...)` → `event-player` is unavailable inside the event.
  In 2.17 use `addon.registry(EventValueRegistry.class).register(EventValue.builder(...).build())`; the static
  `EventValues` class is deprecated for removal.

## Types

- Code name collisions throw `IllegalArgumentException` from `Classes.registerClass`; they do not override.
- Missing `toVariableNameString` breaks variable round-tripping.
- `parse` throwing instead of returning `null` breaks unrelated parsing.
- Missing serializer → values do not survive restarts.
- **Expecting `ClassInfo.name(...)` to name the type at runtime.** It only feeds Skript's documentation. The runtime name is the global language key `types.<codename>`.
- **No `default.lang`, or `setSourceDirectories` never called.** Messages print the raw key `types.mytype`.
- **Calling `setSourceDirectories` twice.** The second call throws; set it once in `init`.
- **Forgetting `default.lang` in the jar.** The localizer throws `IllegalStateException` at enable, not at build time.
- **Using `|` in a noun.** The separator is `¦` (U+00A6), not the ASCII pipe.
- **Unprefixed language keys.** All keys share one global map, so a generic key can silently overwrite Skript's or another addon's text.
- **Expecting language files to localize patterns.** They only hold text; syntax patterns are fixed Java strings.

## Runtime

- Touching Bukkit state off the main thread.
- Mutating a collection returned by Skript (`getArray` may be a defensive copy; do not assume it is mutable).
- Retaining references to players/worlds across reloads → memory leaks.
- Assuming the event is the player's event; check `event instanceof PlayerEvent` before casting.

## Documentation

- `@Examples` exists as a separate `String[]` annotation. Prefer repeated `@Example` instead: it is repeatable, is what Skript's own source uses, and renders identically in both the HTML and JSON generators. Do not put both on one class; only one is rendered, and which one wins differs between the two generators.
- Placeholder text ("TODO", "An expression.") is user-visible.
- Event docs live on the builder, not annotations.

## Contribution policy (if the user wants to upstream a fix)

The Skript repository's `AGENTS.md` forbids AI-drafted issues, PR descriptions, and GitHub comments, and requires disclosure of AI assistance in commits/PRs. If the user asks you to prepare an upstream contribution, tell them this and let them author the communication themselves.
