# Skript Addon Skills

Agent skills for writing **Java 25 Skript addons for Skript 2.17+**.

The skill is a plain directory. A `SKILL.md` with `name` and `description` metadata, plus `references/`.
It follows the Agent Skills layout, so any client that supports skills can load it.

These skills are recommended for:
- Plugin developers who are looking to add Skript support to their plugin.
- Addon developers who are looking to speed up their development.

and are NOT recommended for:
- People that know nothing about Java.
- People looking to contribute to existing addons or Skript without prior Skript API knowledge.

It is recommended to be qualified enough to review and verify the output.

## Contents

```
README.md
skript-addon-dev/                  <- skill ID: skript-addon-dev
├── SKILL.md
└── references/
    ├── 00-version-and-toolchain.md
    ├── 01-project-bootstrap.md
    ├── 02-registration-lifecycle.md
    ├── 03-expressions.md
    ├── 04-property-expressions.md
    ├── 05-effects.md
    ├── 06-conditions.md
    ├── 07-events.md
    ├── 08-structures.md
    ├── 09-types-comparators-converters.md
    ├── 10-expression-sections.md
    ├── 11-async-effects.md
    ├── 12-effect-sections.md
    ├── 13-functions.md
    ├── 14-documentation-annotations.md
    ├── 15-pitfalls.md
    ├── 16-unit-testing.md
    ├── 17-verification.md
    └── templates/                 <- compilable example addon
        ├── build.gradle.kts
        ├── settings.gradle.kts
        ├── gradle.properties
        ├── .gitignore
        ├── .github/workflows/build.yml
        └── src/
            ├── main/java/com/example/exampleaddon/...
            ├── main/resources/plugin.yml
            ├── main/resources/lang/default.lang
            └── test/scripts/example-addon.sk
```

## Install

Installation means placing the `skript-addon-dev/` directory where your client scans for skills. The location differs per client.

On Windows, `~` is `%USERPROFILE%`. On macOS it is your home directory, not `~/Library/Application Support`.

### OpenCode

Global, for every project:

```
~/.config/opencode/skills/skript-addon-dev/
```

Project-local:

```
<project>/.opencode/skills/skript-addon-dev/
```

OpenCode also reads the Claude-compatible paths (`~/.claude/skills/`, `.claude/skills/`) and the agent-compatible paths (`~/.agents/skills/`, `.agents/skills/`). Any one of them works. Do not install the same skill name in more than one, because duplicates make it ambiguous which copy loads.

### Claude Code

Global:

```
~/.claude/skills/skript-addon-dev/
```

Project-local:

```
<project>/.claude/skills/skript-addon-dev/
```

Claude Code discovers the directory on its own; there is no config entry to add.

### Codex

Codex reads `.agents/skills` from the working directory, from each parent directory up to the repository root, and from `$HOME/.agents/skills`.

User-wide:

```
~/.agents/skills/skript-addon-dev/
```

Repository-wide:

```
<repo>/.agents/skills/skript-addon-dev/
```

Codex follows symlinked skill folders. It also detects new skills without a restart; restart only if one does not appear.

`~/.agents/skills/` is shared: OpenCode reads it too, so one install there covers both.

### Other clients

Clients that implement the Agent Skills format use the same `SKILL.md` + `references/` layout but choose their own directory. Look up the skills location in your client's documentation and copy `skript-addon-dev/` there. A path from the sections above does not carry over to a different client.

## Use

All three clients select the skill from its `description` when a prompt matches, so a request about addon work is enough:

```
add a "maximum health" expression to my addon
```

To invoke it explicitly:

| Client | Explicit invocation |
| --- | --- |
| OpenCode | name the skill in the prompt; the agent loads it through its `skill` tool |
| Claude Code | name the skill in the prompt; Claude loads it from the skills directory |
| Codex | `/skills` to list, then `$skript-addon-dev` to invoke |

## Coverage

| Area | Reference |
| --- | --- |
| Version matrix, repos, Java 25, API generations | `00` |
| New project: layout, Gradle Kotlin DSL, `plugin.yml`, main class | `01` |
| `SkriptAddon`, `AddonModule`, `init`/`load`, `SyntaxRegistry` | `02` |
| Expressions, change support, delayed expressions | `03` |
| Property expressions, type-level properties | `04` |
| Effects, `SyntaxStringBuilder`, delays | `05` |
| Conditions, `PropertyCondition`, `PropertyType` | `06` |
| Events, `BukkitSyntaxInfos.Event`, event values | `07` |
| Structures, `EntryValidator`, node types | `08` |
| `ClassInfo`, `Parser`, language files and type names, comparators, converters | `09` |
| Section expressions (`SectionExpression`) | `10` |
| Async effects (`AsyncEffect`) | `11` |
| Effect sections (`EffectSection`, inline vs detached bodies) | `12` |
| Functions (`DefaultFunction`, parameters, optional arguments, plural returns) | `13` |
| Doc annotations | `14` |
| Error handling: never throw, `Skript.error`/`Skript.warning`, silent runtime stop | `02`, `15` |
| Pitfalls | `15` |
| Skript's test system + `skript-test-action` | `16` |
| Build, artifact inspection, test matrix, verification | `17` |
