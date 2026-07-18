package com.teleconnect.billing_service.repository;

import com.teleconnect.billing_service.entity.Invoice;
import com.teleconnect.billing_service.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByAccountId(Long accountId);

    List<Invoice> findByStatus(InvoiceStatus status);

    Optional<Invoice> findByAccountIdAndCycleId(Long accountId, Long cycleId);

    List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDate date);

    List<Invoice> findByAccountIdAndStatus(Long accountId, InvoiceStatus status);

    List<Invoice> findByAccountIdAndDueDateBetween(Long accountId, LocalDate fromDate, LocalDate toDate);

    List<Invoice> findByAccountIdAndStatusAndDueDateBetween(Long accountId, InvoiceStatus status,
                                                            LocalDate fromDate, LocalDate toDate);

    List<Invoice> findByDueDateBetween(LocalDate fromDate, LocalDate toDate);

    List<Invoice> findByStatusAndDueDateBetween(InvoiceStatus status, LocalDate fromDate, LocalDate toDate);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.dueDate BETWEEN :fromDate AND :toDate")
    BigDecimal sumTotalAmountByDueDateBetween(LocalDate fromDate, LocalDate toDate);

    @Query("SELECT COALESCE(SUM(i.paidAmount), 0) FROM Invoice i WHERE i.status = :status AND i.dueDate BETWEEN :fromDate AND :toDate")
    BigDecimal sumPaidAmountByStatusAndDueDateBetween(InvoiceStatus status, LocalDate fromDate, LocalDate toDate);
}
