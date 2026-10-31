package com.swizima.clienttransactionsapi.repository;

import com.swizima.clienttransactionsapi.entity.TransactionPayment;
import com.swizima.clienttransactionsapi.entity.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionPaymentRepository extends JpaRepository<TransactionPayment, UUID> {

    List<TransactionPayment> findByTransactionIdAndRecordStatusNotOrderByPaymentDateTimeDesc(UUID transactionId,
                                                                                             RecordStatus recordStatus);

    Optional<TransactionPayment> findByIdAndTransactionIdAndRecordStatusNot(UUID id, UUID transactionId,
                                                                           RecordStatus recordStatus);

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM TransactionPayment p
            WHERE p.transaction.id = :transactionId AND p.recordStatus <> :excluded
            """)
    BigDecimal sumPaid(@Param("transactionId") UUID transactionId, @Param("excluded") RecordStatus excluded);
}
