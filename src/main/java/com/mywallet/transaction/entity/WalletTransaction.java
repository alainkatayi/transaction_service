package com.mywallet.transaction.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

/**
 * Un identifiant assigné avec @Version fait croire à Spring Data que la ligne existe déjà.
 * Persistable garde newEntity à vrai jusqu'à @PostPersist et @PostLoad.
 */
@Entity
@Table(name = "wallet_transaction")
public class WalletTransaction implements Persistable<UUID> {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "reference", nullable = false, updatable = false, length = 64)
	private String reference;

	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;

	@Column(name = "idempotency_key", nullable = false, updatable = false, length = 64)
	private String idempotencyKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, updatable = false, length = 10)
	private TransactionType type;

	@Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(name = "fee", nullable = false, updatable = false, precision = 19, scale = 2)
	private BigDecimal fee;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 10)
	private TransactionStatus status;

	@Column(name = "step", nullable = false, length = 30)
	private String step;

	@Column(name = "failure_reason", length = 40)
	private String failureReason;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "bank_account", updatable = false, length = 12)
	private String bankAccount;

	@Column(name = "counterparty_user_id", updatable = false)
	private UUID counterpartyUserId;

	@Column(name = "counterparty_phone", updatable = false, length = 20)
	private String counterpartyPhone;

	@Column(name = "sender_phone", updatable = false, length = 20)
	private String senderPhone;

	@Column(name = "sender_name", updatable = false, length = 201)
	private String senderName;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "request_hash", nullable = false, updatable = false, length = 64)
	private String requestHash;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Transient
	private boolean newEntity;

	protected WalletTransaction() {
		this.newEntity = false;
	}

	private WalletTransaction(
			UUID id,
			String reference,
			UUID userId,
			String idempotencyKey,
			TransactionType type,
			BigDecimal amount,
			BigDecimal fee,
			String bankAccount,
			UUID counterpartyUserId,
			String counterpartyPhone,
			String senderPhone,
			String senderName,
			String requestHash,
			Instant now) {
		this.id = id;
		this.reference = reference;
		this.userId = userId;
		this.idempotencyKey = idempotencyKey;
		this.type = type;
		this.amount = amount;
		this.fee = fee;
		this.status = TransactionStatus.PENDING;
		this.step = "CREATED";
		this.bankAccount = bankAccount;
		this.counterpartyUserId = counterpartyUserId;
		this.counterpartyPhone = counterpartyPhone;
		this.senderPhone = senderPhone;
		this.senderName = senderName;
		this.requestHash = requestHash;
		this.createdAt = now;
		this.updatedAt = now;
		this.newEntity = true;
	}

	public static WalletTransaction start(
			UUID id,
			String reference,
			UUID userId,
			String idempotencyKey,
			TransactionType type,
			BigDecimal amount,
			BigDecimal fee,
			String bankAccount,
			String requestHash,
			Instant now) {
		return new WalletTransaction(
				id,
				reference,
				userId,
				idempotencyKey,
				type,
				amount,
				fee,
				bankAccount,
				null,
				null,
				null,
				null,
				requestHash,
				now);
	}

	public static WalletTransaction transfer(
			UUID id,
			String reference,
			UUID userId,
			String idempotencyKey,
			BigDecimal amount,
			BigDecimal fee,
			UUID counterpartyUserId,
			String counterpartyPhone,
			String senderPhone,
			String senderName,
			String requestHash,
			Instant now) {
		return new WalletTransaction(
				id,
				reference,
				userId,
				idempotencyKey,
				TransactionType.TRANSFER,
				amount,
				fee,
				null,
				counterpartyUserId,
				counterpartyPhone,
				senderPhone,
				senderName,
				requestHash,
				now);
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public boolean isNew() {
		return newEntity;
	}

	@PostPersist
	@PostLoad
	void markNotNew() {
		this.newEntity = false;
	}

	public String getReference() {
		return reference;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public TransactionType getType() {
		return type;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public BigDecimal getFee() {
		return fee;
	}

	public TransactionStatus getStatus() {
		return status;
	}

	public String getStep() {
		return step;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public String getBankAccount() {
		return bankAccount == null ? null : bankAccount.trim();
	}

	public UUID getCounterpartyUserId() {
		return counterpartyUserId;
	}

	public String getCounterpartyPhone() {
		return counterpartyPhone;
	}

	public String getSenderPhone() {
		return senderPhone;
	}

	public String getSenderName() {
		return senderName;
	}

	public String getRequestHash() {
		return requestHash == null ? null : requestHash.trim();
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	@Override
	public String toString() {
		return "WalletTransaction{reference=" + reference + ", status=" + status + ", step=" + step + "}";
	}

}
