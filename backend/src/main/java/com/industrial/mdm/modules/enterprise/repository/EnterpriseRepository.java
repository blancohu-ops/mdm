package com.industrial.mdm.modules.enterprise.repository;

import com.industrial.mdm.modules.enterprise.domain.EnterpriseStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnterpriseRepository extends JpaRepository<EnterpriseEntity, UUID> {

    Page<EnterpriseEntity> findByStatusIn(List<EnterpriseStatus> statuses, Pageable pageable);

    Page<EnterpriseEntity> findByNameContainingIgnoreCaseAndStatusIn(
            String keyword, List<EnterpriseStatus> statuses, Pageable pageable);
}
