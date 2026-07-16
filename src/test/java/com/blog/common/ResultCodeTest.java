package com.blog.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultCodeTest {

    @Test
    void valueOf_shouldReturnExactCode() {
        assertThat(ResultCode.valueOf(200)).isSameAs(ResultCode.SUCCESS);
        assertThat(ResultCode.valueOf(404)).isSameAs(ResultCode.NOT_FOUND);
        assertThat(ResultCode.valueOf(1001)).isSameAs(ResultCode.USER_NOT_FOUND);
    }

    @Test
    void valueOf_unknown_shouldReturnSystemError() {
        assertThat(ResultCode.valueOf(9999)).isSameAs(ResultCode.SYSTEM_ERROR);
        assertThat(ResultCode.valueOf(10000)).isSameAs(ResultCode.SYSTEM_ERROR);
    }

    @Test
    void codes_shouldBeUnique() {
        java.util.Set<Integer> codes = new java.util.HashSet<>();
        for (ResultCode code : ResultCode.values()) {
            assertThat(codes.add(code.getCode()))
                    .as("code %d should be unique", code.getCode())
                    .isTrue();
        }
    }

    @Test
    void messages_shouldNotBeBlank() {
        for (ResultCode code : ResultCode.values()) {
            assertThat(code.getMessage())
                    .as("%s message should not be blank", code.name())
                    .isNotBlank();
        }
    }
}
