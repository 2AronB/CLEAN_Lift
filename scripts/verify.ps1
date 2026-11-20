param(
    [switch]$DatabaseTests,
    [string]$EmulatorSerial = 'emulator-5554'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$studioRuntime = 'C:\Program Files\Android\Android Studio\jbr'
if (-not (Test-Path -LiteralPath "$studioRuntime\bin\java.exe")) {
    throw 'Android Studio Java runtime not found. Update studioRuntime in this script for the installed Studio location.'
}
$env:JAVA_HOME = $studioRuntime
$env:GRADLE_USER_HOME = "$env:USERPROFILE\.gradle"
$env:GRADLE_OPTS = "-Duser.home=$env:USERPROFILE"
$env:ANDROID_USER_HOME = "$env:USERPROFILE\.android"
$targets = @(':app:assembleDebug', ':app:testDebugUnitTest', ':app:lintDebug')
if ($DatabaseTests) {
    $env:ANDROID_SERIAL = $EmulatorSerial
    $targets += ':app:connectedDebugAndroidTest'
}
Push-Location -LiteralPath $projectRoot
try {
    & .\gradlew.bat @targets --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Verification failed with exit code $LASTEXITCODE. See Gradle output above." }
} finally { Pop-Location }
