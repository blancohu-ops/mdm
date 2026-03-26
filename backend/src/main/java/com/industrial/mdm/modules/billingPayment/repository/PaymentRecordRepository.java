package com.industrial.mdm.modules.billingPayment.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecordEntity, UUID> {

    List<PaymentRecordEntity> findAllByOrderByCreatedAtDesc();

    List<PaymentRecordEntity> findByServiceOrderIdInOrderByCreatedAtDesc(List<UUID> serviceOrderIds);

    Optional<PaymentRecordEntity> findTopByServiceOrderIdOrderByCreatedAtDesc(UUID serviceOrderId);
}

