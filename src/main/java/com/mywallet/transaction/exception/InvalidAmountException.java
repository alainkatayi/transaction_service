package com.mywallet.transaction.exception;

public class InvalidAmountException extends RuntimeException {

	public InvalidAmountException() {
		super("Le montant est invalide.");
	}

}
