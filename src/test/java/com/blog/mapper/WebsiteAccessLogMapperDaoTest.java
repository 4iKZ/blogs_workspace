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

    @Test
    @DisplayName("批量插入访问日志")
    void insertBatch_shouldPersistAllRows() {
        List<WebsiteAccessLog> logs = List.of(
                buildLog("/dao-test-batch-1", "chrome", "windows", "mobile"),
                buildLog("/dao-test-batch-2", "firefox", "linux", "desktop")
        );

        int inserted = websiteAccessLogMapper.insertBatch(logs);
        assertThat(inserted).isEqualTo(2);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM website_access_log WHERE request_url IN ('/dao-test-batch-1', '/dao-test-batch-2')",
                Integer.class);
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("按时间范围查询日志与去重页面数")
    void selectByDateRange_andCountDistinctPages() {
        insertLogRow("/dao-test-range", "/dao-test/page-a", "http://google.com", "chrome", "windows");
        insertLogRow("/dao-test-range-2", "/dao-test/page-b", "http://google.com", "chrome", "windows");

        LocalDateTime start = LocalDateTime.now().minusMinutes(5);
        LocalDateTime end = LocalDateTime.now().plusMinutes(5);

        List<WebsiteAccessLog> logs = websiteAccessLogMapper.selectByDateRange(start, end);
        assertThat(logs).extracting(WebsiteAccessLog::getRequestUrl)
                .contains("/dao-test-range", "/dao-test-range-2");

        assertThat(websiteAccessLogMapper.countDistinctPages(start, end)).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("热门页面与来源统计")
    void selectTopPages_andTrafficSources() {
        insertLogRow("/dao-test/top", "/dao-test/top", "http://google.com/search", "chrome", "windows");
        insertLogRow("/dao-test/top-2", "/dao-test/top-2", "http://google.com/search", "chrome", "windows");

        List<Map<String, Object>> topPages = websiteAccessLogMapper.selectTopPages(10);
        assertThat(topPages).anyMatch(map -> "/dao-test/top".equals(map.get("page_url")));

        LocalDateTime start = LocalDateTime.now().minusMinutes(5);
        LocalDateTime end = LocalDateTime.now().plusMinutes(5);
        List<Map<String, Object>> sources = websiteAccessLogMapper.selectTrafficSources(start, end);
        assertThat(sources).anyMatch(map -> "http://google.com/search".equals(map.get("referer")));
    }

    @Test
    @DisplayName("按日期字符串范围统计来源与页面（分页）")
    void selectTrafficAndTopPagesByDateRange() {
        String today = LocalDateTime.now().toString().substring(0, 10);
        insertLogRow("/dao-test/dr", "/dao-test/dr", "http://bing.com", "chrome", "windows");

        List<Map<String, Object>> sources = websiteAccessLogMapper.selectTrafficSourcesByDateRange(today, today, 10);
        assertThat(sources).anyMatch(map -> "http://bing.com".equals(map.get("source_name")));

        List<Map<String, Object>> pages = websiteAccessLogMapper.selectTopPagesByDateRange(today, today, 0, 10);
        assertThat(pages).anyMatch(map -> "/dao-test/dr".equals(map.get("page_url")));

        assertThat(websiteAccessLogMapper.countDistinctPageUrls(today, today)).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("浏览器/操作系统统计（今日）")
    void browserAndOsStats() {
        String today = LocalDateTime.now().toString().substring(0, 10);
        jdbcTemplate.execute(
                "INSERT INTO website_access_log (access_date, access_time, ip_address, request_url, browser, operating_system) " +
                        "VALUES ('" + today + "', NOW(), '10.0.0.1', '/dao-test-browser-1', 'daotestbrowser', 'daotestos')"
        );
        jdbcTemplate.execute(
                "INSERT INTO website_access_log (access_date, access_time, ip_address, request_url, browser, operating_system) " +
                        "VALUES ('" + today + "', NOW(), '10.0.0.2', '/dao-test-browser-2', 'daotestbrowser', 'daotestos')"
        );

        List<Map<String, Object>> browsers = websiteAccessLogMapper.countByBrowser();
        assertThat(browsers).anyMatch(map -> "daotestbrowser".equals(map.get("browser")));

        List<Map<String, Object>> oss = websiteAccessLogMapper.countByOperatingSystem();
        assertThat(oss).anyMatch(map -> "daotestos".equals(map.get("operating_system")));
    }

    @Test
    @DisplayName("删除指定时间点之前的旧日志")
    void deleteBeforeDate_shouldRemoveOldRows() {
        insertLogRow("/dao-test-delete", "/dao-test-delete", null, "chrome", "windows");
        jdbcTemplate.execute(
                "UPDATE website_access_log SET access_time = DATE_SUB(NOW(), INTERVAL 2 DAY) " +
                        "WHERE request_url = '/dao-test-delete'"
        );

        int deleted = websiteAccessLogMapper.deleteBeforeDate(LocalDateTime.now().minusDays(1));
        assertThat(deleted).isGreaterThanOrEqualTo(1);

        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM website_access_log WHERE request_url = '/dao-test-delete'", Integer.class);
        assertThat(remaining).isZero();
    }

    private WebsiteAccessLog buildLog(String requestUrl, String browser, String os, String deviceType) {
        WebsiteAccessLog log = new WebsiteAccessLog();
        log.setAccessDate(LocalDateTime.now().toString().substring(0, 10));
        log.setAccessTime(LocalDateTime.now());
        log.setIpAddress("10.1.1.1");
        log.setRequestUrl(requestUrl);
        log.setPageUrl(requestUrl);
        log.setResponseStatus(200);
        log.setDeviceType(deviceType);
        log.setBrowser(browser);
        log.setOperatingSystem(os);
        return log;
    }

    private void insertLogRow(String requestUrl, String pageUrl, String referer, String browser, String os) {
        String today = LocalDateTime.now().toString().substring(0, 10);
        String refererSql = referer == null ? "NULL" : "'" + referer + "'";
        jdbcTemplate.execute(
                "INSERT INTO website_access_log (access_date, access_time, ip_address, request_url, page_url, referer, browser, operating_system) " +
                        "VALUES ('" + today + "', NOW(), '10.1.1.2', '" + requestUrl + "', '" + pageUrl + "', " + refererSql + ", '" + browser + "', '" + os + "')"
        );
    }
}
