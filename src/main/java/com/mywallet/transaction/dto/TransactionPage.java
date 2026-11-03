package com.mywallet.transaction.dto;

import java.util.List;

public record TransactionPage(List<TransactionResponse> items, int page, int size, long totalItems) {
}
