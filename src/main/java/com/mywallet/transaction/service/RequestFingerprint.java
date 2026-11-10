package com.mywallet.transaction.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.math.BigDecimal;

import com.mywallet.transaction.entity.TransactionType;

public final class RequestFingerprint {

	private RequestFingerprint() {
	}

	public static String hash(TransactionType type, BigDecimal amount) {
		return digest(type.name() + amount.toPlainString());
	}

	public static String transferHash(BigDecimal amount, String recipientPhone) {
		return digest(TransactionType.TRANSFER.name() + amount.toPlainString() + recipientPhone);
	}

	private static String digest(String canonical) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(canonical.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256");
		}
	}

	public static String refundReference(String reference) {
		String suffix = "-R";
		if (reference.length() + suffix.length() <= 64) {
			return reference + suffix;
		}
		return reference.substring(0, 64 - suffix.length()) + suffix;
	}

}
