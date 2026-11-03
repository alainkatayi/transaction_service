package com.mywallet.transaction.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.mywallet.transaction.dto.TransactionPage;
import com.mywallet.transaction.dto.TransactionResponse;
import com.mywallet.transaction.entity.TransactionStatus;
import com.mywallet.transaction.entity.TransactionType;
import com.mywallet.transaction.entity.WalletTransaction;
import com.mywallet.transaction.exception.IdempotencyConflictException;
import com.mywallet.transaction.exception.InvalidAmountException;
import com.mywallet.transaction.exception.TransactionNotFoundException;
import com.mywallet.transaction.exception.ValidationException;
import com.mywallet.transaction.repository.WalletTransactionRepository;

import tools.jackson.databind.JsonNode;

@Service
public class TransactionService {

	private final WalletTransactionRepository transactions;
	private final TransactionCreator creator;
	private final TransactionMachine machine;

	public TransactionService(
			WalletTransactionRepository transactions,
			TransactionCreator creator,
			TransactionMachine machine) {
		this.transactions = transactions;
		this.creator = creator;
		this.machine = machine;
	}

	public Submission submit(UUID userId, String idempotencyKey, TransactionType type, JsonNode body) {
		String key = key(idempotencyKey);
		BigDecimal amount = amount(body);
		String hash = RequestFingerprint.hash(type, amount);
		Optional<WalletTransaction> existing = transactions.findByUserIdAndIdempotencyKey(userId, key);
		if (existing.isPresent()) {
			return sameRequest(existing.get(), hash);
		}
		try {
			WalletTransaction created = creator.create(userId, key, type, amount, hash);
			return Submission.created(machine.run(created.getId()));
		}
		catch (DataIntegrityViolationException exception) {
			// Une transaction ayant rencontré une erreur de base est condamnée au rollback.
			if (!idempotencyConstraint(exception)) {
				throw new IllegalStateException("enregistrement");
			}
			WalletTransaction winner = transactions.findByUserIdAndIdempotencyKey(userId, key)
					.orElseThrow(() -> new IllegalStateException("enregistrement"));
			return sameRequest(winner, hash);
		}
	}

	public TransactionPage list(UUID userId, String type, String status, int page, int size) {
		if (page < 0 || size < 1) {
			throw new ValidationException();
		}
		int limited = Math.min(size, 100);
		TransactionType parsedType = enumValue(type, TransactionType.class);
		TransactionStatus parsedStatus = enumValue(status, TransactionStatus.class);
		PageRequest request = PageRequest.of(page, limited, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
		Page<WalletTransaction> result = search(userId, parsedType, parsedStatus, request);
		return new TransactionPage(
				result.getContent().stream().map(TransactionResponse::from).toList(),
				result.getNumber(),
				result.getSize(),
				result.getTotalElements());
	}

	public TransactionResponse get(UUID userId, String reference) {
		if (reference == null || reference.isBlank() || reference.length() > 64) {
			throw new TransactionNotFoundException();
		}
		return transactions.findByUserIdAndReference(userId, reference)
				.map(TransactionResponse::from)
				.orElseThrow(TransactionNotFoundException::new);
	}

	private Page<WalletTransaction> search(
			UUID userId,
			TransactionType type,
			TransactionStatus status,
			PageRequest request) {
		if (type != null && status != null) {
			return transactions.findByUserIdAndTypeAndStatus(userId, type, status, request);
		}
		if (type != null) {
			return transactions.findByUserIdAndType(userId, type, request);
		}
		if (status != null) {
			return transactions.findByUserIdAndStatus(userId, status, request);
		}
		return transactions.findByUserId(userId, request);
	}

	private static Submission sameRequest(WalletTransaction existing, String hash) {
		if (!hash.equals(existing.getRequestHash())) {
			throw new IdempotencyConflictException();
		}
		return Submission.existing(existing);
	}

	private static boolean idempotencyConstraint(DataIntegrityViolationException exception) {
		String name = DataIntegrityConstraints.name(exception);
		return name != null && name.contains("idempotency");
	}

	private static String key(String idempotencyKey) {
		String value = idempotencyKey == null ? "" : idempotencyKey.trim();
		if (value.length() < 8 || value.length() > 64 || value.chars().anyMatch(ch -> ch < 0x20 || ch == 0x7F)) {
			throw new ValidationException();
		}
		return value;
	}

	private static BigDecimal amount(JsonNode body) {
		if (body == null || !body.isObject()) {
			throw new ValidationException();
		}
		JsonNode node = body.get("amount");
		if (node == null || node.isNull()) {
			throw new ValidationException();
		}
		if (!node.isString()) {
			throw new InvalidAmountException();
		}
		String text = node.asString().trim();
		if (!text.matches("(?:0|[1-9]\\d*)(?:\\.\\d{1,2})?")) {
			throw new InvalidAmountException();
		}
		try {
			BigDecimal value = new BigDecimal(text).setScale(2, RoundingMode.UNNECESSARY);
			if (value.signum() <= 0) {
				throw new InvalidAmountException();
			}
			return value;
		}
		catch (NumberFormatException | ArithmeticException exception) {
			throw new InvalidAmountException();
		}
	}

	private static <E extends Enum<E>> E enumValue(String raw, Class<E> type) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return Enum.valueOf(type, raw.trim());
		}
		catch (IllegalArgumentException exception) {
			throw new ValidationException();
		}
	}

	public record Submission(WalletTransaction transaction, boolean created) {

		public static Submission created(WalletTransaction transaction) {
			return new Submission(transaction, true);
		}

		public static Submission existing(WalletTransaction transaction) {
			return new Submission(transaction, false);
		}

	}

}
