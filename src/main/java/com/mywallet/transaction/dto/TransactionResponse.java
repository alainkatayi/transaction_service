package com.mywallet.transaction.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import com.mywallet.transaction.entity.TransactionStatus;
import com.mywallet.transaction.entity.TransactionType;
import com.mywallet.transaction.entity.WalletTransaction;

public record TransactionResponse(
		String reference,
		TransactionType type,
		String direction,
		String amount,
		String fee,
		TransactionStatus status,
		String failureReason,
		String bankAccount,
		UUID counterpartyUserId,
		String counterpartyPhone,
		String counterpartyName,
		Instant createdAt,
		Instant updatedAt) {

	public static TransactionResponse forViewer(
			WalletTransaction transaction,
			UUID viewerId,
			String senderPhone,
			String senderName) {
		boolean incoming = transaction.getType() == TransactionType.TRANSFER
				&& viewerId.equals(transaction.getCounterpartyUserId());
		String direction = transaction.getType() == TransactionType.TRANSFER
				? (incoming ? "IN" : "OUT")
				: null;
		String otherPhone = incoming ? senderPhone : transaction.getCounterpartyPhone();
		String otherName = incoming ? senderName : null;
		return new TransactionResponse(
				transaction.getReference(),
				transaction.getType(),
				direction,
				money(transaction.getAmount()),
				money(transaction.getFee()),
				transaction.getStatus(),
				transaction.getFailureReason(),
				mask(transaction.getBankAccount()),
				incoming ? transaction.getUserId() : transaction.getCounterpartyUserId(),
				otherPhone,
				otherName,
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
