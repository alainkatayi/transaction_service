package com.mywallet.transaction.exception;

public class WalletNotActiveException extends RuntimeException {

	public WalletNotActiveException() {
		super("Le wallet n'est pas actif.");
	}

}
