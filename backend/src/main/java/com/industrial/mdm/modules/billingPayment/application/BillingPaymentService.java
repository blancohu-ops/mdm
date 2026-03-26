package com.industrial.mdm.modules.billingPayment.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.billingPayment.domain.PaymentRecordStatus;
import com.industrial.mdm.modules.billingPayment.dto.PaymentDecisionRequest;
import com.industrial.mdm.modules.billingPayment.dto.PaymentRecordListResponse;
import com.industrial.mdm.modules.billingPayment.dto.PaymentRecordResponse;
import com.industrial.mdm.modules.billingPayment.dto.SubmitPaymentRequest;
import com.industrial.mdm.modules.billingPayment.repository.PaymentRecordEntity;
import com.industrial.mdm.modules.billingPayment.repository.PaymentRecordRepository;
import com.industrial.mdm.modules.iam.application.AuthorizationService;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.marketplacePublication.application.MarketplacePublicationService;
import com.industrial.mdm.modules.marketplacePublication.dto.MarketplacePublicationResponse;
import com.industrial.mdm.modules.message.application.MessageService;
import com.industrial.mdm.modules.message.domain.MessageType;
import com.industrial.mdm.modules.serviceCatalog.application.ServiceCatalogService;
import com.industrial.mdm.modules.serviceOrder.application.ServiceOrderService;
import com.industrial.mdm.modules.serviceOrder.domain.ServiceOrderPaymentStatus;
import com.industrial.mdm.modules.serviceOrder.domain.ServiceOrderStatus;
import com.industrial.mdm.modules.serviceOrder.repository.ServiceOrderEntity;
import com.industrial.mdm.modules.serviceOrder.repository.ServiceOrderRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingPaymentService {

    private final AuthorizationService authorizationService;
    private final PaymentRecordRepository paymentRecordRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final ServiceOrderService serviceOrderService;
    private final ServiceCatalogService serviceCatalogService;
    private final MarketplacePublicationService marketplacePublicationService;
    private final MessageService messageService;

    public BillingPaymentService(
            AuthorizationService authorizationService,
            PaymentRecordRepository paymentRecordRepository,
            ServiceOrderRepository serviceOrderRepository,
            ServiceOrderService serviceOrderService,
            ServiceCatalogService serviceCatalogService,
            MarketplacePublicationService marketplacePublicationService,
            MessageService messageService) {
        this.authorizationService = authorizationService;
        this.paymentRecordRepository = paymentRecordRepository;
        this.serviceOrderRepository = serviceOrderRepository;
        this.serviceOrderService = serviceOrderService;
        this.serviceCatalogService = serviceCatalogService;
        this.marketplacePublicationService = marketplacePublicationService;
        this.messageService = messageService;
    }

    @Transactional(readOnly = true)
    public PaymentRecordListResponse listEnterprisePayments(AuthenticatedUser currentUser) {
        UUID enterpriseId =
                authorizationService.assertCurrentEnterprisePermission(
                        currentUser,
                        PermissionCode.ENTERPRISE_PAYMENT_READ,
                        "current account cannot read enterprise payments");
        List<UUID> orderIds =
                serviceOrderRepository.findByEnterpriseIdOrderByCreatedAtDesc(enterpriseId).stream()
                        .map(ServiceOrderEntity::getId)
                        .toList();
        List<PaymentRecordResponse> items =
                paymentRecordRepository.findByServiceOrderIdInOrderByCreatedAtDesc(orderIds).stream()
                        .map(this::toResponse)
                        .toList();
        return new PaymentRecordListResponse(items, items.size());
    }

    @Transactional
    public PaymentRecordResponse submitEnterprisePayment(
            AuthenticatedUser currentUser, UUID paymentId, SubmitPaymentRequest request) {
        UUID enterpriseId =
                authorizationService.assertCurrentEnterprisePermission(
                        currentUser,
                        PermissionCode.ENTERPRISE_PAYMENT_SUBMIT,
                        "current account cannot submit enterprise payment");
        PaymentRecordEntity payment = loadPayment(paymentId);
        ServiceOrderEntity order = serviceOrderService.loadOrder(payment.getServiceOrderId());
        if (!enterpriseId.equals(order.getEnterpriseId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "payment does not belong to current enterprise");
        }
        payment.setEvidenceFileUrl(normalizeOptional(request.evidenceFileUrl()));
        payment.setNote(normalizeOptional(request.note()));
        payment.setStatus(PaymentRecordStatus.SUBMITTED);
        payment.setSubmittedAt(OffsetDateTime.now());
        payment = paymentRecordRepository.save(payment);
        serviceOrderService.updatePaymentState(
                order,
                ServiceOrderPaymentStatus.SUBMITTED,
                ServiceOrderStatus.PENDING_PAYMENT);
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentRecordListResponse listAdminPayments(AuthenticatedUser currentUser) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ADMIN_PAYMENT_LIST,
                "current account cannot read payments");
        List<PaymentRecordResponse> items =
                paymentRecordRepository.findAllByOrderByCreatedAtDesc().stream()
                        .map(this::toResponse)
                        .toList();
        return new PaymentRecordListResponse(items, items.size());
    }

    @Transactional
    public PaymentRecordResponse confirmPayment(
            AuthenticatedUser currentUser, UUID paymentId, PaymentDecisionRequest request) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ADMIN_PAYMENT_CONFIRM,
                "current account cannot confirm payments");
        PaymentRecordEntity payment = loadPayment(paymentId);
        ServiceOrderEntity order = serviceOrderService.loadOrder(payment.getServiceOrderId());
        payment.setStatus(PaymentRecordStatus.CONFIRMED);
        payment.setConfirmedBy(currentUser.userId());
        payment.setConfirmedAt(OffsetDateTime.now());
        payment.setConfirmedNote(normalizeOptional(request.note()));
        payment = paymentRecordRepository.save(payment);
        serviceOrderService.updatePaymentState(
                order,
                ServiceOrderPaymentStatus.CONFIRMED,
                ServiceOrderStatus.IN_PROGRESS);
        MarketplacePublicationResponse publication =
                marketplacePublicationService.activatePublicationForConfirmedPayment(
                        order,
                        payment,
                        request.note());
        messageService.sendToEnterpriseUsers(
                order.getEnterpriseId(),
                MessageType.REVIEW,
                "服务订单付款已确认",
                "平台已确认您的服务订单付款，订单已进入履约阶段。",
                "订单号 " + order.getOrderNo() + " 已完成付款确认，请留意后续履约进度和交付物。",
                "service_order",
                order.getId());
        if (publication != null) {
            String targetSummary =
                    publication.productName() == null
                            ? "企业级展示权益"
                            : "产品推广权益：" + publication.productName();
            String expirySummary =
                    publication.expiresAt() == null
                            ? "当前权益未设置到期时间。"
                            : "权益到期时间：" + publication.expiresAt() + "。";
            messageService.sendToEnterpriseUsers(
                    order.getEnterpriseId(),
                    MessageType.SYSTEM,
                    "市场推广权益已生效",
                    "支付确认完成后，平台已为您的推广服务激活对应权益。",
                    "订单号 " + order.getOrderNo() + " 对应的" + targetSummary + "已生效。" + expirySummary,
                    "service_order",
                    order.getId());
        }
        return toResponse(payment);
    }

    @Transactional
    public PaymentRecordResponse rejectPayment(
            AuthenticatedUser currentUser, UUID paymentId, PaymentDecisionRequest request) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ADMIN_PAYMENT_REJECT,
                "current account cannot reject payments");
        PaymentRecordEntity payment = loadPayment(paymentId);
        ServiceOrderEntity order = serviceOrderService.loadOrder(payment.getServiceOrderId());
        payment.setStatus(PaymentRecordStatus.REJECTED);
        payment.setConfirmedBy(currentUser.userId());
        payment.setConfirmedAt(OffsetDateTime.now());
        payment.setConfirmedNote(normalizeOptional(request.note()));
        payment = paymentRecordRepository.save(payment);
        serviceOrderService.updatePaymentState(
                order,
                ServiceOrderPaymentStatus.REJECTED,
                ServiceOrderStatus.PENDING_PAYMENT);
        messageService.sendToEnterpriseUsers(
                order.getEnterpriseId(),
                MessageType.REVIEW,
                "服务订单付款被退回",
                "平台已退回您的付款提交，请补充付款凭证后重新提交。",
                "订单号 " + order.getOrderNo() + " 的付款提交被退回，请查看原因后重新提交。",
                "service_order",
                order.getId());
        return toResponse(payment);
    }

    public PaymentRecordEntity loadPayment(UUID paymentId) {
        if (paymentId == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "payment id is required");
        }
        return paymentRecordRepository.findById(paymentId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "payment record not found"));
    }

    private PaymentRecordResponse toResponse(PaymentRecordEntity payment) {
        ServiceOrderEntity order = serviceOrderService.loadOrder(payment.getServiceOrderId());
        String serviceTitle = serviceCatalogService.loadService(order.getServiceId()).getTitle();
        return new PaymentRecordResponse(
                payment.getId(),
                payment.getServiceOrderId(),
                order.getOrderNo(),
                serviceTitle,
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod().getCode(),
                payment.getStatus().getCode(),
                payment.getEvidenceFileUrl(),
                payment.getNote(),
                payment.getSubmittedAt(),
                payment.getConfirmedAt(),
                payment.getConfirmedNote());
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
