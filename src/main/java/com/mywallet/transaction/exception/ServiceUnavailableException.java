package com.mywallet.transaction.exception;

public class ServiceUnavailableException extends RuntimeException {

	public ServiceUnavailableException() {
		super("Service indisponible.");
	}

}
