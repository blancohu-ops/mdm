package com.industrial.mdm.modules.product.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    List<ProductEntity> findByEnterpriseId(UUID enterpriseId);

    long countByEnterpriseId(UUID enterpriseId);
}
