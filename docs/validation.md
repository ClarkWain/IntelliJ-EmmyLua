# IntelliJ-LuaInsight 1.5.4 verification

Date: **2026-10-14**. IDE display name: **Lua Insight**. Publisher: **ClarkWain**.
Source commit: `d2896ee5d760d1520291a46ad845f747217d969a`.

This release adds flow-sensitive nil checks for short-circuit expressions,
guarded branches and definite non-null assignments. See the
[feature description](flow-types.md) and [中文说明](flow-types.zh-CN.md).

## Automated editor checks

| Platform | Test suite | Passed |
| --- | --- | --- |
| Android Studio 2025.2.3, `AI-252.28238.7.2523.14688667` | NilFlowTest | 14/14 |
| Android Studio 2025.2.3, `AI-252.28238.7.2523.14688667` | TypeSafetyTest | 47/47 |
| IntelliJ IDEA 2026.2.3, `IU-262.10968.63` | NilFlowTest | 14/14 |

These checks run IntelliJ Platform editor fixtures with strict nil checks.
Coverage includes `and/or`, nil/boolean comparisons, parentheses,
`if/elseif/else`, loop conditions, arguments, aliases, named members and literal
string keys. Assignment coverage includes optional fields on `self`, parallel
assignments and nullable return values. Negative cases retain diagnostics after
nil writes, calls, shadowing, closure boundaries, conditional assignments,
ancestor/alias writes and loop effects. A direct inference assertion verifies
that the result is narrowed to `Info`, rather than merely hiding an inspection.

Two older override-fixture expectations were updated: an optional string
parameter assigned a definite string no longer produces a nil error on the next
statement. All other cases in that suite pass.

Individual test names and results are in
[nil-flow-tests.json](validation/nil-flow-tests.json).

## Release packages

Both targets compiled and packaged in the
[build run](https://github.com/ClarkWain/IntelliJ-LuaInsight/actions/runs/38526205997).

| Package | Compilation SDK | Supported platform range | Bytes |
| --- | --- | --- | --- |
| `IntelliJ-LuaInsight-1.5.4-IDEA2025.zip` | IDEA 2025.1.7.2 | 251–253 | 8,900,496 |
| `IntelliJ-LuaInsight-1.5.4-IDEA2026.zip` | IDEA 2026.1.5 | 261–262 | 8,901,233 |

SHA-256:

```text
ef015aaba8c59c5393946df389df612782506fa76560a0bc37cbe39c8359bddb  IntelliJ-LuaInsight-1.5.4-IDEA2025.zip
6c86f0a02092076e450b65421c8eae56e4a594b8594a14c84a9b6ff81032bd0c  IntelliJ-LuaInsight-1.5.4-IDEA2026.zip
```

Archive inspection confirms one `IntelliJ-LuaInsight` root, one plugin
descriptor with the expected name, publisher, version and compatibility bounds,
the new flow-type implementation, standard libraries and Windows/macOS/Linux
debugger binaries. Artifact details are in
[release-summary.json](validation/release-summary.json).

## Verification scope

The results above are compilation, packaging and automated editor tests for
this change. The full regression suite, Plugin Verifier matrix and native UI
checks were not repeated for 1.5.4.

The [preceding implementation checks](https://github.com/ClarkWain/IntelliJ-LuaInsight/actions/runs/38520596863)
passed 109 tests per target and reported no compatibility problems across IDEA
2025.1.7.2, 2025.2.6.3, 2025.3.6.1, 2026.1.5 and 2026.2.3. These remain baseline
results. The [1.5.3 record](https://github.com/ClarkWain/IntelliJ-LuaInsight/blob/a2d503b0/docs/validation.md)
documents the project rename and links earlier native editor screenshots.
Native debugger sessions were not exercised.

## Reproduce

Run Gradle with JDK 21. The 2026 target also needs a JDK 25 toolchain.

```sh
./gradlew test --tests '*NilFlowTest' --tests '*TypeSafetyTest' -DIDEA_VER=2025 --no-configuration-cache
./gradlew test --tests '*NilFlowTest' -DIDEA_VER=2026 --no-configuration-cache
./gradlew buildPlugin -DIDEA_VER=2025 --no-configuration-cache
./gradlew buildPlugin -DIDEA_VER=2026 --no-configuration-cache
```

For checks on a specific installed IDE, add `-PlocalIdePath=/path/to/ide`.
In the [build workflow](../.github/workflows/build.yml), manual builds compile
and package by default. Enable the optional `validate` input for a full
regression and compatibility sweep.
