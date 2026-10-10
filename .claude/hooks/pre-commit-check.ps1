# PreToolUse-Hook (Bash): das Tor vor dem Commit.
#
# Der Stop-Hook laeuft erst NACH der Antwort -- und der Commit ist der letzte
# Schritt darin. Er kann ihn also nicht mehr aufhalten, nur noch melden; der
# rote Commit steht dann schon in der Historie und die Korrektur kommt als
# zweiter obendrauf. Dieser Hook prueft dasselbe an der Stelle, an der es noch
# etwas nuetzt: unmittelbar vor `git commit`.
#
# Er laesst in fuenf Faellen ohne Pruefung durch -- jeder davon ist Absicht,
# nicht Nachlaessigkeit (siehe unten). Netto kostet er keinen zusaetzlichen
# Gradle-Lauf: Bei Gruen loescht er den Marker, sodass der Stop-Hook danach
# nichts mehr zu tun hat.

$ErrorActionPreference = "SilentlyContinue"

. (Join-Path $PSScriptRoot "gradle-check.ps1")

# --- 1. Ist es ueberhaupt ein Commit? ---------------------------------------
# Bewusst als Suche im ganzen Kommando statt als Praefix: Ein Commit kommt hier
# meist als `git add -A && git commit -F - <<'MSG'` an, nicht am Zeilenanfang.
# Deshalb auch kein `if`-Filter in settings.local.json -- dessen Praefix-Muster
# wuerde genau diesen Normalfall verfehlen.
$json = [Console]::In.ReadToEnd() | ConvertFrom-Json
$command = $json.tool_input.command
if ([string]::IsNullOrWhiteSpace($command)) { exit 0 }
if ($command -notmatch '\bgit\s+commit\b') { exit 0 }

# --- 2. Wurde ueberhaupt Kotlin angefasst? ----------------------------------
$marker = Get-MarkerPath -HookRoot $PSScriptRoot
if (-not (Test-Path $marker)) { exit 0 }

$projectRoot = Get-ProjectRoot -HookRoot $PSScriptRoot

Push-Location $projectRoot
try {
    $branch = (& git rev-parse --abbrev-ref HEAD 2>$null | Out-String).Trim()
    $gitDir = (& git rev-parse --git-dir 2>$null | Out-String).Trim()
} finally {
    Pop-Location
}

# --- 3. Steht der Branch, fuer den die Regel gilt? --------------------------
# `CLAUDE.md` schreibt `main` als Arbeitsbranch fest. Alles andere ist
# ein Wegwerf-Branch -- etwa der Prototyp-Branch aus `/prototype`, dessen Code
# detekt nie bestehen soll.
if ($branch -ne 'main') { exit 0 }

# --- 4. Laeuft gerade ein Rebase oder Merge? --------------------------------
# Zwischenstaende eines Rebase bauen haeufig nicht; `/resolving-merge-conflicts`
# committet sie trotzdem und ist damit im Recht.
if (-not [string]::IsNullOrWhiteSpace($gitDir)) {
    $gitDirFull = if ([System.IO.Path]::IsPathRooted($gitDir)) {
        $gitDir
    } else {
        Join-Path $projectRoot $gitDir
    }
    foreach ($inArbeit in @('rebase-merge', 'rebase-apply', 'MERGE_HEAD', 'CHERRY_PICK_HEAD', 'REVERT_HEAD')) {
        if (Test-Path (Join-Path $gitDirFull $inArbeit)) { exit 0 }
    }
}

# --- 5. Ist der Gradle-Daemon frei? -----------------------------------------
# Kuerzer als im Stop-Hook: Hier wartet ein Mensch auf den Commit. Bleibt der
# Daemon belegt, geht der Commit durch und der Marker bleibt stehen -- der
# Stop-Hook holt die Pruefung dann nach. Verschieben, nicht blockieren.
if (-not (Wait-ForFreeGradleDaemon -ProjectRoot $projectRoot -MaxWaitSeconds 120)) { exit 0 }

# --- 6. Pruefen ---------------------------------------------------------------
$result = Invoke-GradleCheck -ProjectRoot $projectRoot

if ($result.ExitCode -eq 0) {
    Remove-Item $marker -Force
    exit 0
}

# Der Marker bleibt bei Rot absichtlich stehen: Der naechste Commit-Versuch
# soll erneut geprueft werden, auch wenn zwischendurch nichts editiert wurde.
$summary = Get-CheckSummary -RawOutput $result.RawOutput
$payload = @{
    hookSpecificOutput = @{
        hookEventName            = "PreToolUse"
        permissionDecision       = "deny"
        permissionDecisionReason = "Der Commit wurde nicht ausgefuehrt: ``gradlew test detekt`` ist rot.`n`n$summary`n`nErst beheben (das ist wieder Schritt 2 der Umsetzung), dann erneut committen."
    }
}
$payload | ConvertTo-Json -Compress -Depth 5
exit 0
