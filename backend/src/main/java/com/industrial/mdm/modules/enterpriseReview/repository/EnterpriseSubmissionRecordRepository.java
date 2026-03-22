package com.industrial.mdm.modules.enterpriseReview.repository;

import com.industrial.mdm.modules.enterpriseReview.domain.EnterpriseSubmissionStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnterpriseSubmissionRecordRepository
        extends JpaRepository<EnterpriseSubmissionRecordEntity, UUID> {

    Optional<EnterpriseSubmissionRecordEntity> findTopByEnterpriseIdOrderBySubmittedAtDesc(UUID enterpriseId);

    Optional<EnterpriseSubmissionRecordEntity>
            findTopByEnterpriseIdAndStatusOrderBySubmittedAtDesc(
                    UUID enterpriseId, EnterpriseSubmissionStatus status);
}
