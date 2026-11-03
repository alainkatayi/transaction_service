package com.mywallet.transaction.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.mywallet.transaction.client.BankClient;
import com.mywallet.transaction.client.RemoteOutcome;
import com.mywallet.transaction.client.WalletClient;
import com.mywallet.transaction.entity.TransactionStatus;
import com.mywallet.transaction.entity.TransactionType;
import com.mywallet.transaction.entity.WalletTransaction;
import com.mywallet.transaction.repository.WalletTransactionRepository;

@Service
public class TransactionMachine {

	private final WalletTransactionRepository transactions;
	private final TransactionTransitions transitions;
	private final BankClient bank;
	private final WalletClient wallets;

	public TransactionMachine(
			WalletTransactionRepository transactions,
			TransactionTransitions transitions,
			BankClient bank,
			WalletClient wallets) {
		this.transactions = transactions;
		this.transitions = transitions;
		this.bank = bank;
		this.wallets = wallets;
	}

	public WalletTransaction run(UUID id) {
		for (int attempt = 0; attempt < 6; attempt++) {
			WalletTransaction current = transactions.findById(id).orElseThrow();
			if (current.getStatus() != TransactionStatus.PENDING) {
				return current;
			}
			if (!advance(current)) {
				return transactions.findById(id).orElseThrow();
			}
		}
		return transactions.findById(id).orElseThrow();
	}

	private boolean advance(WalletTransaction transaction) {
		if (transaction.getType() == TransactionType.TOPUP) {
			return topup(transaction);
		}
		return withdraw(transaction);
	}

	private boolean topup(WalletTransaction transaction) {
		return switch (transaction.getStep()) {
			case TransactionSteps.CREATED -> debitBank(transaction);
			case TransactionSteps.BANK_DEBITED -> creditWallet(transaction);
			case TransactionSteps.REFUND_PENDING -> refundBank(transaction);
			default -> false;
		};
	}

	private boolean withdraw(WalletTransaction transaction) {
		return switch (transaction.getStep()) {
			case TransactionSteps.CREATED -> debitWallet(transaction);
			case TransactionSteps.WALLET_DEBITED -> creditBank(transaction);
			case TransactionSteps.REVERSE_PENDING -> reverseWallet(transaction);
			default -> false;
		};
	}

	private boolean debitBank(WalletTransaction transaction) {
		RemoteOutcome outcome = bank.debit(transaction.getReference(), transaction.getBankAccount(), transaction.getAmount());
		if (outcome.succeeded()) {
			return transitions.transition(
					transaction.getId(),
					TransactionSteps.CREATED,
					TransactionStatus.PENDING,
					TransactionSteps.BANK_DEBITED,
					null);
		}
		if (outcome.wasRejected()) {
			transitions.transition(
					transaction.getId(),
					TransactionSteps.CREATED,
					TransactionStatus.FAILED,
					TransactionSteps.FAILED,
					outcome.reason());
		}
		return false;
	}

	private boolean creditWallet(WalletTransaction transaction) {
		RemoteOutcome outcome = wallets.credit(transaction.getUserId(), transaction.getReference(), transaction.getAmount());
		if (outcome.succeeded()) {
			transitions.transition(
					transaction.getId(),
					TransactionSteps.BANK_DEBITED,
					TransactionStatus.SUCCESS,
					TransactionSteps.WALLET_CREDITED,
					null);
			return false;
		}
		if (outcome.wasRejected()) {
			return transitions.transition(
					transaction.getId(),
					TransactionSteps.BANK_DEBITED,
					TransactionStatus.PENDING,
					TransactionSteps.REFUND_PENDING,
					outcome.reason());
		}
		return false;
	}

	private boolean refundBank(WalletTransaction transaction) {
		RemoteOutcome outcome = bank.credit(
				RequestFingerprint.refundReference(transaction.getReference()),
				transaction.getBankAccount(),
				transaction.getAmount());
		if (!outcome.succeeded()) {
			return false;
		}
		transitions.transition(
				transaction.getId(),
				TransactionSteps.REFUND_PENDING,
				TransactionStatus.REVERSED,
				TransactionSteps.REVERSED,
				transaction.getFailureReason());
		return false;
	}

	private boolean debitWallet(WalletTransaction transaction) {
		RemoteOutcome outcome = wallets.debit(transaction.getUserId(), transaction.getReference(), transaction.getAmount());
		if (outcome.succeeded()) {
			return transitions.transition(
					transaction.getId(),
					TransactionSteps.CREATED,
					TransactionStatus.PENDING,
					TransactionSteps.WALLET_DEBITED,
					null);
		}
		if (outcome.wasRejected()) {
			transitions.transition(
					transaction.getId(),
					TransactionSteps.CREATED,
					TransactionStatus.FAILED,
					TransactionSteps.FAILED,
					outcome.reason());
		}
		return false;
	}

	private boolean creditBank(WalletTransaction transaction) {
		RemoteOutcome outcome = bank.credit(transaction.getReference(), transaction.getBankAccount(), transaction.getAmount());
		if (outcome.succeeded()) {
			transitions.transition(
					transaction.getId(),
					TransactionSteps.WALLET_DEBITED,
					TransactionStatus.SUCCESS,
					TransactionSteps.BANK_CREDITED,
					null);
			return false;
		}
		if (outcome.wasRejected()) {
			return transitions.transition(
					transaction.getId(),
					TransactionSteps.WALLET_DEBITED,
					TransactionStatus.PENDING,
					TransactionSteps.REVERSE_PENDING,
					outcome.reason());
		}
		return false;
	}

	private boolean reverseWallet(WalletTransaction transaction) {
		RemoteOutcome outcome = wallets.reverse(transaction.getUserId(), transaction.getReference());
		if (!outcome.succeeded()) {
			return false;
		}
		transitions.transition(
				transaction.getId(),
				TransactionSteps.REVERSE_PENDING,
				TransactionStatus.REVERSED,
				TransactionSteps.REVERSED,
				transaction.getFailureReason());
		return false;
	}

}
