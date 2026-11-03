package com.mywallet.transaction.exception;

public class BankUnavailableException extends RuntimeException {

	public BankUnavailableException() {
		super("La banque est indisponible.");
	}

}
