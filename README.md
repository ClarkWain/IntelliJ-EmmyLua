# Lua Insight

English | [简体中文](README.zh-CN.md)

A Lua plugin for IntelliJ IDEA and Android Studio that brings
[Luanalysis](https://github.com/Benjamin-Dobell/IntelliJ-Luanalysis)'s advanced type
analysis to the maintained [EmmyLua](https://github.com/EmmyLua/IntelliJ-EmmyLua)
codebase, preserving EmmyLua's editor tools and debugger integrations.
This is an independent fork, with plugin ID **`com.weitp`** and display name
**Lua Insight**.

## Install

1. Download the ZIP matching your IDE from [Releases](https://github.com/ClarkWain/IntelliJ-EmmyLua/releases).
2. Disable EmmyLua, Luanalysis and other Lua language plugins to avoid multiple
   providers handling the same Lua files.
3. Open **Settings → Plugins → gear menu → Install Plugin from Disk**, select the
   ZIP, and restart the IDE. Do not extract the ZIP yourself.

The independent ID prevents this fork from replacing an installed EmmyLua or
Luanalysis plugin. Simultaneously enabling them is not supported.

| Release package | Supported IntelliJ Platform range |
| --- | --- |
| `Lua-Insight-1.5.2-IDEA2025.zip` | IDEA 2025.1–2025.3 (251–253) |
| `Lua-Insight-1.5.2-IDEA2026.zip` | IDEA 2026.1–2026.2 (261–262) |

For Android Studio, match the IntelliJ platform build in **Help → About**, not
just the year in its marketing version. The user's Android Studio 2025.2.3
(build `AI-252.28238.7.2523.14688667`) uses the **2025** package.
Exact tested builds, checksums and runtime evidence are recorded in
[validation](docs/validation.md).

## Features

- Structural `@shape` types, indexed fields, typed dictionaries and tuples.
- Generic classes, constrained generics, generic aliases and recursive aliases.
- Expression and return casts with `@type`, and union exclusion with `@not`.
- Callable classes, overloads, optional parameters and variadic results.
- Type checks for assignments, arguments, returns, inheritance and undeclared members.
- Lua 5.1–5.4 definitions, completion, navigation, rename, formatting, documentation
  and the existing EmmyLua debugger integrations.

Type-safety inspections are enabled by default and type mismatches appear as
errors. Adjust their severity and strict-nil/unknown-type handling in the IDE's
inspection and Lua settings when needed.

See the [migration inventory](docs/luanalysis-migration.md) for source revisions
and feature-level regression coverage. The small
[validation project](examples/luanalysis-validation) includes valid generic and
shape examples, plus deliberately invalid examples for checking diagnostics.

## Build and validate

Use JDK 21 to run Gradle. The 2025 target compiles against IDEA 2025.1 with JDK 21;
the 2026 target compiles against IDEA 2026.1 and requires a JDK 25 toolchain.
Set `JAVA_HOME_25_X64` to that JDK if Gradle cannot find it.

```sh
./gradlew test buildPlugin -DIDEA_VER=2025 --no-configuration-cache
./gradlew test buildPlugin -DIDEA_VER=2026 --no-configuration-cache
```

Archives are written to `build/distributions/`. To use an installed IDE as the
SDK, add `-PlocalIdePath=/path/to/ide`. To verify against several installed IDEs,
add `-PverificationIdePaths="/path/to/ide1;/path/to/ide2"` and run `verifyPlugin`.
Compile releases against the oldest IDE in their declared range.

On Windows, this helper also handles non-ASCII checkout paths:

```powershell
.\scripts\validate-local.ps1 -IdePath 'C:\Program Files\Android\Android_Studio_2025'
```

## Credits and license

Based on EmmyLua by [tangzx](https://github.com/tangzx) and its contributors, and
Luanalysis by [Benjamin Dobell](https://github.com/Benjamin-Dobell) and its
contributors. Original copyright notices are retained. Both upstream projects
and this fork use the [Apache License 2.0](LICENSE).

[EmmyLua documentation](https://emmylua.github.io/) ·
[Report an issue in this fork](https://github.com/ClarkWain/IntelliJ-EmmyLua/issues)
