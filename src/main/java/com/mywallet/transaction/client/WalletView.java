package com.mywallet.transaction.client;

import java.util.UUID;

public record WalletView(UUID walletId, String status, String bankAccountNumber) {

	public boolean active() {
		return "ACTIVE".equals(status);
	}

}
