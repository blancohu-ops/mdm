package com.industrial.mdm.modules.baseDictionary.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DictTypeRepository extends JpaRepository<DictTypeEntity, UUID> {

    List<DictTypeEntity> findAllByOrderByCreatedAtAscCodeAsc();

    Optional<DictTypeEntity> findByCode(String code);
}
