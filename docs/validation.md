# IntelliJ-LuaInsight 1.5.3 build verification

Date: **2026-10-14**. IDE display name: **Lua Insight**. Publisher: **ClarkWain**.

Both release targets compiled and packaged successfully in the
[build run](https://github.com/ClarkWain/IntelliJ-LuaInsight/actions/runs/38522104615)
from source commit `73a05d6714bbf605b7e1271b67e47f58fb3aa30f`.

## Release packages

| Package | Compilation SDK | Supported platform range | Bytes |
| --- | --- | --- | --- |
| `IntelliJ-LuaInsight-1.5.3-IDEA2025.zip` | IDEA 2025.1.7.2 | 251–253 | 8,883,446 |
| `IntelliJ-LuaInsight-1.5.3-IDEA2026.zip` | IDEA 2026.1.5 | 261–262 | 8,884,162 |

SHA-256:

```text
c1dd520e875a439e8cff894249d047c220bac1e8c3c31676389104cf5638fa06  IntelliJ-LuaInsight-1.5.3-IDEA2025.zip
0b56f1536e76dc66620e9fdbb2f97fe8605705cd0035c9ac0f0aaaac29e12f71  IntelliJ-LuaInsight-1.5.3-IDEA2026.zip
```

Each ZIP contains one `IntelliJ-LuaInsight` root, one plugin descriptor with the
correct display name, publisher, version and compatibility bounds, Lua standard
libraries and Windows/macOS/Linux debugger binaries. Project links point to this
repository. Original source copyright notices are retained.

Machine-readable artifact details are in
[release-summary.json](validation/release-summary.json).

## Verification scope

This release updates the project name, publisher metadata, links and public
documentation. Verification consists of compilation, packaging and archive
inspection. Regression tests and Plugin Verifier were not rerun for this update.

The [preceding implementation checks](https://github.com/ClarkWain/IntelliJ-LuaInsight/actions/runs/38520596863)
passed 109 tests per target and reported no compatibility problems across IDEA
2025.1.7.2, 2025.2.6.3, 2025.3.6.1, 2026.1.5 and 2026.2.3. These are baseline
results, not new test results for the renamed Gradle project.

Earlier native editor checks cover Android Studio 2025.2.3 (platform 252) and
IDEA 2026.2.3: generic shape completion, four expected type errors and Markdown
Quick Documentation. The [versioned editor validation record](https://github.com/ClarkWain/IntelliJ-LuaInsight/blob/935c6789203317063bbe4a1bd65399e87436efd3/docs/validation.md)
contains the original artifacts, runtimes and screenshots. Native debugger
sessions were not exercised.

## Reproduce the build

Run Gradle with JDK 21. The 2026 target also needs a JDK 25 toolchain.

```sh
./gradlew buildPlugin -DIDEA_VER=2025 --no-configuration-cache
./gradlew buildPlugin -DIDEA_VER=2026 --no-configuration-cache
```

In the [build workflow](../.github/workflows/build.yml), manual builds compile
and package by default. Enable the optional `validate` input to run the full
regression and compatibility checks when needed.
