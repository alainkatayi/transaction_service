package com.mywallet.transaction.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mywallet.transaction.entity.TransactionStatus;
import com.mywallet.transaction.entity.TransactionType;
import com.mywallet.transaction.entity.WalletTransaction;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

	Optional<WalletTransaction> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

	Optional<WalletTransaction> findByUserIdAndReference(UUID userId, String reference);

	Page<WalletTransaction> findByUserId(UUID userId, Pageable pageable);

	Page<WalletTransaction> findByUserIdAndType(UUID userId, TransactionType type, Pageable pageable);

	Page<WalletTransaction> findByUserIdAndStatus(UUID userId, TransactionStatus status, Pageable pageable);

	Page<WalletTransaction> findByUserIdAndTypeAndStatus(
			UUID userId,
			TransactionType type,
			TransactionStatus status,
			Pageable pageable);

	List<WalletTransaction> findTop100ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
			TransactionStatus status,
			Instant updatedAt);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			update wallet_transaction
			set status = :status,
			    step = :step,
			    failure_reason = :reason,
			    updated_at = :now,
			    version = version + 1
			where id = :id and step = :expected and status = 'PENDING'
			""", nativeQuery = true)
	int transition(
			@Param("id") UUID id,
			@Param("expected") String expected,
			@Param("status") String status,
			@Param("step") String step,
			@Param("reason") String reason,
			@Param("now") Instant now);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			update wallet_transaction
			set updated_at = :now,
			    version = version + 1
			where id = :id and updated_at = :expected and status = 'PENDING'
			""", nativeQuery = true)
	int claim(
			@Param("id") UUID id,
			@Param("expected") Instant expected,
			@Param("now") Instant now);

}
