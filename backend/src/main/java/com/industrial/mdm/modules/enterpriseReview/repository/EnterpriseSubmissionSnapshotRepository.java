package com.industrial.mdm.modules.enterpriseReview.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnterpriseSubmissionSnapshotRepository
        extends JpaRepository<EnterpriseSubmissionSnapshotEntity, UUID> {}
