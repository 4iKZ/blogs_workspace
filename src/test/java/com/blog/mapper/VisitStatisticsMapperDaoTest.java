package com.blog.mapper;

import com.blog.entity.VisitStatistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dao-test")
@DisplayName("VisitStatisticsMapper DAO 直测")
class VisitStatisticsMapperDaoTest {

    @Autowired
    private VisitStatisticsMapper visitStatisticsMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM visit_statistics WHERE `date` = '2099-12-31'");
    }

    @Test
    @DisplayName("插入统计后查询与汇总")
    void visitStatistics_shouldPersistAndReturnRows() {
        jdbcTemplate.execute(
                "INSERT INTO visit_statistics (`date`, total_visits, unique_visitors, page_views, new_users, new_articles, new_comments) " +
                        "VALUES ('2099-12-31', 10, 5, 20, 1, 0, 0)"
        );

        VisitStatistics stats = visitStatisticsMapper.selectByDate("2099-12-31");
        assertThat(stats).isNotNull();
        assertThat(stats.getPageViews()).isEqualTo(20);

        assertThat(visitStatisticsMapper.sumTotalPageViews()).isGreaterThan(0);
        assertThat(visitStatisticsMapper.sumLast7DaysPageViews()).isGreaterThanOrEqualTo(0);
        assertThat(visitStatisticsMapper.selectRecentDays(1)).extracting(VisitStatistics::getDate).contains(LocalDate.parse("2099-12-31"));
    }
}
