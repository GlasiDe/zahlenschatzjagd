# PostToolUse-Hook (Write|Edit): setzt einen Marker, wenn eine .kt-Datei
# angefasst wurde. stop-check.ps1 prueft/loescht diesen Marker und laesst
# gradlew nur dann laufen, wenn im Turn tatsaechlich Kotlin-Code geaendert
# wurde -- spart den Gradle-Lauf bei reinen Frage-/Doku-Antworten.

$ErrorActionPreference = "SilentlyContinue"

$json = [Console]::In.ReadToEnd() | ConvertFrom-Json
$filePath = $json.tool_input.file_path

if ($filePath -and $filePath -match '\.kt$') {
    New-Item -ItemType File -Path (Join-Path $PSScriptRoot ".pending-check") -Force | Out-Null
}

exit 0
