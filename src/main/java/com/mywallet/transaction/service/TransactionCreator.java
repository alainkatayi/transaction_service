package com.mywallet.transaction.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.mywallet.transaction.client.WalletClient;
import com.mywallet.transaction.client.WalletView;
import com.mywallet.transaction.entity.TransactionType;
import com.mywallet.transaction.entity.WalletTransaction;
import com.mywallet.transaction.exception.ServiceUnavailableException;
import com.mywallet.transaction.exception.WalletNotActiveException;

@Service
public class TransactionCreator {

	private final WalletClient wallets;
	private final TransactionInserter inserter;

	public TransactionCreator(WalletClient wallets, TransactionInserter inserter) {
		this.wallets = wallets;
		this.inserter = inserter;
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

}
