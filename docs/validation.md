# Lua Insight 1.5.2 release validation

Date: **2026-10-14**. Display name: **Lua Insight**. Plugin ID: **`com.weitp`**.

The release binaries come from source commit
`479e5a656f3cad66a03eb74c28c29267930f88e6` and the successful
[release CI run](https://github.com/ClarkWain/IntelliJ-EmmyLua/actions/runs/38514306565).
Later documentation changes do not change these binaries.

## Artifacts and scope

| Package | Compilation SDK | Supported platform range | Bytes |
| --- | --- | --- | --- |
| `Lua-Insight-1.5.2-IDEA2025.zip` | IDEA 2025.1.7.2 | 251–253 | 8,877,815 |
| `Lua-Insight-1.5.2-IDEA2026.zip` | IDEA 2026.1.5 | 261–262 | 8,878,519 |

```text
0f978abfa440483b46e1b053b56cdfeb3e7004f241f2d9aaf1f21adb54e9045a  Lua-Insight-1.5.2-IDEA2025.zip
eb5d83292c1813d43e13d2e0703ecd1fe82e0131c1a159d22bd4e20284d99877  Lua-Insight-1.5.2-IDEA2026.zip
```

Both ZIPs were inspected: a single `Lua-Insight` root, one plugin descriptor,
correct name/ID/version/build bounds, Lua standard libraries and packaged
Windows/macOS/Linux debugger binaries. The ID, extension points, indexes and
settings identifiers stay unchanged, allowing an existing installation to upgrade.

This release changes naming and package metadata. Every `.class` file in every
packaged JAR was compared with 1.5.1: **3,227** class files in the 2025 package and
**3,228** in the 2026 package are byte-for-byte identical. No implementation
bytecode changed.

## Automated checks of the rebuilt packages

Each CI target passed **109 tests** across **26 suites**, with **0 failures,
0 errors and 0 skipped**. Coverage includes type analysis, completion, parsing,
standard libraries, navigation/refactoring, editor behavior, formatting,
structure view and concurrent resolution.

IntelliJ Plugin Verifier **1.410** checked the rebuilt 1.5.2 binaries:

| Package | IDEA version | Build | Compatibility problems | Internal API usages | Warnings / deprecated / experimental usages |
| --- | --- | --- | --- | --- | --- |
| 2025 | 2025.1.7.2 | IU-251.29188.72 | 0 | 0 | 1 / 24 / 21 |
| 2025 | 2025.2.6.3 | IU-252.28539.97 | 0 | 0 | 1 / 24 / 21 |
| 2025 | 2025.3.6.1 | IU-253.33813.55 | 0 | 0 | 1 / 26 / 21 |
| 2026 | 2026.1.5 | IU-261.27258.48 | 0 | 0 | 1 / 32 / 21 |
| 2026 | 2026.2.3 | IU-262.10968.63 | 0 | 0 | 1 / 33 / 21 |

No failure categories were suppressed. The remaining compatibility warning
concerns the upstream bundled Markdown library; deprecated and experimental API
usages remain visible for future platform updates. Machine-readable hashes,
test counts and verifier verdicts are in [release-summary.json](validation/release-summary.json).
Full reports are attached to the CI run as `validation-IDEA2025` and `validation-IDEA2026`.

## Native loading and existing editor evidence

The exact new CI ZIPs were extracted into isolated native Windows IDE previews,
using separate configuration/system/plugin/log directories. Startup logs confirm:

| Application | Build and runtime | Loaded plugin |
| --- | --- | --- |
| Android Studio Otter 3 Feature Drop 2025.2.3 | AI-252.28238.7.2523.14688667; bundled JBR 21.0.8 | `Lua Insight (1.5.2-IDEA2025)` |
| IntelliJ IDEA 2026.2.3 | IU-262.10968.63; Eclipse Temurin 25.0.4.1+1-LTS | `Lua Insight (1.5.2-IDEA2026)` |

Interactive checks were performed on the bytecode-identical 1.5.1 binaries in
both applications. They were not repeated for the 1.5.2 metadata update.
`Box<Point>.value`
completion offered **x/y** typed **number**, the invalid sample showed the expected
four type-safety errors, and Quick Documentation rendered Markdown bold and
inline code with the correct parameter/return types.

The [preceding exact validation record](https://github.com/ClarkWain/IntelliJ-EmmyLua/blob/935c6789203317063bbe4a1bd65399e87436efd3/docs/validation.md)
retains its original artifact hashes, runtime details and screenshots. The
following images are that prior interactive evidence, not new 1.5.2 captures:

![Previous Android Studio completion check](validation/release-252-completion.png)

![Previous Android Studio Markdown check](validation/release-252-documentation.png)

![Previous IDEA 262 completion check](validation/release-262-completion.png)

![Previous IDEA 262 type-error check](validation/release-262-errors.png)

## Acceptance boundary and reproduction

Both rebuilt packages have fresh automated regression and compatibility checks,
metadata audits and native loading checks. The interactive functional evidence
comes from the preceding bytecode-identical version. IDE configuration and plugin
directories were isolated. Native debugger sessions and other operating systems
were not exercised.

The IDEA 262 preview uses the official cross-platform Maven SDK distribution
and an external JDK, rather than the full Windows installer with bundled JBR.
It logs unrelated Station/OS Integration/WSL startup errors, including a missing
native daemon archive; the loading check does not claim the whole IDE log is
error-free. No exceptions attributed to `com.weitp` were found in the new previews.

The [build workflow](../.github/workflows/build.yml) compiles each target against
the oldest SDK in its range, then runs tests and the five listed verifier checks.
Run Gradle using JDK 21; the 2026 target needs a JDK 25 toolchain. See the README
and `scripts/validate-local.ps1` for local builds and non-ASCII Windows paths.
Disable other Lua language plugins before enabling Lua Insight.
