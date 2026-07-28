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

function Test-ReparsePoint([string]$Path) {
    return (Get-Item -LiteralPath $Path -Force).Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)
}

function Set-PrivateAcl([string]$Path, [bool]$Directory) {
    $currentIdentity = [Security.Principal.WindowsIdentity]::GetCurrent().User
    $acl = if ($Directory) {
        New-Object Security.AccessControl.DirectorySecurity
    } else {
        New-Object Security.AccessControl.FileSecurity
    }
    $acl.SetAccessRuleProtection($true, $false)
    $inheritance = if ($Directory) {
        [Security.AccessControl.InheritanceFlags]::ContainerInherit -bor [Security.AccessControl.InheritanceFlags]::ObjectInherit
    } else {
        [Security.AccessControl.InheritanceFlags]::None
    }
    $rule = New-Object Security.AccessControl.FileSystemAccessRule(
        $currentIdentity, [Security.AccessControl.FileSystemRights]::FullControl, $inheritance,
        [Security.AccessControl.PropagationFlags]::None, [Security.AccessControl.AccessControlType]::Allow)
    $acl.AddAccessRule($rule)
    Set-Acl -LiteralPath $Path -AclObject $acl

    $unexpectedAllow = (Get-Acl -LiteralPath $Path).Access | Where-Object {
        $_.AccessControlType -eq [Security.AccessControl.AccessControlType]::Allow -and
        $_.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value -ne $currentIdentity.Value
    }
    if ($unexpectedAllow) {
        throw "Unsafe ACL on $Path"
    }
}

function Get-SafetyDirectory {
    $requested = if ([string]::IsNullOrWhiteSpace($env:BLOG_RESTORE_SAFETY_DIR)) {
        Join-Path $PSScriptRoot '.restore-safety'
    } else {
        $env:BLOG_RESTORE_SAFETY_DIR
    }
    $requestedFullPath = [IO.Path]::GetFullPath($requested)
    $requestedRoot = [IO.Path]::GetPathRoot($requestedFullPath)
    $requestedLeaf = [IO.Path]::GetFileName($requested.TrimEnd([char[]]@('\', '/')))
    if ([string]::IsNullOrWhiteSpace($requestedLeaf) -or $requestedLeaf -in @('.', '..') -or
        $requestedFullPath.TrimEnd([char[]]@('\', '/')) -eq $requestedRoot.TrimEnd([char[]]@('\', '/'))) {
        throw "Safety directory must be a named child directory: $requested"
    }

    if (Test-Path -LiteralPath $requested) {
        if (Test-ReparsePoint $requested) {
            throw "Safety directory must not be a reparse point: $requested"
        }
        if (-not (Test-Path -LiteralPath $requested -PathType Container)) {
            throw "Safety directory is not a directory: $requested"
        }
    } else {
        [IO.Directory]::CreateDirectory($requested) | Out-Null
    }

    $directory = (Resolve-Path -LiteralPath $requested).Path
    if (Test-ReparsePoint $directory) {
        throw "Safety directory must not be a reparse point: $directory"
    }
    Set-PrivateAcl -Path $directory -Directory $true
    return $directory
}

function New-PrivateSafetyFile([string]$Extension) {
    for ($attempt = 0; $attempt -lt 10; $attempt++) {
        $candidate = Join-Path $script:SafetyDirectory "safety-$script:Database-$([Guid]::NewGuid().ToString('N')).$Extension"
        try {
            $stream = [IO.File]::Open($candidate, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
            $stream.Dispose()
            if (Test-ReparsePoint $candidate) {
                throw "Safety file must not be a reparse point: $candidate"
            }
            Set-PrivateAcl -Path $candidate -Directory $false
            return $candidate
        } catch [IO.IOException] {
            continue
        }
    }
    throw 'Could not atomically create a private safety file'
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

function New-RestoreInput([string]$SourceFile) {
    $restoreInput = New-PrivateSafetyFile 'restore.sql'
    $output = [IO.File]::Open($restoreInput, [IO.FileMode]::Truncate, [IO.FileAccess]::Write, [IO.FileShare]::None)
    try {
        $input = [IO.File]::OpenRead($SourceFile)
        try {
            $input.CopyTo($output)
        } finally {
            $input.Dispose()
        }
        $tail = [Text.Encoding]::UTF8.GetBytes("`nSET FOREIGN_KEY_CHECKS=1;`n")
        $output.Write($tail, 0, $tail.Length)
    } finally {
        $output.Dispose()
    }
    return $restoreInput
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

try {
    $SafetyDirectory = Get-SafetyDirectory
} catch {
    Stop-Restore $_.Exception.Message
}
$safetyBackup = New-PrivateSafetyFile 'sql'
Write-Host "Creating safety backup: $safetyBackup"
$safetyBackupProcess = Start-DatabaseClient -Command $mysqldump `
    -ArgumentList @('--databases', $Database, '--single-transaction', '--routines', '--events') `
    -RedirectStandardOutput $safetyBackup
if ($safetyBackupProcess.ExitCode -ne 0) {
    Stop-Restore "Safety backup failed. Restore was not started. Partial safety backup: $safetyBackup"
}
$checksumFile = New-PrivateSafetyFile 'sha256'
Get-FileHash -LiteralPath $safetyBackup -Algorithm SHA256 | ForEach-Object {
    "$($_.Hash) *$safetyBackup" | Set-Content -LiteralPath $checksumFile -NoNewline
}
Set-PrivateAcl -Path $checksumFile -Directory $false
Write-Host "Safety backup checksum: $checksumFile"

Write-Host "Restoring $backupFile into database $Database..."
$restoreInput = New-RestoreInput $backupFile
try {
    $restoreProcess = Start-DatabaseClient -Command $mysql -ArgumentList @("--database=$Database") `
        -RedirectStandardInput $restoreInput
} finally {
    Remove-Item -LiteralPath $restoreInput -Force -ErrorAction SilentlyContinue
}
if ($restoreProcess.ExitCode -ne 0) {
    Stop-Restore "Restore failed. The safety backup remains at: $safetyBackup"
}

try {
    foreach ($table in @('users', 'articles', 'comments')) {
        $tableCount = Invoke-MySqlQuery "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '$table';"
        if ($tableCount -ne '1') {
            throw "Core table is missing after restore: $table"
        }
    }
    $orphanCount = Invoke-MySqlQuery 'SELECT (SELECT COUNT(*) FROM articles a LEFT JOIN users u ON a.author_id = u.id WHERE u.id IS NULL) + (SELECT COUNT(*) FROM articles a LEFT JOIN categories c ON a.category_id = c.id WHERE c.id IS NULL) + (SELECT COUNT(*) FROM comments c LEFT JOIN articles a ON c.article_id = a.id WHERE a.id IS NULL) + (SELECT COUNT(*) FROM comments c LEFT JOIN users u ON c.user_id = u.id WHERE u.id IS NULL);'
    if ($orphanCount -ne '0') {
        throw "Foreign-key integrity check found $orphanCount orphaned rows"
    }
} catch {
    Stop-Restore "Post-restore verification failed. The safety backup remains at: $safetyBackup. $($_.Exception.Message)"
}

Write-Host "Restore completed and verified. Safety backup retained at: $safetyBackup"
