package com.blog.mapper;

import com.blog.entity.SystemConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dao-test")
@DisplayName("SystemConfigMapper DAO 直测")
class SystemConfigMapperDaoTest {

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM system_config WHERE config_key LIKE 'dao-test-%'");
    }

    @Test
    @DisplayName("配置键查询、存在性检查与批量更新状态")
    void systemConfigQueries_shouldPersistAndReturnRows() {
        Long id = jdbcTemplate.queryForObject("SELECT id FROM system_config WHERE config_key = 'site_name'", Long.class);

        SystemConfig config = new SystemConfig();
        config.setConfigKey("dao-test-key");
        config.setConfigValue("dao-value");
        config.setDescription("dao description");
        config.setConfigType("string");
        config.setIsPublic(1);
        systemConfigMapper.insert(config);

        assertThat(systemConfigMapper.selectByConfigKey("dao-test-key")).isNotNull();
        assertThat(systemConfigMapper.countByConfigKeyExcludeId("dao-test-key", config.getId())).isEqualTo(0);
        assertThat(systemConfigMapper.countByConfigKeyExcludeId("dao-test-key", 99999L)).isEqualTo(1);

        int updated = systemConfigMapper.batchUpdateConfigStatus(List.of("site_name", "dao-test-key"), 0);
        assertThat(updated).isGreaterThanOrEqualTo(1);
    }
}
