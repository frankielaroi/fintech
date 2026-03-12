package com.frankie.fintech.controller;

import com.frankie.fintech.dto.transaction.DepositWithdrawRequest;
import com.frankie.fintech.dto.transaction.SendMoneyRequest;
import com.frankie.fintech.dto.transaction.WalletResponse;
import com.frankie.fintech.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Wallet deposit, withdrawal, and transfer endpoints")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/balance")
    @Operation(summary = "Get wallet balance for the authenticated user")
    public ResponseEntity<WalletResponse> getBalance(
            @AuthenticationPrincipal UserDetails principal) {
        requireAuthenticated(principal);
        log.info("Balance query: user={}", principal.getUsername());
        return ResponseEntity.ok(transactionService.getBalance(principal.getUsername()));
    }

    @PostMapping("/deposit")
    @Operation(summary = "Deposit funds into a wallet")
    public ResponseEntity<WalletResponse> deposit(
            @Valid @RequestBody DepositWithdrawRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        requireAuthenticated(principal);
        log.info("Deposit request: user={}, amount={}", principal.getUsername(), request.getAmount());
        return ResponseEntity.ok(transactionService.deposit(request));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw funds from a wallet")
    public ResponseEntity<WalletResponse> withdraw(
            @Valid @RequestBody DepositWithdrawRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        requireAuthenticated(principal);
        log.info("Withdrawal request: user={}, amount={}", principal.getUsername(), request.getAmount());
        return ResponseEntity.ok(transactionService.withdraw(request));
    }

    @PostMapping("/send")
    @Operation(summary = "Send money from one wallet to another")
    public ResponseEntity<WalletResponse> sendMoney(
            @Valid @RequestBody SendMoneyRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        requireAuthenticated(principal);
        log.info("Transfer request: user={}, amount={}, from={}, to={}",
                principal.getUsername(), request.getAmount(),
                request.getSourceWallet(), request.getDestinationWallet());
        return ResponseEntity.ok(transactionService.sendMoney(request, principal.getUsername()));
    }

    private void requireAuthenticated(UserDetails principal) {
        if (principal == null || principal.getUsername().isBlank()) {
            throw new IllegalArgumentException("Authentication required");
        }
    }
}


