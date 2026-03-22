package com.industrial.mdm.modules.productReview.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSubmissionSnapshotRepository
        extends JpaRepository<ProductSubmissionSnapshotEntity, UUID> {}
