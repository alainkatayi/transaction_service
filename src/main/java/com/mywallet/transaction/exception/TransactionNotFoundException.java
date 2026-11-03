package com.mywallet.transaction.exception;

public class TransactionNotFoundException extends RuntimeException {

	public TransactionNotFoundException() {
		super("Transaction introuvable.");
	}

}
