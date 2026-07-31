package com.blog.mapper;

import com.blog.entity.WebsiteAccessLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dao-test")
@DisplayName("WebsiteAccessLogMapper DAO 直测")
class WebsiteAccessLogMapperDaoTest {

    @Autowired
    private WebsiteAccessLogMapper websiteAccessLogMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM website_access_log WHERE request_url LIKE '%dao-test%'");
    }

    @Test
    @DisplayName("插入日志后查询 PV/UV 与设备统计")
    void accessLogStats_shouldReturnInsertedRows() {
        String today = LocalDateTime.now().toString().substring(0, 10);
        jdbcTemplate.execute(
                "INSERT INTO website_access_log (access_date, access_time, ip_address, request_url, page_url, response_status, device_type, browser, operating_system) " +
                        "VALUES ('" + today + "', NOW(), '127.0.0.1', '/dao-test', '/dao-test', 200, 'mobile', 'chrome', 'windows')"
        );

        assertThat(websiteAccessLogMapper.countPvByDate(today)).isGreaterThan(0);
        assertThat(websiteAccessLogMapper.countUvByDate(today)).isGreaterThan(0);
        assertThat(websiteAccessLogMapper.countTodayPv()).isGreaterThan(0);
        assertThat(websiteAccessLogMapper.countTodayUv()).isGreaterThan(0);

        List<Map<String, Object>> devices = websiteAccessLogMapper.countByDeviceType();
        assertThat(devices).anyMatch(map -> "mobile".equals(map.get("device_type")));
    }
}
