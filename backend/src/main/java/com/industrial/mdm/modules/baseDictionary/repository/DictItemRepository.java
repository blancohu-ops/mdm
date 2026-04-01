package com.industrial.mdm.modules.baseDictionary.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DictItemRepository extends JpaRepository<DictItemEntity, UUID> {

    List<DictItemEntity> findByDictTypeCodeOrderBySortOrderAscNameAsc(String dictTypeCode);

    List<DictItemEntity> findByDictTypeCodeAndEnabledTrueOrderBySortOrderAscNameAsc(
            String dictTypeCode);

    Optional<DictItemEntity> findByIdAndDictTypeCode(UUID id, String dictTypeCode);

    Optional<DictItemEntity> findByDictTypeCodeAndCodeIgnoreCase(String dictTypeCode, String code);
}
