param([switch]$Device)
$ErrorActionPreference='Stop'
. "$PSScriptRoot/env.ps1"
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    & ./gradlew.bat :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
    if($LASTEXITCODE -ne 0) { throw "Deterministic verification failed: exit $LASTEXITCODE" }
    if($Device) {
        if(-not $env:ANDROID_SERIAL) { throw 'Set ANDROID_SERIAL to a dedicated emulator/test device before instrumentation.' }
        & ./gradlew.bat :app:connectedDebugAndroidTest --console=plain
        if($LASTEXITCODE -ne 0) { throw "Instrumentation failed: exit $LASTEXITCODE" }
    } else { Write-Output 'Instrumentation not run by this command; use -Device with ANDROID_SERIAL.' }
} finally { Pop-Location }
