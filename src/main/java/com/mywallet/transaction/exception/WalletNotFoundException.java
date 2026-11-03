package com.mywallet.transaction.exception;

public class WalletNotFoundException extends RuntimeException {

	public WalletNotFoundException() {
		super("Wallet introuvable.");
	}

}
