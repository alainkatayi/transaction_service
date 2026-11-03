package com.mywallet.transaction.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import com.mywallet.transaction.entity.TransactionStatus;
import com.mywallet.transaction.entity.TransactionType;
import com.mywallet.transaction.entity.WalletTransaction;

public record TransactionResponse(
		String reference,
		TransactionType type,
		String amount,
		String fee,
		TransactionStatus status,
		String failureReason,
		String bankAccount,
		Instant createdAt,
		Instant updatedAt) {

	public static TransactionResponse from(WalletTransaction transaction) {
		return new TransactionResponse(
				transaction.getReference(),
				transaction.getType(),
				money(transaction.getAmount()),
				money(transaction.getFee()),
				transaction.getStatus(),
				transaction.getFailureReason(),
				mask(transaction.getBankAccount()),
				transaction.getCreatedAt(),
				transaction.getUpdatedAt());
	}

	private static String money(BigDecimal value) {
		return value.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
	}

	static String mask(String accountNumber) {
		if (accountNumber == null) {
			return null;
		}
		String digits = accountNumber.trim();
		if (digits.length() <= 4) {
			return digits;
		}
		return "*".repeat(digits.length() - 4) + digits.substring(digits.length() - 4);
	}

}
