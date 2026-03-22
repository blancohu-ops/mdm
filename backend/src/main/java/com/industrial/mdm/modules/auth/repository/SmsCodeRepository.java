package com.industrial.mdm.modules.auth.repository;

import com.industrial.mdm.modules.auth.domain.SmsCodePurpose;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmsCodeRepository extends JpaRepository<SmsCodeEntity, UUID> {

    Optional<SmsCodeEntity> findTopByPhoneAndPurposeAndCodeAndUsedAtIsNullOrderByCreatedAtDesc(
            String phone, SmsCodePurpose purpose, String code);
}
