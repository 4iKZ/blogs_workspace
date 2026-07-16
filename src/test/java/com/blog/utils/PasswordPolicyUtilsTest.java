package com.blog.utils;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyUtilsTest {

    @Test
    void validatePassword_null_shouldReturnFalse() {
        assertThat(PasswordPolicyUtils.validatePassword(null)).isFalse();
    }

    @Test
    void validatePassword_empty_shouldReturnFalse() {
        assertThat(PasswordPolicyUtils.validatePassword("")).isFalse();
    }

    @Test
    void validatePassword_tooShort_shouldReturnFalse() {
        assertThat(PasswordPolicyUtils.validatePassword("Ab1!")).isFalse();
    }

    @Test
    void validatePassword_tooLong_shouldReturnFalse() {
        String longPwd = "Ab1!" + "a".repeat(17);
        assertThat(PasswordPolicyUtils.validatePassword(longPwd)).isFalse();
    }

    @Test
    void validatePassword_missingUppercase_shouldReturnFalse() {
        assertThat(PasswordPolicyUtils.validatePassword("abcd1234!")).isFalse();
    }

    @Test
    void validatePassword_missingLowercase_shouldReturnFalse() {
        assertThat(PasswordPolicyUtils.validatePassword("ABCD1234!")).isFalse();
    }

    @Test
    void validatePassword_missingDigit_shouldReturnFalse() {
        assertThat(PasswordPolicyUtils.validatePassword("AbcdEFGH!")).isFalse();
    }

    @Test
    void validatePassword_missingSpecial_shouldReturnFalse() {
        assertThat(PasswordPolicyUtils.validatePassword("Abcd1234")).isFalse();
    }

    @Test
    void validatePassword_valid_shouldReturnTrue() {
        assertThat(PasswordPolicyUtils.validatePassword("Abcd1234!")).isTrue();
    }

    @Test
    void getPasswordPolicy_shouldReturnNonEmptyDescription() {
        String policy = PasswordPolicyUtils.getPasswordPolicy();
        assertThat(policy).isNotNull();
        assertThat(policy).contains("8-20");
        assertThat(policy).contains("大写");
        assertThat(policy).contains("小写");
        assertThat(policy).contains("数字");
        assertThat(policy).contains("特殊字符");
    }
}
