package com.mywallet.transaction.exception;

public class IdempotencyConflictException extends RuntimeException {

	public IdempotencyConflictException() {
		super("Conflit d'idempotence.");
	}

}
