# Stop-Hook: laeuft einmal am Ende jeder Antwort und ist das **Netz** hinter
# pre-commit-check.ps1. Er faengt alles, was ohne Commit endet -- eine Frage
# zwischendurch, abgebrochene Arbeit, oder einen Commit, den das Tor bei
# belegtem Gradle-Daemon durchgelassen hat.
#
# Gefahren wird nur, wenn im Turn tatsaechlich eine .kt-Datei angefasst wurde
# (Marker von mark-kt-touched.ps1) und das Tor ihn nicht schon abgeraeumt hat.
# Bei "alles gruen" gibt es KEINEN Output (kostet keine Tokens).
#
# Laeuft z.B. bereits ein manuell/im Hintergrund gestarteter Gradle-Build fuer
# dasselbe Projekt (etwa ein instrumentierter Test), NICHT parallel dagegen
# antreten -- siehe die Begruendung an Wait-ForFreeGradleDaemon in
# gradle-check.ps1. Ist der Daemon nach der Wartezeit immer noch belegt, die
# Pruefung auf den naechsten Stop verschieben (Marker bleibt bestehen) statt
# entweder zu blockieren oder sie ganz auszulassen.

$ErrorActionPreference = "SilentlyContinue"

. (Join-Path $PSScriptRoot "gradle-check.ps1")

$marker = Get-MarkerPath -HookRoot $PSScriptRoot
if (-not (Test-Path $marker)) {
    exit 0
}

$projectRoot = Get-ProjectRoot -HookRoot $PSScriptRoot

if (-not (Wait-ForFreeGradleDaemon -ProjectRoot $projectRoot -MaxWaitSeconds 480)) {
    # Noch immer belegt: Marker NICHT loeschen, damit die Pruefung beim
    # naechsten Stop erneut versucht wird, statt verloren zu gehen.
    exit 0
}

Remove-Item $marker -Force
$result = Invoke-GradleCheck -ProjectRoot $projectRoot

if ($result.ExitCode -eq 0) {
    exit 0
}

$summary = Get-CheckSummary -RawOutput $result.RawOutput
$payload = @{
    decision = "block"
    reason   = "Automatischer gradlew test/detekt-Lauf ist fehlgeschlagen:`n`n$summary"
}
$payload | ConvertTo-Json -Compress
exit 0
