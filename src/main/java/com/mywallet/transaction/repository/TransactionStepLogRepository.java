package com.mywallet.transaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mywallet.transaction.entity.TransactionStepLog;

public interface TransactionStepLogRepository extends JpaRepository<TransactionStepLog, Long> {
}
