# Gemeinsame Pruef-Logik der beiden Gradle-Hooks:
#
#   pre-commit-check.ps1  laeuft VOR `git commit` und kann ihn verhindern.
#   stop-check.ps1        laeuft NACH der Antwort und ist das Netz fuer alles,
#                         was ohne Commit endet.
#
# Beide fahren denselben Lauf (`gradlew test detekt`) und verdichten seine
# Ausgabe auf dieselbe Weise. Die Logik steht deshalb hier einmal statt zweimal
# -- sonst laeuft sie ueber die Zeit auseinander.
#
# Einbindung per Dot-Sourcing:  . (Join-Path $PSScriptRoot "gradle-check.ps1")

# Die Repo-Wurzel, von .claude/hooks/ aus zwei Ebenen hoeher.
function Get-ProjectRoot {
    param([Parameter(Mandatory)][string]$HookRoot)
    return (Resolve-Path (Join-Path $HookRoot "..\..")).Path
}

# Der Marker, den mark-kt-touched.ps1 setzt, sobald eine .kt-Datei angefasst
# wurde. Ohne ihn gibt es nichts zu pruefen.
function Get-MarkerPath {
    param([Parameter(Mandatory)][string]$HookRoot)
    return (Join-Path $HookRoot ".pending-check")
}

# Wartet begrenzt auf einen freien Gradle-Daemon und meldet, ob er frei wurde.
#
# Gradle serialisiert Builds ueber einen Projekt-Lock. Laeuft schon ein Build
# (etwa ein instrumentierter Test im Hintergrund), wuerde ein zweiter Aufruf
# darauf warten und das Hook-Timeout sprengen -- daraus entstand frueher eine
# Block-Retry-Schleife, die sich nur durch Neustart von Android Studio beenden
# liess. Deshalb: begrenzt warten, und im Zweifel $false melden. Die Aufrufer
# lassen dann durch, statt zu blockieren.
function Wait-ForFreeGradleDaemon {
    param(
        [Parameter(Mandatory)][string]$ProjectRoot,
        [int]$MaxWaitSeconds = 480,
        [int]$PollSeconds = 5
    )
    Push-Location $ProjectRoot
    try {
        $waited = 0
        while ($true) {
            $status = & .\gradlew.bat --status 2>&1 | Out-String
            if ($status -notmatch 'BUSY') { return $true }
            if ($waited -ge $MaxWaitSeconds) { return $false }
            Start-Sleep -Seconds $PollSeconds
            $waited += $PollSeconds
        }
    } finally {
        Pop-Location
    }
}

# Faehrt Unit-Tests und statische Analyse in einem Lauf. `--continue` sorgt
# dafuer, dass beide Ergebnisse in einer Meldung stehen, statt dass der erste
# Fehlschlag den zweiten verdeckt.
#
# Das lokale `$ErrorActionPreference = 'Continue'` ist kein Schoenheitsfehler,
# sondern noetig: Die Hooks setzen global "SilentlyContinue", und damit
# verschluckt PowerShell die per `2>&1` eingesammelten stderr-Zeilen. detekt
# schreibt seine Fundstellen auf stdout und kam durch -- der Kotlin-Compiler
# schreibt `e: Datei.kt:12:34 ...` auf stderr, und genau die Zeile fehlte in
# jeder Fehlermeldung. Die Zuweisung gilt nur in dieser Funktion.
#
# Die Zeilen werden einzeln in Text uebersetzt statt durch `Out-String`
# geschickt: Der Fehler-Formatierer bricht stderr-Zeilen auf Konsolenbreite um
# (die Fundstelle stand dann halb in der Zeile davor) und haengt einen
# CategoryInfo-Block an. Bei einem ErrorRecord ist die Nachricht der Text --
# `ToString()` liefert bei leeren stderr-Zeilen nur "RemoteException", und die
# rutschte durch den Filter, weil dort "Exception" steht.
function Invoke-GradleCheck {
    param([Parameter(Mandatory)][string]$ProjectRoot)
    $ErrorActionPreference = 'Continue'
    Push-Location $ProjectRoot
    try {
        $zeilen = & .\gradlew.bat test detekt --continue --console=plain 2>&1 |
            ForEach-Object {
                if ($_ -is [System.Management.Automation.ErrorRecord]) {
                    $_.Exception.Message
                } else {
                    [string]$_
                }
            }
        return @{ ExitCode = $LASTEXITCODE; RawOutput = ($zeilen -join "`n") }
    } finally {
        Pop-Location
    }
}

# Verdichtet die Gradle-Ausgabe auf das, was Claude braucht: erst determinis-
# tisch filtern, und nur wenn danach immer noch zu viel uebrig ist, durch eine
# lokale Ollama-Analyse schicken. Die ist optional und nicht Teil dieses Repos:
# Fehlt sie, wird gekuerzt. Der Filter kommt zuerst, weil er verlaesslich ist
# und nichts kostet.
#
# Erkannt wird die **Form** einer Fundstelle statt einer Liste von Regelnamen.
# Die fruehere Liste zaehlte MagicNumber, WildcardImport, MaxLineLength und
# NewLineAtEndOfFile auf -- jede andere detekt-Regel fiel still heraus und
# uebrig blieb ein nacktes "> Task :app:detekt FAILED" ohne Fundstelle.
#
# Die beiden Werkzeuge schreiben ihre Fundstelle leicht unterschiedlich:
# detekt als `Datei.kt:12:34: Meldung`, der Kotlin-Compiler als
# `Datei.kt:12:34 Meldung` ohne Doppelpunkt. Deshalb `[:\s]` am Ende.
function Get-CheckSummary {
    param([Parameter(Mandatory)][AllowEmptyString()][string]$RawOutput)

    $relevantLines = $RawOutput -split "`r?`n" | Where-Object {
        $_ -match '\.kts?:\d+(:\d+)?[:\s]|FAILED|error:|Exception|weighted issues|tests completed'
    }
    $filtered = ($relevantLines | Select-Object -First 80) -join "`n"

    if ([string]::IsNullOrWhiteSpace($filtered)) {
        $filtered = ($RawOutput -split "`r?`n" | Select-Object -Last 40) -join "`n"
    }

    if ($filtered.Length -le 3000) {
        return $filtered
    }

    $analyzeCli = Join-Path $env:USERPROFILE "Tools\mcp-ollama-analyzer\analyze-cli.js"
    if (Test-Path $analyzeCli) {
        $env:OLLAMA_HOST = "http://localhost:11434"
        $condensed = $filtered | node $analyzeCli "Fasse die Test-/Detekt-Fehlschlaege kompakt zusammen, nenne zuerst die Ursache." 2>$null
        if (-not [string]::IsNullOrWhiteSpace($condensed)) {
            return $condensed
        }
    }
    return $filtered.Substring(0, 3000) + "`n(...gekuerzt)"
}
