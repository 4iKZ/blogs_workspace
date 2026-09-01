package com.blog.ops;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Requires WSL Ubuntu-22.04 for bash script testing; enable on Linux CI")
class OfflineDatabaseRestoreScriptContractTest {

    private static final Path BASH_SCRIPT = Path.of("scripts", "restore-backup.sh").toAbsolutePath();
    private static final Path POWERSHELL_SCRIPT = Path.of("scripts", "restore-backup.ps1").toAbsolutePath();

    @TempDir
    Path tempDir;

    private Path backupFile;
    private Path fakeBin;
    private Path invocationLog;
    private Path safetyRoot;
    private Path restoreInputCapture;
    private String bashSafetyRoot;

    @BeforeEach
    void setUp() throws IOException {
        backupFile = Files.writeString(tempDir.resolve("restore input.sql"), "-- test backup\n");
        fakeBin = Files.createDirectory(tempDir.resolve("fake-bin"));
        invocationLog = tempDir.resolve("mysql-invocations.log");
        safetyRoot = tempDir.resolve("controlled-safety");
        restoreInputCapture = tempDir.resolve("restore-input.sql");
        bashSafetyRoot = "/tmp/blog-restore-safety-" + java.util.UUID.randomUUID();
    }

    @Test
    void bash_shouldStopBeforeRestoreWhenMysqlClientsAreMissing() throws Exception {
        assumeWslAvailable();

        ProcessResult result = runBash(Map.of("PATH", toWslPath(tempDir.resolve("empty-bin"))), "");

        assertEquals(1, result.exitCode());
        assertTrue(result.output().contains("Required command is not available: mysql"), result.output());
        assertFalse(Files.exists(invocationLog));
    }

    @Test
    void bash_shouldRejectMismatchedDatabaseAndMaintenanceConfirmationBeforeSafetyBackup() throws Exception {
        assumeWslAvailable();
        writeBashClients();

        ProcessResult mismatch = runBash(fakeEnvironment(), "blog\nother\n");
        assertEquals(1, mismatch.exitCode());
        assertTrue(mismatch.output().contains("Database names do not match"), mismatch.output());
        assertNoSafetyBackup();

        ProcessResult maintenance = runBash(fakeEnvironment(), "blog\nblog\nNO\n");
        assertEquals(1, maintenance.exitCode());
        assertTrue(maintenance.output().contains("maintenance mode was not confirmed"), maintenance.output());
        assertNoSafetyBackup();
    }

    @Test
    void bash_shouldKeepSafetyBackupWhenBackupOrRestoreFails() throws Exception {
        assumeWslAvailable();
        writeBashClients();

        ProcessResult safetyFailure = runBash(withFlag("MOCK_SAFETY_FAIL", "1"), "blog\nblog\nMAINTENANCE\n");
        assertEquals(1, safetyFailure.exitCode());
        assertTrue(safetyFailure.output().contains("Safety backup failed"));
        assertFalse(Files.readString(invocationLog).contains("--database=blog"),
                "Restore must not start when the safety backup fails");

        ProcessResult restoreFailure = runBash(withFlag("MOCK_RESTORE_FAIL", "1"), "blog\nblog\nMAINTENANCE\n");
        assertEquals(1, restoreFailure.exitCode(), restoreFailure.output());
        assertTrue(restoreFailure.output().contains("Restore failed. The safety backup remains at:"));
        assertTrue(bashSafetyBackups().size() >= 2,
                "Atomic file creation must retain separate safety backups across failed attempts");

        ProcessResult integrityFailure = runBash(withFlag("MOCK_INTEGRITY_FAIL", "1"),
                "blog\nblog\nMAINTENANCE\n");
        assertEquals(1, integrityFailure.exitCode(), integrityFailure.output());
        assertTrue(integrityFailure.output().contains("Foreign-key integrity check found 1 orphaned rows"));
    }

    @Test
    void bash_shouldRejectSymbolicLinkSafetyDirectory() throws Exception {
        assumeWslAvailable();
        writeBashClients();
        String target = "/tmp/blog-restore-safety-target-" + java.util.UUID.randomUUID();
        ProcessResult created = run(List.of("wsl.exe", "-d", "Ubuntu-22.04", "--", "mkdir", "-m", "700", target),
                "", Map.of());
        assertEquals(0, created.exitCode(), created.output());
        ProcessResult linked = run(List.of("wsl.exe", "-d", "Ubuntu-22.04", "--", "ln", "-s", target, bashSafetyRoot),
                "", Map.of());
        assertEquals(0, linked.exitCode(), linked.output());
        try {
            ProcessResult result = runBash(fakeEnvironment(), "blog\nblog\nMAINTENANCE\n");
            assertEquals(1, result.exitCode());
            assertTrue(result.output().contains("must not be a symbolic link"), result.output());
            ProcessResult files = run(List.of("wsl.exe", "-d", "Ubuntu-22.04", "--", "find", target, "-type", "f"),
                    "", Map.of());
            assertEquals("", files.output().trim(), files.output());
        } finally {
            run(List.of("wsl.exe", "-d", "Ubuntu-22.04", "--", "rm", "-rf", bashSafetyRoot, target), "", Map.of());
        }
    }

    @Test
    void bash_shouldVerifyCoreTablesAndForeignKeysAfterSuccessfulRestore() throws Exception {
        assumeWslAvailable();
        writeBashClients();

        ProcessResult result = runBash(fakeEnvironment(), "blog\nblog\nMAINTENANCE\n");

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("Restore completed and verified"));
        String log = Files.readString(invocationLog);
        assertTrue(log.contains("users"));
        assertTrue(log.contains("articles"));
        assertTrue(log.contains("comments"));
        assertTrue(Files.readString(restoreInputCapture).contains("SET FOREIGN_KEY_CHECKS=1;"));
        assertBashPrivateSafetyPermissions();
    }

    @Test
    void powershell_shouldStopBeforeRestoreWhenMysqlClientsAreMissing() throws Exception {
        assumePowerShellAvailable();
        ProcessResult result = runPowerShell(Map.of("Path", fakeBin.toString()), "");

        assertEquals(1, result.exitCode());
        assertTrue(result.output().contains("Required command is not available: mysql"));
        assertFalse(Files.exists(invocationLog));
    }

    @Test
    void powershell_shouldRejectConfirmationsAndKeepSafetyBackupOnRestoreFailure() throws Exception {
        assumePowerShellAvailable();
        writePowerShellClients();

        ProcessResult mismatch = runPowerShell(powerShellEnvironment(), "blog\nother\n");
        assertEquals(1, mismatch.exitCode());
        assertTrue(mismatch.output().contains("Database names do not match"), mismatch.output());
        assertNoSafetyBackup();

        ProcessResult maintenance = runPowerShell(powerShellEnvironment(), "blog\nblog\nNO\n");
        assertEquals(1, maintenance.exitCode());
        assertTrue(maintenance.output().contains("maintenance mode was not confirmed"), maintenance.output());
        assertNoSafetyBackup();

        ProcessResult safetyFailure = runPowerShell(withPowerShellFlag("MOCK_SAFETY_FAIL", "1"),
                "blog\nblog\nMAINTENANCE\n");
        assertEquals(1, safetyFailure.exitCode(), safetyFailure.output());
        assertTrue(safetyFailure.output().contains("Safety backup failed"), safetyFailure.output());
        assertFalse(Files.readString(invocationLog).contains("--database=blog"),
                "Restore must not start when the safety backup fails");

        ProcessResult restoreFailure = runPowerShell(withPowerShellFlag("MOCK_RESTORE_FAIL", "1"),
                "blog\nblog\nMAINTENANCE\n");
        assertEquals(1, restoreFailure.exitCode(), restoreFailure.output());
        assertTrue(restoreFailure.output().contains("Restore failed. The safety backup remains at:"));
        assertTrue(safetyBackups().size() >= 2,
                "CreateNew must retain separate safety backups across failed attempts");

        ProcessResult integrityFailure = runPowerShell(withPowerShellFlag("MOCK_INTEGRITY_FAIL", "1"),
                "blog\nblog\nMAINTENANCE\n");
        assertEquals(1, integrityFailure.exitCode(), integrityFailure.output());
        assertTrue(integrityFailure.output().contains("Foreign-key integrity check found 1 orphaned rows"));
    }

    @Test
    void powershell_shouldVerifyCoreTablesAndForeignKeysAfterSuccessfulRestore() throws Exception {
        assumePowerShellAvailable();
        writePowerShellClients();

        ProcessResult result = runPowerShell(powerShellEnvironment(), "blog\nblog\nMAINTENANCE\n");

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("Restore completed and verified"));
        String log = Files.readString(invocationLog);
        assertTrue(log.contains("core-check"));
        assertTrue(log.contains("integrity-check"));
        assertTrue(Files.isDirectory(safetyRoot));
        assertTrue(safetyBackups().stream().allMatch(path -> path.startsWith(safetyRoot)));
        assertTrue(Files.readString(restoreInputCapture).contains("SET FOREIGN_KEY_CHECKS=1;"));
    }

    private void writeBashClients() throws Exception {
        writeExecutable(fakeBin.resolve("mysqldump"), """
                #!/usr/bin/env bash
                printf 'mysqldump %s\\n' "$*" >> "$MOCK_LOG"
                if [[ "${MOCK_SAFETY_FAIL:-}" == "1" ]]; then
                  echo 'access denied' >&2
                  exit 9
                fi
                echo '-- safety backup'
                """);
        writeExecutable(fakeBin.resolve("mysql"), """
                #!/usr/bin/env bash
                printf '%s\\n' "$*" >> "$MOCK_LOG"
                if [[ "$*" != *" -e "* ]]; then
                  if [[ "${MOCK_RESTORE_FAIL:-}" == "1" ]]; then
                    echo 'syntax error in SQL backup' >&2
                    exit 7
                  fi
                  cat > "$MOCK_RESTORE_INPUT"
                  exit 0
                fi
                if [[ "$*" == *"LEFT JOIN"* ]]; then
                  echo "${MOCK_INTEGRITY_FAIL:-0}"
                  exit 0
                fi
                if [[ "$*" == *"information_schema"* || "$*" == *"FOREIGN_KEY_CHECKS"* ]]; then
                  echo 1
                else
                  echo 0
                fi
                """);
    }

    private void writePowerShellClients() throws IOException {
        Files.writeString(fakeBin.resolve("mysqldump.cmd"), """
                @echo off
                echo mysqldump %*>>"%MOCK_LOG%"
                if "%MOCK_SAFETY_FAIL%"=="1" (
                  echo access denied 1>&2
                  exit /b 9
                )
                echo -- safety backup
                exit /b 0
                """);
        Files.writeString(fakeBin.resolve("mysql.cmd"), """
                @echo off
                :findQuery
                if "%~1"=="" goto restore
                if /I "%~1"=="-e" goto query
                shift
                goto findQuery
                :restore
                findstr "^" > "%MOCK_RESTORE_INPUT%"
                if "%MOCK_RESTORE_FAIL%"=="1" (
                  echo syntax error in SQL backup 1>&2
                  exit /b 7
                )
                exit /b 0
                :query
                echo %~2 | findstr /C:"LEFT JOIN" >nul
                if not errorlevel 1 (
                  echo integrity-check>>"%MOCK_LOG%"
                  if "%MOCK_INTEGRITY_FAIL%"=="1" (
                    echo 1
                  ) else (
                    echo 0
                  )
                  exit /b 0
                )
                echo core-check>>"%MOCK_LOG%"
                echo %~2 | findstr /C:"information_schema" >nul
                if not errorlevel 1 (
                  echo 1
                  exit /b 0
                )
                echo %* | findstr /C:"FOREIGN_KEY_CHECKS" >nul
                if not errorlevel 1 (
                  echo 1
                  exit /b 0
                )
                echo 0
                exit /b 0
                """);
    }

    private ProcessResult runBash(Map<String, String> environment, String input) throws Exception {
        List<String> command = List.of("wsl.exe", "-d", "Ubuntu-22.04", "--", "env",
                "PATH=" + environment.get("PATH"),
                "MOCK_LOG=" + toWslPath(invocationLog),
                "MOCK_SAFETY_FAIL=" + environment.getOrDefault("MOCK_SAFETY_FAIL", ""),
                "MOCK_RESTORE_FAIL=" + environment.getOrDefault("MOCK_RESTORE_FAIL", ""),
                "MOCK_INTEGRITY_FAIL=" + environment.getOrDefault("MOCK_INTEGRITY_FAIL", ""),
                "MOCK_RESTORE_INPUT=" + toWslPath(restoreInputCapture),
                "BLOG_RESTORE_SAFETY_DIR=" + bashSafetyRoot,
                "/bin/bash", toWslPath(BASH_SCRIPT), toWslPath(backupFile));
        return run(command, input, Map.of());
    }

    private ProcessResult runPowerShell(Map<String, String> environment, String input) throws Exception {
        String command = "$env:Path = '" + escapePowerShell(environment.get("Path")) + "'; "
                + "$env:MOCK_LOG = '" + escapePowerShell(environment.getOrDefault("MOCK_LOG", "")) + "'; "
                + "$env:MOCK_SAFETY_FAIL = '" + escapePowerShell(environment.getOrDefault("MOCK_SAFETY_FAIL", "")) + "'; "
                + "$env:MOCK_RESTORE_FAIL = '" + escapePowerShell(environment.getOrDefault("MOCK_RESTORE_FAIL", "")) + "'; "
                + "$env:MOCK_INTEGRITY_FAIL = '" + escapePowerShell(environment.getOrDefault("MOCK_INTEGRITY_FAIL", "")) + "'; "
                + "$env:MOCK_RESTORE_INPUT = '" + escapePowerShell(restoreInputCapture.toString()) + "'; "
                + "$env:BLOG_RESTORE_SAFETY_DIR = '" + escapePowerShell(safetyRoot.toString()) + "'; "
                + "& '" + escapePowerShell(POWERSHELL_SCRIPT.toString()) + "' -BackupPath '"
                + escapePowerShell(backupFile.toString()) + "'";
        return run(List.of("pwsh.exe", "-NoProfile", "-Command", command), input, Map.of());
    }

    private ProcessResult run(List<String> command, String input, Map<String, String> environment) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        builder.environment().putAll(environment);
        Process process = builder.start();
        process.getOutputStream().write(input.getBytes(StandardCharsets.UTF_8));
        process.getOutputStream().close();
        assertTrue(process.waitFor(15, TimeUnit.SECONDS), () -> "Timed out: " + command);
        return new ProcessResult(process.exitValue(), new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private Map<String, String> fakeEnvironment() {
        return Map.of("PATH", toWslPath(fakeBin) + ":/usr/bin:/bin");
    }

    private Map<String, String> withFlag(String name, String value) {
        return Map.of("PATH", toWslPath(fakeBin) + ":/usr/bin:/bin", name, value);
    }

    private Map<String, String> powerShellEnvironment() {
        return Map.of("Path", fakeBin + ";" + System.getenv("Path"), "MOCK_LOG", invocationLog.toString());
    }

    private Map<String, String> withPowerShellFlag(String name, String value) {
        return Map.of("Path", fakeBin + ";" + System.getenv("Path"), "MOCK_LOG", invocationLog.toString(), name, value);
    }

    private List<Path> safetyBackups() throws IOException {
        if (!Files.isDirectory(safetyRoot)) {
            return List.of();
        }
        try (var paths = Files.list(safetyRoot)) {
            return paths.filter(path -> path.getFileName().toString().startsWith("safety-blog-")
                    && path.getFileName().toString().endsWith(".sql")).toList();
        }
    }

    private List<String> bashSafetyBackups() throws Exception {
        ProcessResult result = run(List.of("wsl.exe", "-d", "Ubuntu-22.04", "--", "find", bashSafetyRoot,
                "-maxdepth", "1", "-type", "f", "-name", "*.sql"), "", Map.of());
        return result.exitCode() == 0 ? result.output().lines().filter(line -> !line.isBlank()).toList() : List.of();
    }

    private void assertNoSafetyBackup() throws IOException {
        assertTrue(safetyBackups().isEmpty(), "Confirmation rejection must not create a safety backup");
    }

    private void assertBashPrivateSafetyPermissions() throws Exception {
        ProcessResult directoryMode = run(List.of("wsl.exe", "-d", "Ubuntu-22.04", "--", "stat", "-c", "%a",
                bashSafetyRoot), "", Map.of());
        assertEquals("700", directoryMode.output().trim());
        for (String file : bashSafetyBackups()) {
            ProcessResult fileMode = run(List.of("wsl.exe", "-d", "Ubuntu-22.04", "--", "stat", "-c", "%a",
                    file), "", Map.of());
            assertEquals("600", fileMode.output().trim());
        }
    }

    private void writeExecutable(Path path, String content) throws Exception {
        Files.writeString(path, content);
        ProcessResult chmod = run(List.of("wsl.exe", "-d", "Ubuntu-22.04", "--", "chmod", "+x", toWslPath(path)),
                "", Map.of());
        assertEquals(0, chmod.exitCode(), chmod.output());
    }

    private void assumeWslAvailable() throws Exception {
        ProcessResult result = run(List.of("wsl.exe", "-d", "Ubuntu-22.04", "--", "/bin/true"), "", Map.of());
        Assumptions.assumeTrue(result.exitCode() == 0, "WSL Ubuntu-22.04 is unavailable");
    }

    private void assumePowerShellAvailable() {
        Assumptions.assumeTrue(System.getProperty("os.name").toLowerCase().contains("win"),
                "PowerShell behavior tests require Windows");
    }

    private String toWslPath(Path path) {
        String absolute = path.toAbsolutePath().toString().replace('\\', '/');
        return "/mnt/" + Character.toLowerCase(absolute.charAt(0)) + absolute.substring(2);
    }

    private String escapePowerShell(String value) {
        return value.replace("'", "''");
    }

    private record ProcessResult(int exitCode, String output) {
    }
}
