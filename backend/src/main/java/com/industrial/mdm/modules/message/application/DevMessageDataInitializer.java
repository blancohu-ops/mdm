package com.industrial.mdm.modules.message.application;

import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.message.domain.MessageStatus;
import com.industrial.mdm.modules.message.domain.MessageType;
import com.industrial.mdm.modules.message.repository.MessageEntity;
import com.industrial.mdm.modules.message.repository.MessageRepository;
import java.time.OffsetDateTime;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DevMessageDataInitializer {

    @Bean
    ApplicationRunner seedMessages(MessageRepository messageRepository, UserRepository userRepository) {
        return args -> {
            if (messageRepository.count() > 0) {
                return;
            }
            UserEntity enterpriseUser =
                    userRepository
                            .findFirstByAccountIgnoreCaseOrPhoneOrEmailIgnoreCase(
                                    "enterprise@example.com",
                                    "enterprise@example.com",
                                    "enterprise@example.com")
                            .orElse(null);
            if (enterpriseUser == null || enterpriseUser.getEnterpriseId() == null) {
                return;
            }

            messageRepository.save(createMessage(
                    enterpriseUser,
                    MessageType.SYSTEM,
                    MessageStatus.UNREAD,
                    "平台公告：工业类目与 HS Code 字典已更新",
                    "产品录入时请优先使用最新类目树与编码建议。",
                    "你可以在产品编辑页使用新的 HS Code 推荐结果，并结合类目树检查是否需要更新历史草稿。",
                    "category",
                    null,
                    OffsetDateTime.now().minusDays(2)));

            messageRepository.save(createMessage(
                    enterpriseUser,
                    MessageType.REVIEW,
                    MessageStatus.READ,
                    "产品审核提醒：液压挖掘机组件已驳回待修改",
                    "驳回原因会同步到产品详情与编辑页，请尽快补充清晰主图。",
                    "平台审核员已驳回该产品，请根据驳回原因修正后重新提交审核。",
                    "product",
                    null,
                    OffsetDateTime.now().minusDays(1)));
        };
    }

    private MessageEntity createMessage(
            UserEntity user,
            MessageType type,
            MessageStatus status,
            String title,
            String summary,
            String content,
            String relatedResourceType,
            java.util.UUID relatedResourceId,
            OffsetDateTime sentAt) {
        MessageEntity entity = new MessageEntity();
        entity.setRecipientUserId(user.getId());
        entity.setEnterpriseId(user.getEnterpriseId());
        entity.setType(type);
        entity.setStatus(status);
        entity.setTitle(title);
        entity.setSummary(summary);
        entity.setContent(content);
        entity.setRelatedResourceType(relatedResourceType);
        entity.setRelatedResourceId(relatedResourceId);
        entity.setSentAt(sentAt);
        if (status == MessageStatus.READ) {
            entity.setReadAt(sentAt.plusHours(3));
        }
        return entity;
    }
}
