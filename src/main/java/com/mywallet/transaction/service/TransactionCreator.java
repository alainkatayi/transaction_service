package com.mywallet.transaction.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.mywallet.transaction.client.UserClient;
import com.mywallet.transaction.client.UserClient.UserView;
import com.mywallet.transaction.client.WalletClient;
import com.mywallet.transaction.client.WalletView;
import com.mywallet.transaction.entity.TransactionType;
import com.mywallet.transaction.entity.WalletTransaction;
import com.mywallet.transaction.exception.ServiceUnavailableException;
import com.mywallet.transaction.exception.WalletNotActiveException;
import com.mywallet.transaction.service.TransferSupport.ResolvedRecipient;

@Service
public class TransactionCreator {

	private final WalletClient wallets;
	private final UserClient users;
	private final TransactionInserter inserter;
	private final TransferSupport transfers;

	public TransactionCreator(
			WalletClient wallets,
			UserClient users,
			TransactionInserter inserter,
			TransferSupport transfers) {
		this.wallets = wallets;
		this.users = users;
		this.inserter = inserter;
		this.transfers = transfers;
	}

	public WalletTransaction create(
			UUID userId,
			String idempotencyKey,
			TransactionType type,
			BigDecimal amount,
			String requestHash) {
		WalletView wallet = wallets.getWallet(userId);
		if (!wallet.active()) {
			throw new WalletNotActiveException();
		}
		String account = wallet.bankAccountNumber();
		if (account == null || !account.matches("\\d{12}")) {
			throw new ServiceUnavailableException();
		}
		return inserter.insert(userId, idempotencyKey, type, amount, account, requestHash);
	}

	public WalletTransaction createTransfer(
			UUID userId,
			String idempotencyKey,
			BigDecimal amount,
			String recipientPhone,
			String requestHash) {
		ResolvedRecipient recipient = transfers.resolve(userId, recipientPhone);
		UserView sender = users.byId(userId);
		String senderName = (sender.firstName() + " " + sender.lastName()).trim();
		return inserter.insertTransfer(
				userId,
				idempotencyKey,
				amount,
				recipient.userId(),
				recipient.phone(),
				sender.phone(),
				senderName,
				requestHash);
	}

}
