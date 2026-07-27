[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$BackupPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Stop-Restore([string]$Message) {
    Write-Error $Message
    exit 1
}

function Invoke-MySqlQuery([string]$Query) {
    $output = & $script:mysql.Source "--database=$script:Database" '--batch' '--skip-column-names' '-e' $Query
    if ($LASTEXITCODE -ne 0) {
        throw "mysql verification command failed"
    }
    return ($output | Out-String).Trim()
}

function Quote-CmdArgument([string]$Value) {
    return '"' + $Value.Replace('"', '""') + '"'
}

function Start-DatabaseClient {
    param(
        [Parameter(Mandatory = $true)] $Command,
        [Parameter(Mandatory = $true)] [string[]]$ArgumentList,
        [string]$RedirectStandardInput,
        [string]$RedirectStandardOutput
    )

    $filePath = $Command.Source
    $arguments = $ArgumentList
    if ([System.IO.Path]::GetExtension($filePath) -in @('.cmd', '.bat')) {
        $commandLine = 'call ' + (Quote-CmdArgument $Command.Source) + ' ' +
                (($ArgumentList | ForEach-Object { Quote-CmdArgument $_ }) -join ' ') + ' & exit /b !errorlevel!'
        $filePath = $env:ComSpec
        $arguments = @('/d', '/v:on', '/c', $commandLine)
    }

    $startParameters = @{
        FilePath = $filePath
        ArgumentList = $arguments
        PassThru = $true
        Wait = $true
        NoNewWindow = $true
    }
    if ($RedirectStandardInput) {
        $startParameters.RedirectStandardInput = $RedirectStandardInput
    }
    if ($RedirectStandardOutput) {
        $startParameters.RedirectStandardOutput = $RedirectStandardOutput
    }
    return Start-Process @startParameters
}

$mysql = Get-Command mysql -ErrorAction SilentlyContinue
$mysqldump = Get-Command mysqldump -ErrorAction SilentlyContinue
if ($null -eq $mysql) {
    Stop-Restore 'Required command is not available: mysql'
}
if ($null -eq $mysqldump) {
    Stop-Restore 'Required command is not available: mysqldump'
}

if (-not (Test-Path -LiteralPath $BackupPath -PathType Leaf)) {
    Stop-Restore "Backup file does not exist or is not a regular file: $BackupPath"
}
$backupFile = (Resolve-Path -LiteralPath $BackupPath).Path

$Database = Read-Host 'Database name'
$databaseConfirmation = Read-Host 'Enter the same database name again'
if ($Database -ne $databaseConfirmation) {
    Stop-Restore 'Database names do not match'
}
if ($Database -notmatch '^[A-Za-z0-9_]{1,64}$') {
    Stop-Restore 'Database name may only contain letters, numbers, and underscores'
}

$maintenanceConfirmation = Read-Host 'Confirm the application is in maintenance mode by typing MAINTENANCE'
if ($maintenanceConfirmation -ne 'MAINTENANCE') {
    Stop-Restore 'Restore cancelled because maintenance mode was not confirmed'
}

$timestamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$safetyBackup = Join-Path (Split-Path -Parent $backupFile) "safety-$Database-$timestamp.sql"
Write-Host "Creating safety backup: $safetyBackup"
$safetyBackupProcess = Start-DatabaseClient -Command $mysqldump `
    -ArgumentList @('--databases', $Database, '--single-transaction', '--routines', '--events') `
    -RedirectStandardOutput $safetyBackup
if ($safetyBackupProcess.ExitCode -ne 0) {
    Stop-Restore "Safety backup failed. Restore was not started. Partial safety backup: $safetyBackup"
}
Get-FileHash -LiteralPath $safetyBackup -Algorithm SHA256 | ForEach-Object {
    "$($_.Hash) *$safetyBackup" | Set-Content -LiteralPath "$safetyBackup.sha256" -NoNewline
}
Write-Host "Safety backup checksum: $safetyBackup.sha256"

Write-Host "Restoring $backupFile into database $Database..."
$restoreProcess = Start-DatabaseClient -Command $mysql -ArgumentList @("--database=$Database") `
    -RedirectStandardInput $backupFile
if ($restoreProcess.ExitCode -ne 0) {
    Stop-Restore "Restore failed. The safety backup remains at: $safetyBackup"
}

try {
    foreach ($table in @('users', 'articles', 'comments')) {
        $tableCount = Invoke-MySqlQuery "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '$table';"
        if ($tableCount -ne '1') {
            throw "Core table is missing after restore: $table"
        }
        [void](Invoke-MySqlQuery "SELECT COUNT(*) FROM ``$table``;")
    }
    $foreignKeyChecks = Invoke-MySqlQuery 'SET FOREIGN_KEY_CHECKS = 1; SELECT @@FOREIGN_KEY_CHECKS;'
    if ($foreignKeyChecks -ne '1') {
        throw 'FOREIGN_KEY_CHECKS could not be restored'
    }
} catch {
    Stop-Restore "Post-restore verification failed. The safety backup remains at: $safetyBackup. $($_.Exception.Message)"
}

Write-Host "Restore completed and verified. Safety backup retained at: $safetyBackup"
