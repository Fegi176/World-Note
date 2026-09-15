param([string]$Serial)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/env.ps1"
if (-not $Serial) {
    $connected = @(adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '^([^\s]+)\s+device$' -and $_ -notmatch '^emulator-' } | ForEach-Object { ($_ -split '\s+')[0] })
    if ($connected.Count -ne 1) { throw 'Connect and authorize exactly one phone, or pass -Serial. No app was changed.' }
    $Serial = $connected[0]
}
$phoneReady = @(adb devices | Where-Object { $_ -match ('^' + [regex]::Escape($Serial) + '\s+device\s*$') })
if ($phoneReady.Count -ne 1) { throw 'Phone is disconnected or unauthorized. Unlock it and accept Allow USB debugging. No app was changed.' }
$api = (adb -s $Serial shell getprop ro.build.version.sdk | Out-String).Trim()
if ($api -ne '34') { throw "This installer targets the owner's Android 14 phone; connected API is $api." }
$apk = Join-Path (Split-Path $PSScriptRoot -Parent) 'artifacts/World-Note-debug.apk'
if (-not (Test-Path -LiteralPath $apk)) { throw 'Build and package the APK first.' }
adb -s $Serial install -r $apk
if ($LASTEXITCODE -ne 0) { throw 'Update failed. Existing app data was not cleared; do not uninstall to fix a signing conflict.' }
adb -s $Serial shell am start -W -n 'app.nodenote.worldbuilder.debug/app.nodenote.worldbuilder.MainActivity'
if ($LASTEXITCODE -ne 0) { throw 'APK installed, but launch needs inspection.' }
adb -s $Serial shell dumpsys package app.nodenote.worldbuilder.debug | Select-String -Pattern 'versionCode=|versionName='
