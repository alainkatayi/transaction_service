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

	@Query("""
			select t from WalletTransaction t
			where t.reference = :reference
			  and (t.userId = :userId or t.counterpartyUserId = :userId)
			""")
	Optional<WalletTransaction> findVisibleByReference(@Param("userId") UUID userId, @Param("reference") String reference);

	@Query("""
			select t from WalletTransaction t
			where t.userId = :userId or t.counterpartyUserId = :userId
			""")
	Page<WalletTransaction> findVisible(@Param("userId") UUID userId, Pageable pageable);

	@Query("""
			select t from WalletTransaction t
			where (t.userId = :userId or t.counterpartyUserId = :userId)
			  and t.type = :type
			""")
	Page<WalletTransaction> findVisibleByType(
			@Param("userId") UUID userId,
			@Param("type") TransactionType type,
			Pageable pageable);

	@Query("""
			select t from WalletTransaction t
			where (t.userId = :userId or t.counterpartyUserId = :userId)
			  and t.status = :status
			""")
	Page<WalletTransaction> findVisibleByStatus(
			@Param("userId") UUID userId,
			@Param("status") TransactionStatus status,
			Pageable pageable);

	@Query("""
			select t from WalletTransaction t
			where (t.userId = :userId or t.counterpartyUserId = :userId)
			  and t.type = :type
			  and t.status = :status
			""")
	Page<WalletTransaction> findVisibleByTypeAndStatus(
			@Param("userId") UUID userId,
			@Param("type") TransactionType type,
			@Param("status") TransactionStatus status,
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
