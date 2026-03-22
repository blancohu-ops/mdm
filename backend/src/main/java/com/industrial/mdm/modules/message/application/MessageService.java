package com.industrial.mdm.modules.message.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.message.domain.MessageStatus;
import com.industrial.mdm.modules.message.domain.MessageType;
import com.industrial.mdm.modules.message.dto.EnterpriseMessageListResponse;
import com.industrial.mdm.modules.message.dto.MessageResponse;
import com.industrial.mdm.modules.message.repository.MessageEntity;
import com.industrial.mdm.modules.message.repository.MessageRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public EnterpriseMessageListResponse listEnterpriseMessages(
            AuthenticatedUser currentUser, String type, String status) {
        UserEntity user = loadEnterpriseUser(currentUser);
        List<MessageEntity> items =
                messageRepository.findByRecipientUserIdOrderBySentAtDesc(user.getId()).stream()
                        .filter(item -> matchesType(item, type))
                        .filter(item -> matchesStatus(item, status))
                        .toList();
        long unreadTotal =
                messageRepository.countByRecipientUserIdAndStatus(user.getId(), MessageStatus.UNREAD);
        return new EnterpriseMessageListResponse(
                items.stream().map(this::toResponse).toList(), items.size(), unreadTotal);
    }

    @Transactional
    public MessageResponse markRead(AuthenticatedUser currentUser, UUID messageId) {
        UserEntity user = loadEnterpriseUser(currentUser);
        MessageEntity entity =
                messageRepository
                        .findById(messageId)
                        .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "message not found"));
        if (!entity.getRecipientUserId().equals(user.getId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "message does not belong to current account");
        }
        if (entity.getStatus() == MessageStatus.UNREAD) {
            entity.setStatus(MessageStatus.READ);
            entity.setReadAt(OffsetDateTime.now());
            entity = messageRepository.save(entity);
        }
        return toResponse(entity);
    }

    @Transactional
    public Map<String, Long> markAllRead(AuthenticatedUser currentUser) {
        UserEntity user = loadEnterpriseUser(currentUser);
        List<MessageEntity> items = messageRepository.findByRecipientUserIdOrderBySentAtDesc(user.getId());
        long changed = 0;
        for (MessageEntity item : items) {
            if (item.getStatus() == MessageStatus.UNREAD) {
                item.setStatus(MessageStatus.READ);
                item.setReadAt(OffsetDateTime.now());
                changed++;
            }
        }
        messageRepository.saveAll(items);
        return Map.of("markedCount", changed);
    }

    @Transactional
    public void sendToEnterpriseUsers(
            UUID enterpriseId,
            MessageType type,
            String title,
            String summary,
            String content,
            String relatedResourceType,
            UUID relatedResourceId) {
        List<UserEntity> recipients = userRepository.findByEnterpriseId(enterpriseId);
        for (UserEntity recipient : recipients) {
            MessageEntity entity = new MessageEntity();
            entity.setRecipientUserId(recipient.getId());
            entity.setEnterpriseId(enterpriseId);
            entity.setType(type);
            entity.setStatus(MessageStatus.UNREAD);
            entity.setTitle(title);
            entity.setSummary(summary);
            entity.setContent(content);
            entity.setRelatedResourceType(relatedResourceType);
            entity.setRelatedResourceId(relatedResourceId);
            entity.setSentAt(OffsetDateTime.now());
            messageRepository.save(entity);
        }
    }

    private MessageResponse toResponse(MessageEntity entity) {
        return new MessageResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getType().getCode(),
                entity.getSummary(),
                entity.getContent(),
                entity.getStatus().getCode(),
                entity.getSentAt(),
                entity.getRelatedResourceType(),
                entity.getRelatedResourceId());
    }

    private UserEntity loadEnterpriseUser(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.role() != UserRole.ENTERPRISE_OWNER) {
            throw new BizException(ErrorCode.FORBIDDEN, "current account cannot read enterprise messages");
        }
        return userRepository
                .findById(currentUser.userId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "current user not found"));
    }

    private boolean matchesType(MessageEntity entity, String type) {
        return type == null
                || type.isBlank()
                || "all".equalsIgnoreCase(type)
                || entity.getType().getCode().equalsIgnoreCase(type);
    }

    private boolean matchesStatus(MessageEntity entity, String status) {
        return status == null
                || status.isBlank()
                || "all".equalsIgnoreCase(status)
                || entity.getStatus().getCode().equalsIgnoreCase(status);
    }
}
