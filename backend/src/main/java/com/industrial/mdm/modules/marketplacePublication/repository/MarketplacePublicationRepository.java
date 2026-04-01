package com.industrial.mdm.modules.marketplacePublication.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketplacePublicationRepository extends JpaRepository<MarketplacePublicationEntity, UUID> {

    Optional<MarketplacePublicationEntity> findByServiceOrderId(UUID serviceOrderId);

    List<MarketplacePublicationEntity> findByEnterpriseIdOrderByCreatedAtDesc(UUID enterpriseId);

    List<MarketplacePublicationEntity> findAllByOrderByCreatedAtDesc();

    List<MarketplacePublicationEntity> findByProductIdInOrderByCreatedAtDesc(List<UUID> productIds);

    List<MarketplacePublicationEntity> findByProductIdOrderByCreatedAtDesc(UUID productId);

    boolean existsByServiceId(UUID serviceId);

    boolean existsByOfferId(UUID offerId);
}
