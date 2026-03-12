package com.frankie.fintech.dto.transaction;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SendMoneyRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setup() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void validRequestPassesValidation() {
        SendMoneyRequest request = SendMoneyRequest.builder()
            .amount(new BigDecimal("50.00"))
            .sourceWallet(UUID.randomUUID())
            .destinationWallet(UUID.randomUUID())
            .build();

        Set<ConstraintViolation<SendMoneyRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void amountWithTooManyDecimalsFailsValidation() {
        SendMoneyRequest request = SendMoneyRequest.builder()
            .amount(new BigDecimal("10.123"))
            .sourceWallet(UUID.randomUUID())
            .destinationWallet(UUID.randomUUID())
            .build();

        Set<ConstraintViolation<SendMoneyRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "amount".equals(v.getPropertyPath().toString()));
    }

    @Test
    void sameWalletFailsValidation() {
        UUID walletId = UUID.randomUUID();
        SendMoneyRequest request = SendMoneyRequest.builder()
            .amount(new BigDecimal("10.00"))
            .sourceWallet(walletId)
            .destinationWallet(walletId)
            .build();

        Set<ConstraintViolation<SendMoneyRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "walletCombinationValid".equals(v.getPropertyPath().toString()));
    }
}

