package com.servicehub.validation;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class StrongPasswordValidatorTest {
    private final StrongPasswordValidator validator = new StrongPasswordValidator();
    @Test
    void countsCharactersAndLimitsUtf8BytesSeparately() {
        assertThat(validator.isValid("A1ééé", null)).isFalse();
        assertThat(validator.isValid("A1éééééé", null)).isTrue();
        assertThat(validator.isValid("A1" + "é".repeat(36), null)).isFalse();
        assertThat(validator.isValid("ServiceHub123!", null)).isTrue();
    }
}
