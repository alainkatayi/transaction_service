package com.mywallet.transaction.service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.mywallet.transaction.client.UserClient;
import com.mywallet.transaction.client.UserClient.UserView;
import com.mywallet.transaction.client.WalletClient;
import com.mywallet.transaction.client.WalletView;
import com.mywallet.transaction.dto.TransferPreviewResponse;
import com.mywallet.transaction.exception.RecipientNotFoundException;
import com.mywallet.transaction.exception.RecipientWalletNotActiveException;
import com.mywallet.transaction.exception.SelfTransferException;
import com.mywallet.transaction.exception.ValidationException;
import com.mywallet.transaction.exception.WalletNotActiveException;
import com.mywallet.transaction.exception.WalletNotFoundException;

@Service
public class TransferSupport {

	private static final Pattern PHONE = Pattern.compile("^\\+[1-9][0-9]{7,14}$");

	private final UserClient users;
	private final WalletClient wallets;

	public TransferSupport(UserClient users, WalletClient wallets) {
		this.users = users;
		this.wallets = wallets;
	}

	public TransferPreviewResponse preview(UUID senderId, String rawPhone, BigDecimal amount) {
		ResolvedRecipient recipient = resolve(senderId, rawPhone);
		return new TransferPreviewResponse(
				recipient.firstName(),
				recipient.lastName(),
				recipient.phone(),
				amount.toPlainString());
	}

	public ResolvedRecipient resolve(UUID senderId, String rawPhone) {
		String phone = normalizePhone(rawPhone);
		UserView user;
		try {
			user = users.byPhone(phone);
		}
		catch (RecipientNotFoundException exception) {
			throw exception;
		}
		if (user.id().equals(senderId)) {
			throw new SelfTransferException();
		}
		if ("SUSPENDED".equals(user.status())) {
			throw new RecipientNotFoundException();
		}
		WalletView sender = wallets.getWallet(senderId);
		if (!sender.active()) {
			throw new WalletNotActiveException();
		}
		WalletView recipientWallet;
		try {
			recipientWallet = wallets.getWallet(user.id());
		}
		catch (WalletNotFoundException exception) {
			throw new RecipientNotFoundException();
		}
		if (!recipientWallet.active()) {
			throw new RecipientWalletNotActiveException();
		}
		return new ResolvedRecipient(user.id(), user.firstName(), user.lastName(), user.phone());
	}

	public static String normalizePhone(String phone) {
		if (phone == null || phone.isBlank()) {
			throw new ValidationException();
		}
		String normalized = phone.replace(" ", "").replace("-", "");
		if (!PHONE.matcher(normalized).matches()) {
			throw new ValidationException();
		}
		return normalized;
	}

	public record ResolvedRecipient(UUID userId, String firstName, String lastName, String phone) {
	}

}
