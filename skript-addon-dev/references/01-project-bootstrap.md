# Project bootstrap

Copy `references/templates/` as a starting point. The template project **builds successfully** against Skript 2.17.1 and paper-api 26.3 with `gradle build`.

## Layout

```
my-addon/
├── .github/workflows/build.yml    build + skript-test-action
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat          (generate with `gradle wrapper`)
├── gradle/wrapper/
└── src/
    ├── main/
    │   ├── java/com/example/myaddon/
    │   │   ├── MyAddon.java       main class, implements AddonModule
    │   │   ├── Elements.java      explicit syntax + function registration
    │   │   ├── Types.java         ClassInfo + comparators + converters
    │   │   └── elements/
    │   │       ├── expressions/
    │   │       ├── effects/
    │   │       ├── functions/
    │   │       ├── conditions/
    │   │       ├── events/
    │   │       └── structures/
    │   └── resources/
    │       ├── plugin.yml
    │       └── lang/
    │           └── default.lang
    └── test/scripts/
        └── example-addon.sk       test cases for the action
```

Generate the wrapper once with a local Gradle: `gradle wrapper --gradle-version 9.8.0`. Commit the wrapper so builds are reproducible.

`.github/workflows/build.yml` builds the addon and runs the scripts under `src/test/scripts/` with `SkriptLang/skript-test-action`, pinned to Skript 2.17.1. See `16-unit-testing.md` for the inputs, the test syntax, and what the harness cannot cover. The template's script covers every element, including both functions.

## `build.gradle.kts`

```kotlin
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    java
}

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    // Skript is NOT on Maven Central. Keep this repository last.
    maven("https://repo.skriptlang.org/releases")
}

dependencies {
    // Must match the Minecraft version you target.
    compileOnly("io.papermc.paper:paper-api:26.3.build.226-stable")

    // compileOnly: Skript is provided by the server. NEVER shade it.
    compileOnly("com.github.SkriptLang:Skript:2.17.1") {
        isTransitive = false
    }

    compileOnly("org.jetbrains:annotations:24.1.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filesMatching("plugin.yml") {
        filter<ReplaceTokens>("tokens" to mapOf("version" to project.version.toString()))
    }
}
```

Why each piece:

- `compileOnly` — Skript and Paper are already on the server. Bundling them causes duplicate classes and `LinkageError`. It is also a licensing problem for Skript (GPLv3).
- `isTransitive = false` — Skript's own dependencies are supplied by the Skript plugin at runtime. Resolving them into your build causes near-duplicate, version-conflicting jars.
- Repository order — Gradle checks repositories in order; a Maven Central mirror that lacks Skript will otherwise fail the resolution before reaching the Skript repo.
- Java toolchain — a mismatch produces `UnsupportedClassVersionError` at server start, which looks like a plugin bug but is a build bug.
- Token replacement — `plugin.yml` uses `'@version@'` so the version is declared once.

If Gradle cannot find a Java 25 installation, either install one or add a toolchain resolver. Gradle 10 will hard-fail on auto-provisioned toolchains without repositories.

## `plugin.yml`

```yaml
name: MyAddon
version: '@version@'
main: com.example.myaddon.MyAddon
api-version: '1.21.5'
description: An example Skript addon.
authors: [YourName]
depend: [Skript]
```

- `depend: [Skript]` guarantees ordering and prevents enabling without Skript.
- Use `softdepend` only for optional integrations, and then guard every call into them.
- `api-version` should match the Paper/MC version you build against. Do not leave it out.

## `settings.gradle.kts`

```kotlin
rootProject.name = "my-addon"
```

## Main class

See `references/templates/src/main/java/com/example/exampleaddon/ExampleAddon.java`. The required shape:

```java
public class MyAddon extends JavaPlugin implements AddonModule {

    private static final String MINIMUM_SKRIPT_VERSION = "2.17";

    @Override
    public void onEnable() {
        if (!getServer().getPluginManager().isPluginEnabled("Skript")) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (Skript.getVersion().isSmallerThan(new Version(MINIMUM_SKRIPT_VERSION))) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        SkriptAddon addon = Skript.instance().registerAddon(MyAddon.class, name());
        addon.loadModules(this);
    }

    @Override
    public String name() {
        return "my-addon";
    }

    @Override
    public void init(SkriptAddon addon) {
        // Only needed if you ship lang/default.lang (type display names live there).
        addon.localizer().setSourceDirectories("lang", null);
        Types.register();
    }

    @Override
    public void load(SkriptAddon addon) {
        Elements.register(addon);
    }
}
```

Details that are easy to get wrong:

- `Skript` here is `ch.njol.skript.Skript` (the plugin class). `Skript.instance()` returns the `org.skriptlang.skript.Skript` API interface. Importing both unqualified causes a clash — import one and fully qualify the other if needed.
- `AddonModule.name()` has **no default in 2.17**. Implement it.
- `loadModules(this)` calls `canLoad` for each module, then `init`, then `load`. You do not call them yourself.
- Keep the version gate. Without it, an older Skript fails with `NoSuchMethodError` deep inside reload instead of a clear message.
- Never throw from `onEnable`, `init`, or `load`. Throwing aborts the module list and can leave the addon half-registered. Log and skip the failing registration; see `02-registration-lifecycle.md`.
- Functions are registered from the same `Elements.register(addon)` list, in `load`. Read `13-functions.md`.

## Multi-module addons

`AddonModule` composes: call `addon.loadModules(new CoreModule(), new CompatModule())`. Use `AddonModule.canLoad(addon)` to skip modules whose optional dependency is missing. Use `HierarchicalAddonModule` for nested modules. `moduleRegistry(addon)` returns a registry that stamps the module's origin onto registrations.
