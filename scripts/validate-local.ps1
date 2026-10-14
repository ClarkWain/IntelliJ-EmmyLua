param(
    [Parameter(Mandatory = $true)][string]$IdePath,
    [string]$IdeaVersion = '2025.2'
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
$validationDrive = $null
Push-Location $repoRoot
try {
    $env:JAVA_HOME = Join-Path $ideRoot 'jbr'
    if (-not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
        throw 'The IDE must include its JetBrains Runtime.'
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
    if ($validationDrive) { & subst.exe $validationDrive /D }
}
