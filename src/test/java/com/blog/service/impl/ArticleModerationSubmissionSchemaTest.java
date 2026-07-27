package com.blog.service.impl;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArticleModerationSubmissionSchemaTest {
    @Test
    void manualReviewKeepsActiveArticleLockUntilAdministratorMakesTerminalDecision() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:moderation-lock;MODE=MySQL;DB_CLOSE_DELAY=-1");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS article_moderation_submissions");
            statement.execute("CREATE TABLE article_moderation_submissions (id BIGINT AUTO_INCREMENT PRIMARY KEY, active_article_id BIGINT, status VARCHAR(32), CONSTRAINT active_one UNIQUE(active_article_id))");
            statement.execute("INSERT INTO article_moderation_submissions(active_article_id, status) VALUES (7, 'PROCESSING')");
            statement.execute("UPDATE article_moderation_submissions SET status = 'MANUAL_REVIEW' WHERE active_article_id = 7");

            assertThatThrownBy(() -> statement.execute("INSERT INTO article_moderation_submissions(active_article_id, status) VALUES (7, 'PENDING')"))
                    .isInstanceOf(SQLException.class);
        }
    }
}
