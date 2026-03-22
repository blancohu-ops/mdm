package com.industrial.mdm.modules.productReview.repository;

import com.industrial.mdm.modules.productReview.domain.ProductSubmissionStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSubmissionRecordRepository
        extends JpaRepository<ProductSubmissionRecordEntity, UUID> {

    Optional<ProductSubmissionRecordEntity> findTopByProductIdOrderBySubmittedAtDesc(UUID productId);

    Optional<ProductSubmissionRecordEntity> findTopByProductIdAndStatusOrderBySubmittedAtDesc(
            UUID productId, ProductSubmissionStatus status);
}
