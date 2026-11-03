package com.mywallet.transaction.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mywallet.transaction.entity.TransactionStepLog;
import com.mywallet.transaction.entity.TransactionType;
import com.mywallet.transaction.entity.WalletTransaction;
import com.mywallet.transaction.repository.TransactionStepLogRepository;
import com.mywallet.transaction.repository.WalletTransactionRepository;

@Service
public class TransactionInserter {

	private static final BigDecimal FEE = new BigDecimal("0.00");

	private final WalletTransactionRepository transactions;
	private final TransactionStepLogRepository steps;
	private final Clock clock;

	public TransactionInserter(
			WalletTransactionRepository transactions,
			TransactionStepLogRepository steps,
			Clock clock) {
		this.transactions = transactions;
		this.steps = steps;
		this.clock = clock;
	}

	@Transactional
	public WalletTransaction insert(
			UUID userId,
			String idempotencyKey,
			TransactionType type,
			BigDecimal amount,
			String bankAccount,
			String requestHash) {
		Instant now = clock.instant();
		UUID id = UUID.randomUUID();
		WalletTransaction transaction = WalletTransaction.start(
				id,
				"TX-" + id,
				userId,
				idempotencyKey,
				type,
				amount,
				FEE,
				bankAccount,
				requestHash,
				now);
		WalletTransaction saved = transactions.saveAndFlush(transaction);
		steps.save(TransactionStepLog.entry(saved.getId(), TransactionSteps.CREATED, "RECORDED", null, now));
		return saved;
	}

}
