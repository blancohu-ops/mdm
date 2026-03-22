package com.industrial.mdm.modules.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findFirstByAccountIgnoreCaseOrPhoneOrEmailIgnoreCase(
            String account, String phone, String email);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, UUID id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    boolean existsByAccountIgnoreCase(String account);

    Optional<UserEntity> findByPhone(String phone);

    List<UserEntity> findByEnterpriseId(UUID enterpriseId);
}
