package com.industrial.mdm.modules.serviceOrder.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.billingPayment.domain.PaymentMethod;
import com.industrial.mdm.modules.billingPayment.domain.PaymentRecordStatus;
import com.industrial.mdm.modules.billingPayment.dto.PaymentRecordResponse;
import com.industrial.mdm.modules.billingPayment.repository.PaymentRecordEntity;
import com.industrial.mdm.modules.billingPayment.repository.PaymentRecordRepository;
import com.industrial.mdm.modules.iam.application.AuthorizationService;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.message.application.MessageService;
import com.industrial.mdm.modules.message.domain.MessageType;
import com.industrial.mdm.modules.product.repository.ProductEntity;
import com.industrial.mdm.modules.product.repository.ProductProfileEntity;
import com.industrial.mdm.modules.product.repository.ProductProfileRepository;
import com.industrial.mdm.modules.product.repository.ProductRepository;
import com.industrial.mdm.modules.serviceCatalog.application.ServiceCatalogService;
import com.industrial.mdm.modules.serviceCatalog.domain.ServiceTargetResourceType;
import com.industrial.mdm.modules.serviceCatalog.dto.ServiceSummaryResponse;
import com.industrial.mdm.modules.serviceCatalog.repository.ServiceEntity;
import com.industrial.mdm.modules.serviceCatalog.repository.ServiceOfferEntity;
import com.industrial.mdm.modules.serviceFulfillment.domain.FulfillmentStatus;
import com.industrial.mdm.modules.serviceFulfillment.dto.DeliveryArtifactResponse;
import com.industrial.mdm.modules.serviceFulfillment.dto.FulfillmentResponse;
import com.industrial.mdm.modules.serviceFulfillment.repository.DeliveryArtifactEntity;
import com.industrial.mdm.modules.serviceFulfillment.repository.DeliveryArtifactRepository;
import com.industrial.mdm.modules.serviceFulfillment.repository.ServiceFulfillmentEntity;
import com.industrial.mdm.modules.serviceFulfillment.repository.ServiceFulfillmentRepository;
import com.industrial.mdm.modules.serviceOrder.domain.ServiceOrderPaymentStatus;
import com.industrial.mdm.modules.serviceOrder.domain.ServiceOrderStatus;
import com.industrial.mdm.modules.serviceOrder.dto.AssignServiceOrderRequest;
import com.industrial.mdm.modules.serviceOrder.dto.CreateServiceOrderRequest;
import com.industrial.mdm.modules.serviceOrder.dto.ServiceOrderListResponse;
import com.industrial.mdm.modules.serviceOrder.dto.ServiceOrderResponse;
import com.industrial.mdm.modules.serviceOrder.repository.ServiceOrderEntity;
import com.industrial.mdm.modules.serviceOrder.repository.ServiceOrderRepository;
import com.industrial.mdm.modules.serviceProvider.domain.ServiceProviderStatus;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderEntity;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderProfileRepository;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderRepository;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceOrderService {

    private final AuthorizationService authorizationService;
    private final ServiceOrderRepository serviceOrderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final ServiceFulfillmentRepository serviceFulfillmentRepository;
    private final DeliveryArtifactRepository deliveryArtifactRepository;
    private final ProductRepository productRepository;
    private final ProductProfileRepository productProfileRepository;
    private final ServiceCatalogService serviceCatalogService;
    private final ServiceProviderRepository serviceProviderRepository;
    private final ServiceProviderProfileRepository serviceProviderProfileRepository;
    private final MessageService messageService;

    public ServiceOrderService(
            AuthorizationService authorizationService,
            ServiceOrderRepository serviceOrderRepository,
            PaymentRecordRepository paymentRecordRepository,
            ServiceFulfillmentRepository serviceFulfillmentRepository,
            DeliveryArtifactRepository deliveryArtifactRepository,
            ProductRepository productRepository,
            ProductProfileRepository productProfileRepository,
            ServiceCatalogService serviceCatalogService,
            ServiceProviderRepository serviceProviderRepository,
            ServiceProviderProfileRepository serviceProviderProfileRepository,
            MessageService messageService) {
        this.authorizationService = authorizationService;
        this.serviceOrderRepository = serviceOrderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.serviceFulfillmentRepository = serviceFulfillmentRepository;
        this.deliveryArtifactRepository = deliveryArtifactRepository;
        this.productRepository = productRepository;
        this.productProfileRepository = productProfileRepository;
        this.serviceCatalogService = serviceCatalogService;
        this.serviceProviderRepository = serviceProviderRepository;
        this.serviceProviderProfileRepository = serviceProviderProfileRepository;
        this.messageService = messageService;
    }

    @Transactional
    public ServiceOrderResponse createEnterpriseOrder(
            AuthenticatedUser currentUser, CreateServiceOrderRequest request) {
        UUID enterpriseId =
                authorizationService.assertCurrentEnterprisePermission(
                        currentUser,
                        PermissionCode.ENTERPRISE_SERVICE_ORDER_CREATE,
                        "current account cannot create service orders");
        ServiceEntity service = serviceCatalogService.loadPublishedService(request.serviceId());
        ServiceOfferEntity offer = serviceCatalogService.loadOffer(request.offerId());
        if (!service.getId().equals(offer.getServiceId()) || !offer.isEnabled()) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "service offer does not match service");
        }
        validateTargetResource(enterpriseId, offer, request.productId());

        ServiceOrderEntity order = new ServiceOrderEntity();
        order.setOrderNo(generateOrderNo());
        order.setEnterpriseId(enterpriseId);
        order.setProductId(request.productId());
        order.setServiceId(service.getId());
        order.setOfferId(offer.getId());
        order.setServiceProviderId(service.getServiceProviderId());
        order.setStatus(ServiceOrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(ServiceOrderPaymentStatus.PENDING_SUBMISSION);
        order.setAmount(offer.getPriceAmount());
        order.setCurrency(offer.getCurrency());
        order.setCustomerNote(normalizeOptional(request.customerNote()));
        order.setCreatedByUserId(currentUser.userId());
        order = serviceOrderRepository.save(order);

        PaymentRecordEntity payment = new PaymentRecordEntity();
        payment.setServiceOrderId(order.getId());
        payment.setAmount(order.getAmount());
        payment.setCurrency(order.getCurrency());
        payment.setPaymentMethod(PaymentMethod.OFFLINE_TRANSFER);
        payment.setStatus(PaymentRecordStatus.PENDING_SUBMISSION);
        paymentRecordRepository.save(payment);

        createDefaultFulfillment(order);
        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public ServiceOrderListResponse listEnterpriseOrders(AuthenticatedUser currentUser) {
        UUID enterpriseId =
                authorizationService.assertCurrentEnterprisePermission(
                        currentUser,
                        PermissionCode.ENTERPRISE_SERVICE_ORDER_READ,
                        "current account cannot read enterprise service orders");
        List<ServiceOrderResponse> items =
                serviceOrderRepository.findByEnterpriseIdOrderByCreatedAtDesc(enterpriseId).stream()
                        .map(this::toOrderResponse)
                        .toList();
        return new ServiceOrderListResponse(items, items.size());
    }

    @Transactional(readOnly = true)
    public ServiceOrderResponse getEnterpriseOrder(AuthenticatedUser currentUser, UUID orderId) {
        UUID enterpriseId =
                authorizationService.assertCurrentEnterprisePermission(
                        currentUser,
                        PermissionCode.ENTERPRISE_SERVICE_ORDER_READ,
                        "current account cannot read enterprise service orders");
        ServiceOrderEntity order = loadOrder(orderId);
        if (!enterpriseId.equals(order.getEnterpriseId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "service order does not belong to current enterprise");
        }
        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public ServiceOrderListResponse listAdminOrders(AuthenticatedUser currentUser) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ADMIN_SERVICE_ORDER_LIST,
                "current account cannot read service orders");
        List<ServiceOrderResponse> items =
                serviceOrderRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toOrderResponse).toList();
        return new ServiceOrderListResponse(items, items.size());
    }

    @Transactional
    public ServiceOrderResponse assignProvider(
            AuthenticatedUser currentUser, UUID orderId, AssignServiceOrderRequest request) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ADMIN_SERVICE_ORDER_ASSIGN,
                "current account cannot assign service orders");
        ServiceOrderEntity order = loadOrder(orderId);
        ServiceProviderEntity provider =
                serviceProviderRepository.findById(request.providerId())
                        .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "service provider not found"));
        if (provider.getStatus() != ServiceProviderStatus.ACTIVE) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "service provider is not active");
        }
        order.setServiceProviderId(provider.getId());
        serviceOrderRepository.save(order);
        List<ServiceFulfillmentEntity> fulfillments =
                serviceFulfillmentRepository.findByServiceOrderIdOrderByCreatedAtAsc(order.getId());
        for (ServiceFulfillmentEntity fulfillment : fulfillments) {
            fulfillment.setServiceProviderId(provider.getId());
        }
        serviceFulfillmentRepository.saveAll(fulfillments);
        String providerName = resolveProviderName(provider.getId());
        messageService.sendToEnterpriseUsers(
                order.getEnterpriseId(),
                MessageType.SYSTEM,
                "服务订单已分配服务商",
                "平台已为您的订单分配服务商，订单进入正式履约协作阶段。",
                "订单号 " + order.getOrderNo() + " 已分配给服务商“" + providerName + "”，请关注后续履约进度和交付物。",
                "service_order",
                order.getId());
        messageService.sendToProviderUsers(
                provider.getId(),
                MessageType.SYSTEM,
                "收到新的服务订单协作任务",
                "平台已向您分配新的服务订单，请尽快查看需求和履约节点。",
                "订单号 " + order.getOrderNo() + " 已分配到您的服务商工作台，请尽快确认需求并推进履约。",
                "service_order",
                order.getId());
        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public ServiceOrderListResponse listProviderOrders(AuthenticatedUser currentUser) {
        UUID providerId =
                authorizationService.assertCurrentProviderPermission(
                        currentUser,
                        PermissionCode.PROVIDER_ORDER_READ,
                        "current account cannot read provider orders");
        List<ServiceOrderResponse> items =
                serviceOrderRepository.findByServiceProviderIdOrderByCreatedAtDesc(providerId).stream()
                        .map(this::toOrderResponse)
                        .toList();
        return new ServiceOrderListResponse(items, items.size());
    }

    @Transactional(readOnly = true)
    public ServiceOrderResponse getProviderOrder(AuthenticatedUser currentUser, UUID orderId) {
        UUID providerId =
                authorizationService.assertCurrentProviderPermission(
                        currentUser,
                        PermissionCode.PROVIDER_ORDER_READ,
                        "current account cannot read provider orders");
        ServiceOrderEntity order = loadOrder(orderId);
        if (!providerId.equals(order.getServiceProviderId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "service order does not belong to current provider");
        }
        return toOrderResponse(order);
    }

    public ServiceOrderEntity loadOrder(UUID orderId) {
        if (orderId == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "service order id is required");
        }
        return serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "service order not found"));
    }

    @Transactional
    public void updatePaymentState(
            ServiceOrderEntity order,
            ServiceOrderPaymentStatus paymentStatus,
            ServiceOrderStatus orderStatus) {
        order.setPaymentStatus(paymentStatus);
        order.setStatus(orderStatus);
        serviceOrderRepository.save(order);
    }

    @Transactional
    public void refreshLatestFulfillmentAt(ServiceOrderEntity order) {
        order.setLatestFulfillmentAt(OffsetDateTime.now());
        serviceOrderRepository.save(order);
    }

    @Transactional
    public void markDeliveredIfReady(ServiceOrderEntity order) {
        List<ServiceFulfillmentEntity> items =
                serviceFulfillmentRepository.findByServiceOrderIdOrderByCreatedAtAsc(order.getId());
        boolean allAccepted = items.stream().allMatch(item -> item.getStatus() == FulfillmentStatus.ACCEPTED);
        if (allAccepted) {
            order.setStatus(ServiceOrderStatus.DELIVERED);
            order.setCompletedAt(OffsetDateTime.now());
            serviceOrderRepository.save(order);
        }
    }

    private void validateTargetResource(
            UUID enterpriseId, ServiceOfferEntity offer, UUID productId) {
        if (offer.getTargetResourceType() == ServiceTargetResourceType.PRODUCT) {
            if (productId == null) {
                throw new BizException(ErrorCode.INVALID_REQUEST, "product targeted service requires productId");
            }
            ProductEntity product =
                    productRepository.findById(productId)
                            .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "product not found"));
            if (!enterpriseId.equals(product.getEnterpriseId())) {
                throw new BizException(ErrorCode.FORBIDDEN, "product does not belong to current enterprise");
            }
        }
    }

    private void createDefaultFulfillment(ServiceOrderEntity order) {
        ServiceFulfillmentEntity requirement = new ServiceFulfillmentEntity();
        requirement.setServiceOrderId(order.getId());
        requirement.setServiceProviderId(order.getServiceProviderId());
        requirement.setMilestoneCode("requirement_confirm");
        requirement.setMilestoneName("需求确认");
        requirement.setStatus(FulfillmentStatus.PENDING);
        requirement.setDetail("确认服务范围、所需材料与交付边界。");

        ServiceFulfillmentEntity delivery = new ServiceFulfillmentEntity();
        delivery.setServiceOrderId(order.getId());
        delivery.setServiceProviderId(order.getServiceProviderId());
        delivery.setMilestoneCode("delivery");
        delivery.setMilestoneName("交付成果");
        delivery.setStatus(FulfillmentStatus.PENDING);
        delivery.setDetail("上传最终交付物并等待平台/企业确认。");

        serviceFulfillmentRepository.saveAll(List.of(requirement, delivery));
    }

    private ServiceOrderResponse toOrderResponse(ServiceOrderEntity order) {
        ServiceSummaryResponse service = serviceCatalogService.toSummary(serviceCatalogService.loadService(order.getServiceId()));
        ServiceOfferEntity offer = serviceCatalogService.loadOffer(order.getOfferId());
        String providerName = resolveProviderName(order.getServiceProviderId());
        PaymentRecordResponse payment =
                paymentRecordRepository.findTopByServiceOrderIdOrderByCreatedAtDesc(order.getId())
                        .map(record -> toPaymentResponse(record, order.getOrderNo(), service.title()))
                        .orElse(null);
        List<FulfillmentResponse> fulfillments =
                serviceFulfillmentRepository.findByServiceOrderIdOrderByCreatedAtAsc(order.getId()).stream()
                        .map(this::toFulfillmentResponse)
                        .toList();
        List<DeliveryArtifactResponse> artifacts =
                deliveryArtifactRepository.findByServiceOrderIdOrderByCreatedAtAsc(order.getId()).stream()
                        .map(this::toArtifactResponse)
                        .toList();
        return new ServiceOrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getEnterpriseId(),
                order.getProductId(),
                order.getServiceId(),
                order.getOfferId(),
                order.getServiceProviderId(),
                providerName,
                service.title(),
                offer.getName(),
                offer.getTargetResourceType().getCode(),
                order.getStatus().getCode(),
                order.getPaymentStatus().getCode(),
                order.getAmount(),
                order.getCurrency(),
                order.getCustomerNote(),
                order.getCreatedAt(),
                order.getCompletedAt(),
                payment,
                fulfillments,
                artifacts);
    }

    private PaymentRecordResponse toPaymentResponse(
            PaymentRecordEntity record, String orderNo, String serviceTitle) {
        return new PaymentRecordResponse(
                record.getId(),
                record.getServiceOrderId(),
                orderNo,
                serviceTitle,
                record.getAmount(),
                record.getCurrency(),
                record.getPaymentMethod().getCode(),
                record.getStatus().getCode(),
                record.getEvidenceFileUrl(),
                record.getNote(),
                record.getSubmittedAt(),
                record.getConfirmedAt(),
                record.getConfirmedNote());
    }

    private FulfillmentResponse toFulfillmentResponse(ServiceFulfillmentEntity item) {
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

    private String resolveProviderName(UUID providerId) {
        if (providerId == null) {
            return "平台自营";
        }
        return serviceProviderProfileRepository.findTopByServiceProviderIdOrderByVersionNoDesc(providerId)
                .map(profile -> profile.getCompanyName())
                .orElse("服务商");
    }

    private String generateOrderNo() {
        return "SO-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(OffsetDateTime.now()) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
