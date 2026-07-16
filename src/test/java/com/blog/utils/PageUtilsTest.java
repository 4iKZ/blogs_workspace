package com.blog.utils;

import com.blog.dto.PageDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageUtilsTest {

    @Test
    void getValidPage_null_shouldReturnDefault() {
        assertThat(PageUtils.getValidPage(null)).isEqualTo(1);
    }

    @Test
    void getValidPage_zero_shouldReturnDefault() {
        assertThat(PageUtils.getValidPage(0)).isEqualTo(1);
        assertThat(PageUtils.getValidPage(-5)).isEqualTo(1);
    }

    @Test
    void getValidPage_positive_shouldReturnAsIs() {
        assertThat(PageUtils.getValidPage(3)).isEqualTo(3);
    }

    @Test
    void getValidSize_null_shouldReturnDefault() {
        assertThat(PageUtils.getValidSize(null)).isEqualTo(10);
    }

    @Test
    void getValidSize_zero_shouldReturnDefault() {
        assertThat(PageUtils.getValidSize(0)).isEqualTo(10);
    }

    @Test
    void calculateOffset_shouldComputeCorrectly() {
        assertThat(PageUtils.calculateOffset(1, 10)).isEqualTo(0);
        assertThat(PageUtils.calculateOffset(2, 10)).isEqualTo(10);
        assertThat(PageUtils.calculateOffset(3, 20)).isEqualTo(40);
    }

    @Test
    void convertList_null_shouldReturnEmpty() {
        List<String> result = PageUtils.<String, String>convertList(null, s -> s.toUpperCase());
        assertThat(result).isEmpty();
    }

    @Test
    void convertList_empty_shouldReturnEmpty() {
        List<String> result = PageUtils.<String, String>convertList(java.util.Collections.emptyList(), s -> s.toUpperCase());
        assertThat(result).isEmpty();
    }

    @Test
    void convertList_nonEmpty_shouldConvert() {
        List<String> result = PageUtils.<String, String>convertList(java.util.List.of("a", "b"), s -> s.toUpperCase());
        assertThat(result).containsExactly("A", "B");
    }

    @Test
    void convertPageResult_shouldMapFields() {
        com.baomidou.mybatisplus.core.metadata.IPage<String> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>();
        page.setTotal(100);
        page.setCurrent(2);
        page.setSize(10);
        page.setRecords(List.of("x", "y"));

        PageDTO<Integer> dto = PageUtils.convertPageResult(page, s -> s.length());

        assertThat(dto.getTotal()).isEqualTo(100);
        assertThat(dto.getCurrent()).isEqualTo(2);
        assertThat(dto.getSize()).isEqualTo(10);
        assertThat(dto.getRecords()).containsExactly(1, 1);
    }
}
