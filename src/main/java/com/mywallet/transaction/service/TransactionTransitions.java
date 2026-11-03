package com.mywallet.transaction.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mywallet.transaction.entity.TransactionStatus;
import com.mywallet.transaction.entity.TransactionStepLog;
import com.mywallet.transaction.repository.TransactionStepLogRepository;
import com.mywallet.transaction.repository.WalletTransactionRepository;

@Service
public class TransactionTransitions {

	private final WalletTransactionRepository transactions;
	private final TransactionStepLogRepository steps;
	private final Clock clock;

	public TransactionTransitions(
			WalletTransactionRepository transactions,
			TransactionStepLogRepository steps,
			Clock clock) {
		this.transactions = transactions;
		this.steps = steps;
		this.clock = clock;
	}

	@Transactional
	public boolean transition(UUID id, String expectedStep, TransactionStatus status, String step, String reason) {
		Instant now = clock.instant();
		int updated = transactions.transition(id, expectedStep, status.name(), step, reason, now);
		if (updated != 1) {
			return false;
		}
		steps.save(TransactionStepLog.entry(id, step, outcome(status), reason, now));
		return true;
	}

	@Transactional
	public boolean claim(UUID id, Instant expectedUpdatedAt) {
		return transactions.claim(id, expectedUpdatedAt, clock.instant()) == 1;
	}

	private static String outcome(TransactionStatus status) {
		return switch (status) {
			case SUCCESS -> "SUCCESS";
			case FAILED -> "FAILED";
			case REVERSED -> "REVERSED";
			case PENDING -> "MOVED";
		};
	}

}
