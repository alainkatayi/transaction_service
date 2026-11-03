package com.mywallet.transaction.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mywallet.transaction.dto.TransactionPage;
import com.mywallet.transaction.dto.TransactionResponse;
import com.mywallet.transaction.entity.TransactionStatus;
import com.mywallet.transaction.entity.TransactionType;
import com.mywallet.transaction.security.CurrentUser;
import com.mywallet.transaction.service.TransactionService;
import com.mywallet.transaction.service.TransactionService.Submission;

import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

	private final CurrentUser currentUser;
	private final TransactionService transactions;

	public TransactionController(CurrentUser currentUser, TransactionService transactions) {
		this.currentUser = currentUser;
		this.transactions = transactions;
	}

	@PostMapping("/topup")
	public ResponseEntity<TransactionResponse> topup(
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
			@RequestBody(required = false) JsonNode body) {
		return respond(transactions.submit(currentUser.id(), idempotencyKey, TransactionType.TOPUP, body));
	}

	@PostMapping("/withdraw")
	public ResponseEntity<TransactionResponse> withdraw(
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
			@RequestBody(required = false) JsonNode body) {
		return respond(transactions.submit(currentUser.id(), idempotencyKey, TransactionType.WITHDRAW, body));
	}

	@GetMapping
	public TransactionPage list(
			@RequestParam(required = false) String type,
			@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return transactions.list(currentUser.id(), type, status, page, size);
	}

	@GetMapping("/{reference}")
	public TransactionResponse one(@PathVariable String reference) {
		return transactions.get(currentUser.id(), reference);
	}

	private static ResponseEntity<TransactionResponse> respond(Submission submission) {
		TransactionResponse body = TransactionResponse.from(submission.transaction());
		if (!submission.created()) {
			return ResponseEntity.ok(body);
		}
		if (submission.transaction().getStatus() == TransactionStatus.PENDING) {
			return ResponseEntity.accepted().body(body);
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

}
