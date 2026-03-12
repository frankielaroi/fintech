package com.frankie.fintech.service;

import com.frankie.fintech.dto.transaction.DepositWithdrawRequest;
import com.frankie.fintech.dto.transaction.SendMoneyRequest;
import com.frankie.fintech.dto.transaction.WalletResponse;
import com.frankie.fintech.entity.Transaction;
import com.frankie.fintech.entity.User;
import com.frankie.fintech.entity.Wallet;
import com.frankie.fintech.repository.TransactionRepository;
import com.frankie.fintech.repository.UserRepository;
import com.frankie.fintech.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    // ── helpers ────────────────────────────────────────────────────────────────

    /** Resolve the default wallet for an email using a pessimistic write lock. */
    private Wallet getLockedWalletByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        User user = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        return walletRepository.findDefaultWalletByUserIdWithLock(user.getId())
                .orElseThrow(() -> new IllegalStateException("Default wallet not found for user: " + email));
    }

    /** Resolve a wallet by ID using a pessimistic write lock. */
    private Wallet getLockedWalletById(UUID walletId) {
        return walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));
    }

    private static void assertOperational(Wallet wallet) {
        if (!wallet.isOperational()) {
            throw new IllegalStateException(
                    "Wallet " + wallet.getId() + " is not operational (status=" + wallet.getStatus()
                    + ", frozen=" + wallet.getIsFrozen() + ")");
        }
    }

    private String generateReference(String prefix) {
        return prefix + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
               + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public WalletResponse getBalance(String email) {
        Wallet wallet = getLockedWalletByEmail(email);
        return WalletResponse.from(wallet);
    }

    // ── deposit ────────────────────────────────────────────────────────────────

    @Transactional
    public WalletResponse deposit(DepositWithdrawRequest request) {
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero");
        }

        // Idempotency: skip if already processed
        String idempotencyKey = request.getSourceWalletEmail() + ":DEPOSIT:" + request.getAmount();
        if (transactionRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            log.warn("Duplicate deposit request ignored (idempotencyKey={})", idempotencyKey);
            return getBalance(request.getDestinationWalletEmail());
        }

        Wallet wallet = getLockedWalletByEmail(request.getDestinationWalletEmail());
        assertOperational(wallet);

        if (wallet.wouldExceedMaxLimit(request.getAmount())) {
            throw new IllegalStateException("Deposit would exceed wallet maximum balance limit");
        }

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.credit(request.getAmount());
        walletRepository.save(wallet);

        Transaction tx = Transaction.builder()
                .transactionReference(generateReference("DEP"))
                .idempotencyKey(idempotencyKey)
                .user(wallet.getUser())
                .destinationWallet(wallet)
                .amount(request.getAmount())
                .currency(wallet.getCurrency())
                .transactionType(Transaction.TransactionType.DEPOSIT)
                .status(Transaction.TransactionStatus.COMPLETED)
                .paymentMethod(Transaction.PaymentMethod.WALLET)
                .destinationBalanceBefore(balanceBefore)
                .destinationBalanceAfter(wallet.getBalance())
                .completedAt(LocalDateTime.now())
                .build();
        tx.calculateTotalAmount();
        transactionRepository.save(tx);

        log.info("Deposit completed: ref={}, amount={}, wallet={}", tx.getTransactionReference(),
                request.getAmount(), wallet.getId());
        return WalletResponse.from(wallet);
    }

    // ── withdraw ───────────────────────────────────────────────────────────────

    @Transactional
    public WalletResponse withdraw(DepositWithdrawRequest request) {
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero");
        }

        String idempotencyKey = request.getSourceWalletEmail() + ":WITHDRAWAL:" + request.getAmount();
        if (transactionRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            log.warn("Duplicate withdrawal request ignored (idempotencyKey={})", idempotencyKey);
            return getBalance(request.getSourceWalletEmail());
        }

        Wallet wallet = getLockedWalletByEmail(request.getSourceWalletEmail());
        assertOperational(wallet);

        if (!wallet.hasSufficientBalance(request.getAmount())) {
            throw new IllegalStateException("Insufficient available balance");
        }

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.debit(request.getAmount());
        walletRepository.save(wallet);

        Transaction tx = Transaction.builder()
                .transactionReference(generateReference("WDR"))
                .idempotencyKey(idempotencyKey)
                .user(wallet.getUser())
                .sourceWallet(wallet)
                .amount(request.getAmount())
                .currency(wallet.getCurrency())
                .transactionType(Transaction.TransactionType.WITHDRAWAL)
                .status(Transaction.TransactionStatus.COMPLETED)
                .paymentMethod(Transaction.PaymentMethod.WALLET)
                .sourceBalanceBefore(balanceBefore)
                .sourceBalanceAfter(wallet.getBalance())
                .completedAt(LocalDateTime.now())
                .build();
        tx.calculateTotalAmount();
        transactionRepository.save(tx);

        log.info("Withdrawal completed: ref={}, amount={}, wallet={}", tx.getTransactionReference(),
                request.getAmount(), wallet.getId());
        return WalletResponse.from(wallet);
    }

    // ── send money ─────────────────────────────────────────────────────────────

    @Transactional
    public WalletResponse sendMoney(SendMoneyRequest request, String senderEmail) {
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }
        if (request.getSourceWallet().equals(request.getDestinationWallet())) {
            throw new IllegalArgumentException("Source and destination wallets must be different");
        }

        String idempotencyKey = senderEmail + ":TRANSFER:" + request.getSourceWallet()
                                + ":" + request.getDestinationWallet() + ":" + request.getAmount();
        if (transactionRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            log.warn("Duplicate transfer request ignored (idempotencyKey={})", idempotencyKey);
            Wallet src = walletRepository.findById(request.getSourceWallet())
                    .orElseThrow(() -> new IllegalArgumentException("Source wallet not found"));
            return WalletResponse.from(src);
        }

        // Lock in consistent UUID order to avoid deadlocks
        UUID firstId  = request.getSourceWallet().compareTo(request.getDestinationWallet()) < 0
                ? request.getSourceWallet() : request.getDestinationWallet();
        UUID secondId = firstId.equals(request.getSourceWallet())
                ? request.getDestinationWallet() : request.getSourceWallet();

        Wallet first  = getLockedWalletById(firstId);
        Wallet second = getLockedWalletById(secondId);

        Wallet source      = first.getId().equals(request.getSourceWallet())      ? first : second;
        Wallet destination = first.getId().equals(request.getDestinationWallet()) ? first : second;

        assertOperational(source);
        assertOperational(destination);

        if (!source.getUser().getId().equals(
                userRepository.findByEmail(senderEmail)
                        .orElseThrow(() -> new IllegalArgumentException("Sender not found"))
                        .getId())) {
            throw new IllegalArgumentException("Source wallet does not belong to the authenticated user");
        }

        if (!source.hasSufficientBalance(request.getAmount())) {
            throw new IllegalStateException("Insufficient available balance");
        }
        if (destination.wouldExceedMaxLimit(request.getAmount())) {
            throw new IllegalStateException("Transfer would exceed destination wallet maximum balance limit");
        }

        BigDecimal srcBefore  = source.getBalance();
        BigDecimal destBefore = destination.getBalance();

        source.debit(request.getAmount());
        destination.credit(request.getAmount());

        walletRepository.save(source);
        walletRepository.save(destination);

        User sender = source.getUser();
        Transaction tx = Transaction.builder()
                .transactionReference(generateReference("TRF"))
                .idempotencyKey(idempotencyKey)
                .user(sender)
                .sourceWallet(source)
                .destinationWallet(destination)
                .amount(request.getAmount())
                .currency(source.getCurrency())
                .transactionType(Transaction.TransactionType.TRANSFER)
                .status(Transaction.TransactionStatus.COMPLETED)
                .paymentMethod(Transaction.PaymentMethod.WALLET)
                .sourceBalanceBefore(srcBefore)
                .sourceBalanceAfter(source.getBalance())
                .destinationBalanceBefore(destBefore)
                .destinationBalanceAfter(destination.getBalance())
                .completedAt(LocalDateTime.now())
                .build();
        tx.calculateTotalAmount();
        transactionRepository.save(tx);

        log.info("Transfer completed: ref={}, amount={}, from={}, to={}", tx.getTransactionReference(),
                request.getAmount(), source.getId(), destination.getId());
        return WalletResponse.from(source);
    }
}
