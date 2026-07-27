package com.blog.ops;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflineDatabaseRestoreScriptContractTest {

    @Test
    void offlineRestoreScripts_shouldRequireSafetyPreflightAndPostRestoreChecks() throws Exception {
        assertRestoreContract(Path.of("scripts", "restore-backup.sh"));
        assertRestoreContract(Path.of("scripts", "restore-backup.ps1"));
    }

    private void assertRestoreContract(Path script) throws Exception {
        assertTrue(Files.isRegularFile(script), () -> "Missing offline restore script: " + script);
        String content = Files.readString(script);
        assertTrue(content.contains("mysqldump"), "A safety backup must use mysqldump");
        assertTrue(content.contains("mysql"), "Restore and verification must use mysql");
        assertTrue(content.contains("MAINTENANCE"), "Maintenance-mode confirmation is required");
        assertTrue(content.contains("FOREIGN_KEY_CHECKS"), "Foreign-key checks must be restored and verified");
        assertTrue(content.contains("users") && content.contains("articles") && content.contains("comments"),
                "Core tables must be verified after restore");
    }
}
