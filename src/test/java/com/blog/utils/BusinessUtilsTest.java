package com.blog.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessUtilsTest {

    @Test
    void isValidId_null_shouldReturnFalse() {
        assertThat(BusinessUtils.isValidId(null)).isFalse();
    }

    @Test
    void isValidId_zero_shouldReturnFalse() {
        assertThat(BusinessUtils.isValidId(0L)).isFalse();
    }

    @Test
    void isValidId_negative_shouldReturnFalse() {
        assertThat(BusinessUtils.isValidId(-1L)).isFalse();
    }

    @Test
    void isValidId_positive_shouldReturnTrue() {
        assertThat(BusinessUtils.isValidId(1L)).isTrue();
    }

    @Test
    void checkExist_null_shouldThrow() {
        assertThatThrownBy(() -> BusinessUtils.checkExist(null, "should not be null"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("should not be null");
    }

    @Test
    void checkExist_nonNull_shouldReturnObject() {
        String obj = "existing";
        assertThat(BusinessUtils.checkExist(obj, "error")).isSameAs(obj);
    }

    @Test
    void isValidStatus_null_shouldReturnFalse() {
        assertThat(BusinessUtils.isValidStatus(null, 1, 2)).isFalse();
    }

    @Test
    void isValidStatus_matching_shouldReturnTrue() {
        assertThat(BusinessUtils.isValidStatus(2, 1, 2, 3)).isTrue();
    }

    @Test
    void isValidStatus_nonMatching_shouldReturnFalse() {
        assertThat(BusinessUtils.isValidStatus(5, 1, 2, 3)).isFalse();
    }

    @Test
    void success_shouldReturnSuccessResult() {
        assertThat(BusinessUtils.success("data").isSuccess()).isTrue();
    }

    @Test
    void error_shouldReturnErrorResult() {
        assertThat(BusinessUtils.error("oops").isSuccess()).isFalse();
        assertThat(BusinessUtils.error("oops").getMessage()).isEqualTo("oops");
    }
}
