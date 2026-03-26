package com.industrial.mdm.modules.serviceFulfillment.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.iam.application.AuthorizationService;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.message.application.MessageService;
import com.industrial.mdm.modules.message.domain.MessageType;
import com.industrial.mdm.modules.serviceCatalog.application.ServiceCatalogService;
import com.industrial.mdm.modules.serviceCatalog.dto.ServiceSummaryResponse;
import com.industrial.mdm.modules.serviceFulfillment.domain.FulfillmentStatus;
import com.industrial.mdm.modules.serviceFulfillment.dto.DeliveryArtifactCreateRequest;
import com.industrial.mdm.modules.serviceFulfillment.dto.DeliveryArtifactResponse;
import com.industrial.mdm.modules.serviceFulfillment.dto.FulfillmentResponse;
import com.industrial.mdm.modules.serviceFulfillment.dto.FulfillmentUpdateRequest;
import com.industrial.mdm.modules.serviceFulfillment.dto.FulfillmentWorkspaceItemResponse;
import com.industrial.mdm.modules.serviceFulfillment.dto.FulfillmentWorkspaceResponse;
import com.industrial.mdm.modules.serviceFulfillment.repository.DeliveryArtifactEntity;
import com.industrial.mdm.modules.serviceFulfillment.repository.DeliveryArtifactRepository;
import com.industrial.mdm.modules.serviceFulfillment.repository.ServiceFulfillmentEntity;
import com.industrial.mdm.modules.serviceFulfillment.repository.ServiceFulfillmentRepository;
import com.industrial.mdm.modules.serviceOrder.application.ServiceOrderService;
import com.industrial.mdm.modules.serviceOrder.repository.ServiceOrderEntity;
import com.industrial.mdm.modules.serviceOrder.repository.ServiceOrderRepository;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderProfileRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceFulfillmentService {

    private final AuthorizationService authorizationService;
    private final ServiceFulfillmentRepository serviceFulfillmentRepository;
    private final DeliveryArtifactRepository deliveryArtifactRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final ServiceOrderService serviceOrderService;
    private final ServiceCatalogService serviceCatalogService;
    private final ServiceProviderProfileRepository serviceProviderProfileRepository;
    private final MessageService messageService;

    public ServiceFulfillmentService(
            AuthorizationService authorizationService,
            ServiceFulfillmentRepository serviceFulfillmentRepository,
            DeliveryArtifactRepository deliveryArtifactRepository,
            ServiceOrderRepository serviceOrderRepository,
            ServiceOrderService serviceOrderService,
            ServiceCatalogService serviceCatalogService,
            ServiceProviderProfileRepository serviceProviderProfileRepository,
            MessageService messageService) {
        this.authorizationService = authorizationService;
        this.serviceFulfillmentRepository = serviceFulfillmentRepository;
        this.deliveryArtifactRepository = deliveryArtifactRepository;
        this.serviceOrderRepository = serviceOrderRepository;
        this.serviceOrderService = serviceOrderService;
        this.serviceCatalogService = serviceCatalogService;
        this.serviceProviderProfileRepository = serviceProviderProfileRepository;
        this.messageService = messageService;
    }

    @Transactional(readOnly = true)
    public FulfillmentWorkspaceResponse listEnterpriseDeliveries(AuthenticatedUser currentUser) {
        UUID enterpriseId =
                authorizationService.assertCurrentEnterprisePermission(
                        currentUser,
                        PermissionCode.ENTERPRISE_DELIVERY_READ,
                        "current account cannot read enterprise deliveries");
        List<UUID> orderIds =
                serviceOrderRepository.findByEnterpriseIdOrderByCreatedAtDesc(enterpriseId).stream()
                        .map(ServiceOrderEntity::getId)
                        .toList();
        List<FulfillmentWorkspaceItemResponse> items =
                serviceFulfillmentRepository.findAllByOrderByUpdatedAtDesc().stream()
                        .filter(item -> orderIds.contains(item.getServiceOrderId()))
                        .map(this::toWorkspaceItem)
                        .toList();
        return new FulfillmentWorkspaceResponse(items, items.size());
    }

    @Transactional(readOnly = true)
    public FulfillmentWorkspaceResponse listAdminFulfillment(AuthenticatedUser currentUser) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ADMIN_FULFILLMENT_LIST,
                "current account cannot read fulfillment workspace");
        List<FulfillmentWorkspaceItemResponse> items =
                serviceFulfillmentRepository.findAllByOrderByUpdatedAtDesc().stream()
                        .map(this::toWorkspaceItem)
                        .toList();
        return new FulfillmentWorkspaceResponse(items, items.size());
    }

    @Transactional(readOnly = true)
    public FulfillmentWorkspaceResponse listProviderFulfillment(AuthenticatedUser currentUser) {
        UUID providerId =
                authorizationService.assertCurrentProviderPermission(
                        currentUser,
                        PermissionCode.PROVIDER_FULFILLMENT_READ,
                        "current account cannot read provider fulfillment");
        List<FulfillmentWorkspaceItemResponse> items =
                serviceFulfillmentRepository.findByServiceProviderIdOrderByUpdatedAtDesc(providerId).stream()
                        .map(this::toWorkspaceItem)
                        .toList();
        return new FulfillmentWorkspaceResponse(items, items.size());
    }

    @Transactional
    public FulfillmentResponse updateProviderFulfillment(
            AuthenticatedUser currentUser, UUID fulfillmentId, FulfillmentUpdateRequest request) {
        UUID providerId =
                authorizationService.assertCurrentProviderPermission(
                        currentUser,
                        PermissionCode.PROVIDER_FULFILLMENT_UPDATE,
                        "current account cannot update provider fulfillment");
        ServiceFulfillmentEntity fulfillment = loadFulfillment(fulfillmentId);
        if (!providerId.equals(fulfillment.getServiceProviderId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "fulfillment does not belong to current provider");
        }
        applyUpdate(fulfillment, request, currentUser.userId());
        if (fulfillment.getStatus() == FulfillmentStatus.SUBMITTED) {
            ServiceOrderEntity order = serviceOrderService.loadOrder(fulfillment.getServiceOrderId());
            serviceOrderService.refreshLatestFulfillmentAt(order);
            messageService.sendToEnterpriseUsers(
                    order.getEnterpriseId(),
                    MessageType.REVIEW,
                    "服务履约有新进展",
                    "服务商已提交新的履约节点，请查看交付进度。",
                    "订单号 " + order.getOrderNo() + " 的履约节点已更新。",
                    "service_order",
                    order.getId());
        }
        return toResponse(fulfillment);
    }

    @Transactional
    public FulfillmentResponse updateAdminFulfillment(
            AuthenticatedUser currentUser, UUID fulfillmentId, FulfillmentUpdateRequest request) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ADMIN_FULFILLMENT_UPDATE,
                "current account cannot update fulfillment");
        ServiceFulfillmentEntity fulfillment = loadFulfillment(fulfillmentId);
        applyUpdate(fulfillment, request, currentUser.userId());
        ServiceOrderEntity order = serviceOrderService.loadOrder(fulfillment.getServiceOrderId());
        serviceOrderService.refreshLatestFulfillmentAt(order);
        if (fulfillment.getStatus() == FulfillmentStatus.ACCEPTED) {
            messageService.sendToEnterpriseUsers(
                    order.getEnterpriseId(),
                    MessageType.SYSTEM,
                    "服务履约节点已验收",
                    "平台已确认服务商提交的履约节点，订单继续推进。",
                    "订单号 " + order.getOrderNo() + " 的履约节点“" + fulfillment.getMilestoneName() + "”已验收。",
                    "service_order",
                    order.getId());
            serviceOrderService.markDeliveredIfReady(order);
        }
        return toResponse(fulfillment);
    }

    @Transactional
    public DeliveryArtifactResponse addProviderArtifact(
            AuthenticatedUser currentUser, UUID orderId, DeliveryArtifactCreateRequest request) {
        UUID providerId =
                authorizationService.assertCurrentProviderPermission(
                        currentUser,
                        PermissionCode.PROVIDER_FULFILLMENT_UPDATE,
                        "current account cannot upload provider delivery artifacts");
        ServiceOrderEntity order = serviceOrderService.loadOrder(orderId);
        if (!providerId.equals(order.getServiceProviderId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "service order does not belong to current provider");
        }
        DeliveryArtifactEntity artifact = new DeliveryArtifactEntity();
        artifact.setServiceOrderId(orderId);
        artifact.setFileName(normalizeRequired(request.fileName(), "artifact file name is required"));
        artifact.setFileUrl(normalizeRequired(request.fileUrl(), "artifact file url is required"));
        artifact.setArtifactType(normalizeRequired(request.artifactType(), "artifact type is required"));
        artifact.setNote(normalizeOptional(request.note()));
        artifact.setVisibleToEnterprise(request.visibleToEnterprise() == null || request.visibleToEnterprise());
        artifact = deliveryArtifactRepository.save(artifact);
        if (artifact.isVisibleToEnterprise()) {
            messageService.sendToEnterpriseUsers(
                    order.getEnterpriseId(),
                    MessageType.SYSTEM,
                    "服务订单新增交付物",
                    "服务商已上传新的交付文件，请及时查看。",
                    "订单号 " + order.getOrderNo() + " 新增交付物“" + artifact.getFileName() + "”，请前往订单详情查看。",
                    "service_order",
                    order.getId());
        }
        return toArtifactResponse(artifact);
    }

    public List<DeliveryArtifactResponse> getArtifacts(UUID orderId) {
        return deliveryArtifactRepository.findByServiceOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(this::toArtifactResponse)
                .toList();
    }

    public List<FulfillmentResponse> getFulfillments(UUID orderId) {
        return serviceFulfillmentRepository.findByServiceOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void applyUpdate(
            ServiceFulfillmentEntity fulfillment, FulfillmentUpdateRequest request, UUID userId) {
        fulfillment.setStatus(parseStatus(request.status()));
        fulfillment.setDetail(normalizeOptional(request.detail()));
        fulfillment.setUpdatedByUserId(userId);
        if (fulfillment.getStatus() == FulfillmentStatus.ACCEPTED) {
            fulfillment.setCompletedAt(OffsetDateTime.now());
        }
        serviceFulfillmentRepository.save(fulfillment);
    }

    private ServiceFulfillmentEntity loadFulfillment(UUID fulfillmentId) {
        if (fulfillmentId == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "fulfillment id is required");
        }
        return serviceFulfillmentRepository.findById(fulfillmentId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "fulfillment not found"));
    }

    private FulfillmentWorkspaceItemResponse toWorkspaceItem(ServiceFulfillmentEntity item) {
        ServiceOrderEntity order = serviceOrderService.loadOrder(item.getServiceOrderId());
        ServiceSummaryResponse service =
                serviceCatalogService.toSummary(serviceCatalogService.loadService(order.getServiceId()));
        String providerName =
                order.getServiceProviderId() == null
                        ? "平台自营"
                        : serviceProviderProfileRepository
                                .findTopByServiceProviderIdOrderByVersionNoDesc(order.getServiceProviderId())
                                .map(profile -> profile.getCompanyName())
                                .orElse("服务商");
        return new FulfillmentWorkspaceItemResponse(
                item.getId(),
                order.getId(),
                order.getOrderNo(),
                service.title(),
                providerName,
                item.getMilestoneCode(),
                item.getMilestoneName(),
                item.getStatus().getCode(),
                item.getDetail(),
                item.getDueAt(),
                item.getCompletedAt());
    }

    private FulfillmentResponse toResponse(ServiceFulfillmentEntity item) {
        return new FulfillmentResponse(
                item.getId(),
                item.getMilestoneCode(),
                item.getMilestoneName(),
                item.getStatus().getCode(),
                item.getDetail(),
                item.getDueAt(),
                item.getCompletedAt());
    }

    private DeliveryArtifactResponse toArtifactResponse(DeliveryArtifactEntity item) {
        return new DeliveryArtifactResponse(
                item.getId(),
                item.getFileName(),
                item.getFileUrl(),
                item.getArtifactType(),
                item.getNote(),
                item.isVisibleToEnterprise());
    }

    private FulfillmentStatus parseStatus(String value) {
        String normalized = normalizeRequired(value, "fulfillment status is required");
        return switch (normalized) {
            case "pending" -> FulfillmentStatus.PENDING;
            case "in_progress" -> FulfillmentStatus.IN_PROGRESS;
            case "submitted" -> FulfillmentStatus.SUBMITTED;
            case "accepted" -> FulfillmentStatus.ACCEPTED;
            default -> throw new BizException(ErrorCode.INVALID_REQUEST, "unsupported fulfillment status");
        };
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(ErrorCode.INVALID_REQUEST, message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
