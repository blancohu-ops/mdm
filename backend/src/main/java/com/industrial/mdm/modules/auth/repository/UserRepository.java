package com.industrial.mdm.modules.auth.repository;

import com.industrial.mdm.common.security.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {

    Optional<UserEntity> findFirstByAccountIgnoreCaseOrPhoneOrEmailIgnoreCase(
            String account, String phone, String email);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, UUID id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    boolean existsByAccountIgnoreCase(String account);

    boolean existsByAccountIgnoreCaseAndIdNot(String account, UUID id);

    Optional<UserEntity> findByAccountIgnoreCase(String account);

    Optional<UserEntity> findByPhone(String phone);

    List<UserEntity> findByEnterpriseId(UUID enterpriseId);

    List<UserEntity> findByServiceProviderId(UUID serviceProviderId);

    Optional<UserEntity> findFirstByEnterpriseIdAndRole(UUID enterpriseId, UserRole role);

    Optional<UserEntity> findFirstByServiceProviderIdAndRole(UUID serviceProviderId, UserRole role);
}
