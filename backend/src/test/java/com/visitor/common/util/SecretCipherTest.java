package com.visitor.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretCipherTest {

    private final SecretCipher cipher = new SecretCipher("unit-test-master-key");

    @Test
    void encryptThenDecrypt() {
        String encrypted = cipher.encrypt("s3cret-value");

        assertThat(encrypted).startsWith("enc:v1:");
        assertThat(encrypted).doesNotContain("s3cret-value");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("s3cret-value");
    }

    @Test
    void encryptIsIdempotentForCipherText() {
        String encrypted = cipher.encrypt("abc");
        assertThat(cipher.encrypt(encrypted)).isEqualTo(encrypted);
    }

    @Test
    void legacyPlainTextPassesThrough() {
        assertThat(cipher.decrypt("plain-text")).isEqualTo("plain-text");
        assertThat(cipher.decrypt(null)).isNull();
    }

    @Test
    void decryptWithWrongKeyFails() {
        String encrypted = cipher.encrypt("abc");
        SecretCipher other = new SecretCipher("another-key");
        assertThatThrownBy(() -> other.decrypt(encrypted)).hasMessageContaining("解密失败");
    }
}
