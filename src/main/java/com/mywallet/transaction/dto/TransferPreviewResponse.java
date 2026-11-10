package com.mywallet.transaction.dto;

public record TransferPreviewResponse(
		String firstName,
		String lastName,
		String phone,
		String amount) {
}
