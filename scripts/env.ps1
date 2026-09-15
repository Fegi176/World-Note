$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$localJdk = Get-ChildItem -Directory -LiteralPath "$projectRoot/.tooling/jdk" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName }
if (Test-Path -LiteralPath "$projectRoot/.tooling/android-sdk") { $env:ANDROID_HOME = "$projectRoot/.tooling/android-sdk" }
if ($env:JAVA_HOME) { $env:Path = "$env:JAVA_HOME/bin;$env:Path" }
if ($env:ANDROID_HOME) { $env:Path = "$env:ANDROID_HOME/platform-tools;$env:ANDROID_HOME/cmdline-tools/latest/bin;$env:ANDROID_HOME/build-tools/35.0.0;$env:Path" }
