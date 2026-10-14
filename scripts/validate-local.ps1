param(
    [Parameter(Mandatory = $true)][string]$IdePath,
    [ValidateSet('2025', '2026')][string]$IdeaVersion = '2025',
    [string]$GradleJavaHome
)
$ErrorActionPreference = 'Stop'
$ideRoot = (Resolve-Path -LiteralPath $IdePath).Path
$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
if (-not (Test-Path -LiteralPath (Join-Path $ideRoot 'product-info.json'))) {
    throw 'IdePath must point to an installed IntelliJ-based IDE.'
}
$product = Get-Content -LiteralPath (Join-Path $ideRoot 'product-info.json') -Raw | ConvertFrom-Json
Write-Host "Validating against $($product.name) $($product.version), build $($product.buildNumber)"
$previousJavaHome = $env:JAVA_HOME
$previousJavaHome25 = $env:JAVA_HOME_25_X64
$validationDrive = $null
Push-Location $repoRoot
try {
    $ideJbr = Join-Path $ideRoot 'jbr'
    if ($GradleJavaHome) {
        $env:JAVA_HOME = (Resolve-Path -LiteralPath $GradleJavaHome).Path
    } elseif (Test-Path -LiteralPath (Join-Path $ideJbr 'bin/java.exe')) {
        $env:JAVA_HOME = $ideJbr
    }
    if (-not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
        throw 'Provide a JDK 21 path using -GradleJavaHome.'
    }
    $javaRelease = Get-Content -LiteralPath (Join-Path $env:JAVA_HOME 'release') -Raw
    if ($javaRelease -notmatch 'JAVA_VERSION="21[.\"]') {
        throw 'Gradle 8.14 requires JDK 21 for this project. Provide -GradleJavaHome pointing to JDK 21.'
    }
    if ($IdeaVersion -eq '2026' -and (Test-Path -LiteralPath (Join-Path $ideJbr 'release'))) {
        $jbrRelease = Get-Content -LiteralPath (Join-Path $ideJbr 'release') -Raw
        if ($jbrRelease -match 'JAVA_VERSION="25[.\"]') { $env:JAVA_HOME_25_X64 = $ideJbr }
    }
    # Windows Java argument files can misread a non-ASCII checkout path.
    # Map the existing checkout without moving or copying any source files.
    if ($repoRoot -match '[^\x00-\x7F]') {
        foreach ($letter in @('L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z')) {
            if (-not (Get-PSDrive -Name $letter -ErrorAction SilentlyContinue) -and -not (Test-Path "$letter`:\")) {
                $validationDrive = "$letter`:"
                break
            }
        }
        if (-not $validationDrive) { throw 'No unused drive letter available for an ASCII path mapping.' }
        & subst.exe $validationDrive $repoRoot
        if ($LASTEXITCODE -ne 0) { throw 'Failed to create temporary checkout drive mapping.' }
        Set-Location "$validationDrive\"
    }
    & .\gradlew.bat test buildPlugin verifyPlugin "-PlocalIdePath=$($ideRoot.Replace('\', '/'))" "-DIDEA_VER=$IdeaVersion" --no-configuration-cache --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Validation failed (Gradle exit $LASTEXITCODE). See build/reports." }
} finally {
    Pop-Location
    $env:JAVA_HOME = $previousJavaHome
    $env:JAVA_HOME_25_X64 = $previousJavaHome25
    if ($validationDrive) { & subst.exe $validationDrive /D }
}
