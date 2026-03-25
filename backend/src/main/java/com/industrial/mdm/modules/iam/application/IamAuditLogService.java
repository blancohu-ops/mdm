package com.industrial.mdm.modules.iam.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.iam.domain.audit.IamAuditAction;
import com.industrial.mdm.modules.iam.repository.IamAuditLogEntity;
import com.industrial.mdm.modules.iam.repository.IamAuditLogRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IamAuditLogService {

    private final IamAuditLogRepository iamAuditLogRepository;
    private final ObjectMapper objectMapper;

    public IamAuditLogService(
            IamAuditLogRepository iamAuditLogRepository, ObjectMapper objectMapper) {
        this.iamAuditLogRepository = iamAuditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(
            AuthenticatedUser actor,
            IamAuditAction action,
            String targetType,
            UUID targetId,
            UUID targetUserId,
            UUID targetEnterpriseId,
            String summary,
            Object detail) {
        IamAuditLogEntity entity = new IamAuditLogEntity();
        if (actor != null) {
            entity.setActorUserId(actor.userId());
            entity.setActorRole(actor.role().getCode());
            entity.setActorEnterpriseId(actor.enterpriseId());
        }
        entity.setActionCode(action.getCode());
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setTargetUserId(targetUserId);
        entity.setTargetEnterpriseId(targetEnterpriseId);
        entity.setSummary(summary);
        entity.setDetailJson(serializeDetail(detail));
        entity.setRequestId(MDC.get(RequestIdFilter.REQUEST_ID));
        iamAuditLogRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<IamAuditLogEntity> listRecent() {
        return iamAuditLogRepository.findTop50ByOrderByCreatedAtDesc();
    }

    private String serializeDetail(Object detail) {
        if (detail == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize iam audit detail", exception);
        }
    }
}
