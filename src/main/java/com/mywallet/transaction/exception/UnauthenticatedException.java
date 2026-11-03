package com.mywallet.transaction.exception;

public class UnauthenticatedException extends RuntimeException {

	public UnauthenticatedException() {
		super("Authentification requise.");
	}

}
