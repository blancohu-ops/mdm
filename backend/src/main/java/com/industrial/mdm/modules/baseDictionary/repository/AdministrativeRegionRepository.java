package com.industrial.mdm.modules.baseDictionary.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministrativeRegionRepository
        extends JpaRepository<AdministrativeRegionEntity, UUID> {

    List<AdministrativeRegionEntity> findAllByOrderByLevelAscSortOrderAscNameAsc();

    List<AdministrativeRegionEntity> findByLevelOrderBySortOrderAscNameAsc(int level);

    List<AdministrativeRegionEntity> findByLevelAndEnabledTrueOrderBySortOrderAscNameAsc(int level);

    List<AdministrativeRegionEntity> findByParentCodeOrderBySortOrderAscNameAsc(String parentCode);

    List<AdministrativeRegionEntity> findByParentCodeAndEnabledTrueOrderBySortOrderAscNameAsc(
            String parentCode);

    Optional<AdministrativeRegionEntity> findByCode(String code);
}
