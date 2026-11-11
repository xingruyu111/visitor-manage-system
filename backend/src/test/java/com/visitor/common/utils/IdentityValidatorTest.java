package com.visitor.common.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityValidatorTest {

    @Test
    void phoneValidation() {
        assertThat(IdentityValidator.isPhone("13800000000")).isTrue();
        assertThat(IdentityValidator.isPhone("13912345678")).isTrue();
        assertThat(IdentityValidator.isPhone("12345678901")).isFalse();
        assertThat(IdentityValidator.isPhone("1380000000")).isFalse();
        assertThat(IdentityValidator.isPhone("1380000000a")).isFalse();
        assertThat(IdentityValidator.isPhone(null)).isFalse();
    }

    @Test
    void idCardChecksumValidation() {
        assertThat(IdentityValidator.isIdCard("11010519491231002X")).isTrue();
        assertThat(IdentityValidator.isIdCard("440524188001010014")).isTrue();
        assertThat(IdentityValidator.isIdCard("110105194912310021")).isFalse();
        assertThat(IdentityValidator.isIdCard("11010519491231002")).isFalse();
        assertThat(IdentityValidator.isIdCard("11010519491231002A")).isFalse();
    }

    @Test
    void idCardBirthDateValidation() {
        assertThat(IdentityValidator.isIdCard("110101199002300023")).isFalse();
        assertThat(IdentityValidator.isIdCard("110101299003071230")).isFalse();
    }
}
