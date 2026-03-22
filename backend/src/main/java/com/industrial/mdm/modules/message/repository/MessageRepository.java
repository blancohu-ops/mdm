package com.industrial.mdm.modules.message.repository;

import com.industrial.mdm.modules.message.domain.MessageStatus;
import com.industrial.mdm.modules.message.domain.MessageType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {

    List<MessageEntity> findByRecipientUserIdOrderBySentAtDesc(UUID recipientUserId);

    long countByRecipientUserIdAndStatus(UUID recipientUserId, MessageStatus status);

    List<MessageEntity> findByRecipientUserIdAndTypeOrderBySentAtDesc(UUID recipientUserId, MessageType type);
}
