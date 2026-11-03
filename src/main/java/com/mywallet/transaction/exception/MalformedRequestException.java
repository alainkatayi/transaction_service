package com.mywallet.transaction.exception;

public class MalformedRequestException extends RuntimeException {

	public MalformedRequestException() {
		super("Le corps de la requête est illisible.");
	}

}
