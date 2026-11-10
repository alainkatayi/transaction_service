package com.mywallet.transaction.service;

public final class TransactionSteps {

	public static final String CREATED = "CREATED";
	public static final String BANK_DEBITED = "BANK_DEBITED";
	public static final String WALLET_CREDITED = "WALLET_CREDITED";
	public static final String REFUND_PENDING = "REFUND_PENDING";
	public static final String WALLET_DEBITED = "WALLET_DEBITED";
	public static final String BANK_CREDITED = "BANK_CREDITED";
	public static final String RECIPIENT_CREDITED = "RECIPIENT_CREDITED";
	public static final String REVERSE_PENDING = "REVERSE_PENDING";
	public static final String FAILED = "FAILED";
	public static final String REVERSED = "REVERSED";

	private TransactionSteps() {
	}

}
