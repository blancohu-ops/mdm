package com.industrial.mdm.modules.product.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductProfileRepository extends JpaRepository<ProductProfileEntity, UUID> {

    Optional<ProductProfileEntity> findTopByProductIdOrderByVersionNoDesc(UUID productId);
}
