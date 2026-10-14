# IntelliJ-LuaInsight 1.5.5 verification

Date: **2026-10-14**. IDE display name: **Lua Insight**. Publisher: **ClarkWain**.
Source commit: `866df1e7bd1b040984a1fe97ec15251010fdfdc4`.

This release declares standard-library package fields as `string`, fixing false
empty-string literal errors when assigning paths such as:

```lua
package.path = directory .. "?.lua"
```

The resource-only fix adds 11 type annotations: `package.path` and
`package.cpath` in Lua 5.1–5.4, plus `package.config` in Lua 5.2–5.4.
Type inference and inspection code are unchanged.

## Targeted editor checks

The four `StandardLibraryTypesTest` cases passed on Android Studio 2025.2.3,
build `AI-252.28238.7.2523.14688667`, using IntelliJ Platform editor fixtures.
Each case loads the actual bundled standard library for one Lua language level
and verifies:

- Assigning a concatenated string to the package fields produces no error.
- Assigning a number still produces a type error.
- Inference returns `string` for each field, rather than an empty-string literal.

Results are recorded in [stdlib-string-tests.json](validation/stdlib-string-tests.json).

## Release packages

Both targets compiled and packaged in the
[build run](https://github.com/ClarkWain/IntelliJ-LuaInsight/actions/runs/38540393810).

| Package | Compilation SDK | Supported platform range | Bytes |
| --- | --- | --- | --- |
| `IntelliJ-LuaInsight-1.5.5-IDEA2025.zip` | IDEA 2025.1.7.2 | 251–253 | 8,900,563 |
| `IntelliJ-LuaInsight-1.5.5-IDEA2026.zip` | IDEA 2026.1.5 | 261–262 | 8,901,281 |

SHA-256:

```text
6897950b306c0ebdade96ef719e33d07ac2903e9b5ae9f52f1c22c36fd0fc586  IntelliJ-LuaInsight-1.5.5-IDEA2025.zip
82900b9b5877278c6b8dbf96de16cd8d8981613a4b4e23ac832cbf1dc75782e9  IntelliJ-LuaInsight-1.5.5-IDEA2026.zip
```

Archive inspection verifies the name, publisher, version, compatibility bounds,
standard libraries and debugger resources. All 11 corrected declarations match
the source in both the ZIP resource directory and the plugin JAR used by the
standard-library provider. Artifact details are in
[release-summary.json](validation/release-summary.json).

## Verification scope

Verification for 1.5.5 consists of four targeted editor tests, compilation of
both release targets, packaging and archive inspection. The full regression
suite, Plugin Verifier matrix and native UI checks were not repeated.

The [1.5.4 verification record](https://github.com/ClarkWain/IntelliJ-LuaInsight/blob/2949f7b7/docs/validation.md)
documents the preceding flow-type tests on platforms 252 and 262 and links the
earlier compatibility checks. Those results remain baseline evidence.
Native debugger sessions were not exercised.

## Reproduce

Run Gradle with JDK 21. The 2026 target also needs a JDK 25 toolchain.

```sh
./gradlew test --tests '*StandardLibraryTypesTest' -DIDEA_VER=2025 --no-configuration-cache
./gradlew buildPlugin -DIDEA_VER=2025 --no-configuration-cache
./gradlew buildPlugin -DIDEA_VER=2026 --no-configuration-cache
```

To reproduce the editor checks on an installed IDE, add
`-PlocalIdePath=/path/to/ide`. Manual builds in the
[workflow](../.github/workflows/build.yml) compile and package by default;
the optional `validate` input enables the full regression and compatibility checks.
