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

import static org.assertj.core.api.Assertions.assertThat;

class DepositWithdrawRequestValidationTest {

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
        DepositWithdrawRequest request = DepositWithdrawRequest.builder()
            .amount(new BigDecimal("100.50"))
            .sourceWalletEmail("source@example.com")
            .destinationWalletEmail("destination@example.com")
            .build();

        Set<ConstraintViolation<DepositWithdrawRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void nonPositiveAmountFailsValidation() {
        DepositWithdrawRequest request = DepositWithdrawRequest.builder()
            .amount(new BigDecimal("0"))
            .sourceWalletEmail("source@example.com")
            .destinationWalletEmail("destination@example.com")
            .build();

        Set<ConstraintViolation<DepositWithdrawRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "amount".equals(v.getPropertyPath().toString()));
    }

    @Test
    void sameWalletFailsValidation() {
        String walletEmail = "same@example.com";
        DepositWithdrawRequest request = DepositWithdrawRequest.builder()
            .amount(new BigDecimal("10.00"))
            .sourceWalletEmail(walletEmail)
            .destinationWalletEmail(walletEmail)
            .build();

        Set<ConstraintViolation<DepositWithdrawRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "walletCombinationValid".equals(v.getPropertyPath().toString()));
    }
}
