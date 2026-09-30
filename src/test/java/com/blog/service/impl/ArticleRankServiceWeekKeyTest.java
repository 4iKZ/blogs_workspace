package com.blog.service.impl;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleRankServiceWeekKeyTest {

    @Test
    void getWeekKey_crossYearWeek_shouldUseIsoWeekBasedYear() throws Exception {
        ArticleRankServiceImpl service = new ArticleRankServiceImpl();
        Method getWeekKey = ArticleRankServiceImpl.class.getDeclaredMethod("getWeekKey", LocalDate.class);
        getWeekKey.setAccessible(true);

        // 2021-12-31（周五）与 2022-01-01（周六）同属 ISO 2021-W52
        assertThat(getWeekKey.invoke(service, LocalDate.of(2021, 12, 31)))
                .isEqualTo("hot:articles:zset:week:2021-W52");
        assertThat(getWeekKey.invoke(service, LocalDate.of(2022, 1, 1)))
                .isEqualTo("hot:articles:zset:week:2021-W52");
        // 2022-01-03（周一）是 ISO 2022-W01 的第一天
        assertThat(getWeekKey.invoke(service, LocalDate.of(2022, 1, 3)))
                .isEqualTo("hot:articles:zset:week:2022-W01");
    }
}
