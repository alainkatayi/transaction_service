package com.mywallet.transaction.exception;

public class ValidationException extends RuntimeException {

	public ValidationException() {
		super("La requête est invalide.");
	}

}
