package com.blog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

public class UserVerifyTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_LIVE_DATABASE_TESTS", matches = "true")
    public void checkUser() {
        System.out.println("=== VERIFYING USER EXISTENCE ===");
        String userQuery = "SELECT count(*) FROM users WHERE username = 'SiYuanHao'";
        try (Connection conn = DriverManager.getConnection(
                requireEnvironment("LIVE_DATABASE_URL"),
                requireEnvironment("LIVE_DATABASE_USERNAME"),
                requireEnvironment("LIVE_DATABASE_PASSWORD"));
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(userQuery)) {
            
            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("Query: " + userQuery);
                System.out.println("Result Count: " + count);
                if (count > 0) {
                     System.out.println(">>> USER 'SiYuanHao' EXISTS in Database!");
                } else {
                     System.err.println(">>> USER 'SiYuanHao' DOES NOT EXIST in Database!");
                     throw new RuntimeException("User 'SiYuanHao' NOT FOUND in database!");
                }
            } else {
                throw new RuntimeException("Query returned no rows!");
            }
        } catch (Exception e) {
            System.err.println("Failed to query user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String requireEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少环境变量: " + name);
        }
        return value;
    }
}
