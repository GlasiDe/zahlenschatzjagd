# PreToolUse-Hook (Bash/PowerShell): schuetzt echte Geraete vor `connectedAndroidTest`.
#
# WARUM ES DIESEN HOOK GIBT -- ein realer Verlust:
# Waehrend ein Geraet per WLAN mit adb verbunden war, lief ein
# `gradlew connectedAndroidTest`. Der Lauf geht auf *alle* verbundenen Geraete.
# Auf dem Geraet lag der signierte Release-Build; die Test-App ist ein Debug-Build
# mit anderer Signatur. Gradle loest das, indem es die vorhandene App
# **deinstalliert** -- und mit ihr die Datenbank. Der Fortschritt war weg.
#
# Die App traegt `allowBackup=false`
# (`docs/adr/0022-kein-android-auto-backup.md`), es gibt also keinen zweiten
# Rettungsweg -- nur die von Hand angelegte Sicherung.
#
# Der Hook laesst Emulatoren durch und blockiert alles andere. Wer wirklich auf
# einem echten Geraet instrumentiert testen will, trennt es vorher
# (`adb disconnect` / Kabel ab) oder setzt ANDROID_SERIAL auf den Emulator --
# beides sagt der Hook in seiner Meldung.

$ErrorActionPreference = "SilentlyContinue"

$json = [Console]::In.ReadToEnd() | ConvertFrom-Json
$command = $json.tool_input.command
if ([string]::IsNullOrWhiteSpace($command)) { exit 0 }

# Nur die Gradle-Aufgaben, die tatsaechlich installieren. `assembleRelease`,
# `test` und `detekt` fassen kein Geraet an.
if ($command -notmatch 'connected(Debug)?(Android)?Test|installDebug|installRelease') { exit 0 }

# Laeuft der Aufruf bereits gezielt gegen ein Geraet, entscheidet der Aufrufer.
if ($command -match 'ANDROID_SERIAL\s*=') { exit 0 }

$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { exit 0 }

$zeilen = & $adb devices 2>$null
if (-not $zeilen) { exit 0 }

# Alles, was nicht `emulator-<port>` heisst, ist ein echtes Geraet -- auch die
# langen `adb-<seriennummer>-<x>._adb-tls-connect._tcp`-Namen der WLAN-Kopplung.
$echteGeraete = @()
foreach ($zeile in $zeilen) {
    if ($zeile -match '^(\S+)\s+device\b') {
        $id = $Matches[1]
        if ($id -notmatch '^emulator-\d+$') { $echteGeraete += $id }
    }
}

if ($echteGeraete.Count -eq 0) { exit 0 }

$liste = ($echteGeraete -join ", ")
$grund = @"
Abgebrochen: An adb haengt ein echtes Geraet ($liste).

``connectedAndroidTest`` laeuft auf *allen* verbundenen Geraeten und
**deinstalliert** dort eine App mit abweichender Signatur -- auf einem
Geraet mit Release-Build kostet das den Fortschritt der Kinder. Genau das ist
schon einmal passiert.

Zwei Wege weiter:
  1. Geraet trennen: ``adb disconnect $($echteGeraete[0])`` -- danach laeuft es
     nur noch auf dem Emulator.
  2. Lauf festnageln: ``ANDROID_SERIAL=emulator-5554 ./gradlew connectedAndroidTest``

Zum Aufspielen einer neuen Version ist ohnehin nicht dieser Befehl gedacht,
sondern ``adb -s <geraet> install -r <apk>`` -- der ersetzt, ohne zu loeschen.
"@

$payload = @{
    hookSpecificOutput = @{
        hookEventName            = "PreToolUse"
        permissionDecision       = "deny"
        permissionDecisionReason = $grund
    }
}
$payload | ConvertTo-Json -Compress -Depth 5
exit 0
