package com.industrial.mdm.modules.auth.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginLogRepository extends JpaRepository<LoginLogEntity, UUID> {}
