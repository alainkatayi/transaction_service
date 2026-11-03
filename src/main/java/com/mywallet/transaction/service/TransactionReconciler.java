package com.mywallet.transaction.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mywallet.transaction.entity.TransactionStatus;
import com.mywallet.transaction.entity.WalletTransaction;
import com.mywallet.transaction.repository.WalletTransactionRepository;

@Component
public class TransactionReconciler {

	private static final Logger log = LoggerFactory.getLogger(TransactionReconciler.class);
	private static final Duration STUCK = Duration.ofHours(1);

	private final WalletTransactionRepository transactions;
	private final TransactionTransitions transitions;
	private final TransactionMachine machine;
	private final Clock clock;
	private final Duration staleAfter;

	public TransactionReconciler(
			WalletTransactionRepository transactions,
			TransactionTransitions transitions,
			TransactionMachine machine,
			Clock clock,
			@Value("${app.reconcile.stale-after}") Duration staleAfter) {
		this.transactions = transactions;
		this.transitions = transitions;
		this.machine = machine;
		this.clock = clock;
		this.staleAfter = staleAfter;
	}

	@Scheduled(fixedDelayString = "${app.reconcile.interval}")
	public void reconcile() {
		Instant now = clock.instant();
		List<WalletTransaction> stale = transactions.findTop100ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
				TransactionStatus.PENDING,
				now.minus(staleAfter));
		for (WalletTransaction transaction : stale) {
			try {
				if (transaction.getUpdatedAt().isBefore(now.minus(STUCK))) {
					log.error("Transaction {} encore PENDING après une heure", transaction.getReference());
				}
				if (!transitions.claim(transaction.getId(), transaction.getUpdatedAt())) {
					continue;
				}
				log.warn("Reprise de la transaction {}", transaction.getReference());
				machine.run(transaction.getId());
			}
			catch (RuntimeException exception) {
				log.error("Reprise interrompue pour la transaction {}", transaction.getReference());
			}
		}
	}

}
