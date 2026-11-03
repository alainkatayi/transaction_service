package com.mywallet.transaction.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transaction_step_log")
public class TransactionStepLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "transaction_id", nullable = false, updatable = false)
	private UUID transactionId;

	@Column(name = "step", nullable = false, updatable = false, length = 30)
	private String step;

	@Column(name = "outcome", nullable = false, updatable = false, length = 20)
	private String outcome;

	@JdbcTypeCode(SqlTypes.LONGVARCHAR)
	@Column(name = "detail", updatable = false)
	private String detail;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected TransactionStepLog() {
	}

	public static TransactionStepLog entry(
			UUID transactionId,
			String step,
			String outcome,
			String detail,
			Instant createdAt) {
		TransactionStepLog log = new TransactionStepLog();
		log.transactionId = transactionId;
		log.step = step;
		log.outcome = outcome;
		log.detail = detail;
		log.createdAt = createdAt;
		return log;
	}

}
