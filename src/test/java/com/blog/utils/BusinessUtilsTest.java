package com.blog.utils;

import com.blog.common.ResultCode;
import com.blog.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("deprecation")
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
    void checkExist_null_shouldThrowBusinessExceptionWithNotFoundCode() {
        assertThatThrownBy(() -> BusinessUtils.checkExist(null, "should not be null"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("should not be null")
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResultCode.NOT_FOUND.getCode()));
    }

    @Test
    void checkExist_nonNull_shouldReturnObject() {
        String obj = "existing";
        assertThat(BusinessUtils.checkExist(obj, "error")).isSameAs(obj);
    }

    @Test
    void checkIdExist_missing_shouldThrowWithDomainCode() {
        assertThatThrownBy(() -> BusinessUtils.checkIdExist(1L, id -> null, ResultCode.ARTICLE_NOT_FOUND, "文章不存在"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文章不存在")
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResultCode.ARTICLE_NOT_FOUND.getCode()));
    }

    @Test
    void checkIdExist_invalidId_shouldThrowBadRequest() {
        assertThatThrownBy(() -> BusinessUtils.checkIdExist(null, id -> null, "文章不存在"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResultCode.BAD_REQUEST.getCode()));
    }

    @Test
    void checkIdExist_existing_shouldReturnObject() {
        String obj = "existing";
        String result = BusinessUtils.checkIdExist(1L, id -> obj, "不存在");
        assertThat(result).isSameAs(obj);
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
