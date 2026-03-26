package com.industrial.mdm.modules.iam.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.common.security.JwtAuthenticationFilter;
import com.industrial.mdm.common.security.JwtTokenService;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.config.SecurityConfig;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.iam.application.AccessGrantRequestService;
import com.industrial.mdm.modules.iam.application.AuthorizationAdministrationService;
import com.industrial.mdm.modules.iam.application.ReviewDomainAdministrationService;
import com.industrial.mdm.modules.iam.dto.GrantReviewDomainAssignmentRequest;
import com.industrial.mdm.modules.iam.dto.AccessGrantRequestDecisionRequest;
import com.industrial.mdm.modules.iam.dto.GrantCapabilityBindingRequest;
import com.industrial.mdm.modules.iam.dto.GrantRoleTemplateRequest;
import com.industrial.mdm.modules.iam.dto.GrantTemporaryAccessRequest;
import com.industrial.mdm.modules.iam.dto.RevokeAuthorizationRequest;
import com.industrial.mdm.modules.iam.dto.SubmitAccessGrantRequestRequest;
import com.industrial.mdm.modules.iam.repository.AccessGrantRequestEntity;
import com.industrial.mdm.modules.iam.repository.IamAuditLogEntity;
import com.industrial.mdm.modules.iam.repository.ReviewDomainAssignmentEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminIamController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminIamControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccessGrantRequestService accessGrantRequestService;

    @MockBean
    private AuthorizationAdministrationService authorizationAdministrationService;

    @MockBean
    private ReviewDomainAdministrationService reviewDomainAdministrationService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void iamEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/iam/audit-logs")).andExpect(status().isUnauthorized());
    }

    @Test
    void iamEndpointsRejectEnterpriseRole() throws Exception {
        mockMvc.perform(
                        get("/api/v1/admin/iam/audit-logs")
                                .with(authentication(authenticationFor(UserRole.ENTERPRISE_OWNER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void iamEndpointsRejectReviewerRole() throws Exception {
        mockMvc.perform(
                        get("/api/v1/admin/iam/audit-logs")
                                .with(authentication(authenticationFor(UserRole.REVIEWER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void grantRoleTemplateAllowsOperationsAdmin() throws Exception {
        AuthenticatedUser currentUser = authenticatedUser(UserRole.OPERATIONS_ADMIN);
        UUID targetUserId = UUID.randomUUID();
        UUID bindingId = UUID.randomUUID();
        OffsetDateTime effectiveFrom = OffsetDateTime.parse("2026-03-24T10:15:30+08:00");
        OffsetDateTime expiresAt = OffsetDateTime.parse("2026-04-24T10:15:30+08:00");
        when(authorizationAdministrationService.grantRoleTemplate(
                        eq(currentUser), any(AuthorizationAdministrationService.GrantRoleTemplateCommand.class)))
                .thenReturn(
                        new AuthorizationAdministrationService.AuthorizationMutationResult(
                                bindingId,
                                "role_binding",
                                targetUserId,
                                "operations_admin",
                                effectiveFrom,
                                null,
                                null));

        mockMvc.perform(
                        post("/api/v1/admin/iam/role-bindings")
                                .with(authentication(authenticationFor(currentUser)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new GrantRoleTemplateRequest(
                                                        targetUserId,
                                                        "operations_admin",
                                                        "platform bootstrap",
                                                        effectiveFrom,
                                                        expiresAt))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").value(bindingId.toString()))
                .andExpect(jsonPath("$.data.targetUserId").value(targetUserId.toString()))
                .andExpect(jsonPath("$.data.code").value("operations_admin"));

        verify(authorizationAdministrationService)
                .grantRoleTemplate(
                        eq(currentUser),
                        argThat(
                                command ->
                                        targetUserId.equals(command.targetUserId())
                                                && "operations_admin"
                                                        .equals(command.roleTemplateCode())
                                                && "platform bootstrap".equals(command.reason())
                                                && effectiveFrom
                                                        .toInstant()
                                                        .equals(command.effectiveFrom().toInstant())
                                                && expiresAt
                                                        .toInstant()
                                                        .equals(command.expiresAt().toInstant())));
    }

    @Test
    void grantReviewDomainAssignmentAllowsOperationsAdmin() throws Exception {
        AuthenticatedUser currentUser = authenticatedUser(UserRole.OPERATIONS_ADMIN);
        UUID targetUserId = UUID.randomUUID();
        UUID enterpriseId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        ReviewDomainAssignmentEntity assignment = new ReviewDomainAssignmentEntity();
        assignment.setUserId(targetUserId);
        assignment.setDomainType("company_review");
        assignment.setEnterpriseId(enterpriseId);
        assignment.setGrantedBy(currentUser.userId());
        assignment.setReason("assign enterprise pool");
        assignment.setEffectiveFrom(OffsetDateTime.parse("2026-03-24T10:15:30+08:00"));
        setEntityId(assignment, assignmentId);

        when(reviewDomainAdministrationService.grantAssignment(
                        eq(currentUser),
                        any(ReviewDomainAdministrationService.GrantReviewDomainAssignmentCommand.class)))
                .thenReturn(assignment);

        mockMvc.perform(
                        post("/api/v1/admin/iam/review-domain-assignments")
                                .with(authentication(authenticationFor(currentUser)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new GrantReviewDomainAssignmentRequest(
                                                        targetUserId,
                                                        "company_review",
                                                        enterpriseId,
                                                        "assign enterprise pool",
                                                        OffsetDateTime.parse("2026-03-24T10:15:30+08:00"),
                                                        OffsetDateTime.parse("2026-04-24T10:15:30+08:00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(assignmentId.toString()))
                .andExpect(jsonPath("$.data.targetUserId").value(targetUserId.toString()))
                .andExpect(jsonPath("$.data.domainType").value("company_review"));
    }

    @Test
    void grantReviewDomainAssignmentAllowsReviewer() throws Exception {
        AuthenticatedUser currentUser = authenticatedUser(UserRole.REVIEWER);
        UUID targetUserId = UUID.randomUUID();
        UUID enterpriseId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        ReviewDomainAssignmentEntity assignment = new ReviewDomainAssignmentEntity();
        assignment.setUserId(targetUserId);
        assignment.setDomainType("company_review");
        assignment.setEnterpriseId(enterpriseId);
        assignment.setGrantedBy(currentUser.userId());
        assignment.setReason("assign enterprise pool");
        assignment.setEffectiveFrom(OffsetDateTime.parse("2026-03-24T10:15:30+08:00"));
        setEntityId(assignment, assignmentId);

        when(reviewDomainAdministrationService.grantAssignment(
                        eq(currentUser),
                        any(ReviewDomainAdministrationService.GrantReviewDomainAssignmentCommand.class)))
                .thenReturn(assignment);

        mockMvc.perform(
                        post("/api/v1/admin/iam/review-domain-assignments")
                                .with(authentication(authenticationFor(currentUser)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new GrantReviewDomainAssignmentRequest(
                                                        targetUserId,
                                                        "company_review",
                                                        enterpriseId,
                                                        "assign enterprise pool",
                                                        OffsetDateTime.parse("2026-03-24T10:15:30+08:00"),
                                                        OffsetDateTime.parse("2026-04-24T10:15:30+08:00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(assignmentId.toString()));
    }

    @Test
    void grantReviewDomainAssignmentValidatesPayload() throws Exception {
        mockMvc.perform(
                        post("/api/v1/admin/iam/review-domain-assignments")
                                .with(authentication(authenticationFor(UserRole.OPERATIONS_ADMIN)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "targetUserId": null,
                                          "domainType": "",
                                          "enterpriseId": null,
                                          "reason": ""
                                        }
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listReviewDomainAssignmentsAllowsOperationsAdmin() throws Exception {
        AuthenticatedUser currentUser = authenticatedUser(UserRole.OPERATIONS_ADMIN);
        UUID targetUserId = UUID.randomUUID();
        UUID enterpriseId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        ReviewDomainAssignmentEntity assignment = new ReviewDomainAssignmentEntity();
        assignment.setUserId(targetUserId);
        assignment.setDomainType("product_review");
        assignment.setEnterpriseId(enterpriseId);
        assignment.setGrantedBy(UUID.randomUUID());
        assignment.setReason("product pool");
        assignment.setEffectiveFrom(OffsetDateTime.parse("2026-03-24T10:15:30+08:00"));
        setEntityId(assignment, assignmentId);

        when(reviewDomainAdministrationService.listAssignments(
                        eq(currentUser),
                        any(ReviewDomainAdministrationService.ListReviewDomainAssignmentsQuery.class)))
                .thenReturn(List.of(assignment));

        mockMvc.perform(
                        get("/api/v1/admin/iam/review-domain-assignments")
                                .with(authentication(authenticationFor(currentUser)))
                                .param("targetUserId", targetUserId.toString())
                                .param("domainType", "product_review")
                                .param("enterpriseId", enterpriseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(assignmentId.toString()))
                .andExpect(jsonPath("$.data.items[0].domainType").value("product_review"));
    }

    @Test
    void listReviewDomainAssignmentsAllowsReviewer() throws Exception {
        AuthenticatedUser currentUser = authenticatedUser(UserRole.REVIEWER);
        when(reviewDomainAdministrationService.listAssignments(
                        eq(currentUser),
                        any(ReviewDomainAdministrationService.ListReviewDomainAssignmentsQuery.class)))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/admin/iam/review-domain-assignments")
                                .with(authentication(authenticationFor(currentUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void revokeReviewDomainAssignmentAllowsOperationsAdmin() throws Exception {
        AuthenticatedUser currentUser = authenticatedUser(UserRole.OPERATIONS_ADMIN);
        UUID assignmentId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID enterpriseId = UUID.randomUUID();
        ReviewDomainAssignmentEntity assignment = new ReviewDomainAssignmentEntity();
        assignment.setUserId(targetUserId);
        assignment.setDomainType("company_manage");
        assignment.setEnterpriseId(enterpriseId);
        assignment.setGrantedBy(UUID.randomUUID());
        assignment.setReason("manage pool");
        assignment.setEffectiveFrom(OffsetDateTime.parse("2026-03-24T10:15:30+08:00"));
        assignment.setRevokedAt(OffsetDateTime.parse("2026-03-24T11:15:30+08:00"));
        setEntityId(assignment, assignmentId);

        when(reviewDomainAdministrationService.revokeAssignment(
                        eq(currentUser), eq(assignmentId), eq("manual revoke")))
                .thenReturn(assignment);

        mockMvc.perform(
                        post("/api/v1/admin/iam/review-domain-assignments/{assignmentId}/revoke", assignmentId)
                                .with(authentication(authenticationFor(currentUser)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new RevokeAuthorizationRequest("manual revoke"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(assignmentId.toString()))
                .andExpect(jsonPath("$.data.revokedAt").exists());
    }

    @Test
    void revokeReviewDomainAssignmentAllowsReviewer() throws Exception {
        AuthenticatedUser currentUser = authenticatedUser(UserRole.REVIEWER);
        UUID assignmentId = UUID.randomUUID();
        ReviewDomainAssignmentEntity assignment = new ReviewDomainAssignmentEntity();
        assignment.setUserId(UUID.randomUUID());
        assignment.setDomainType("company_review");
        assignment.setEnterpriseId(UUID.randomUUID());
        assignment.setGrantedBy(currentUser.userId());
        assignment.setReason("review coverage");
        assignment.setEffectiveFrom(OffsetDateTime.parse("2026-03-24T10:15:30+08:00"));
        assignment.setRevokedAt(OffsetDateTime.parse("2026-03-24T11:15:30+08:00"));
        setEntityId(assignment, assignmentId);

        when(reviewDomainAdministrationService.revokeAssignment(
                        eq(currentUser), eq(assignmentId), eq("manual revoke")))
                .thenReturn(assignment);

        mockMvc.perform(
                        post("/api/v1/admin/iam/review-domain-assignments/{assignmentId}/revoke", assignmentId)
                                .with(authentication(authenticationFor(currentUser)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new RevokeAuthorizationRequest("manual revoke"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(assignmentId.toString()));
    }

    @Test
    void revokeRoleTemplateAllowsOperationsAdmin() throws Exception {
        AuthenticatedUser currentUser = authenticatedUser(UserRole.OPERATIONS_ADMIN);
        UUID bindingId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        when(authorizationAdministrationService.revokeRoleTemplateBinding(
                        eq(currentUser), eq(bindingId), eq("manual revoke")))
                .thenReturn(
                        new AuthorizationAdministrationService.AuthorizationMutationResult(
                                bindingId,
                                "role_binding",
                                targetUserId,
                                "revoked",
                                OffsetDateTime.parse("2026-03-24T10:15:30+08:00"),
                                null,
                                OffsetDateTime.parse("2026-03-24T11:15:30+08:00")));

        mockMvc.perform(
                        post("/api/v1/admin/iam/role-bindings/{bindingId}/revoke", bindingId)
                                .with(authentication(authenticationFor(currentUser)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new RevokeAuthorizationRequest("manual revoke"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(bindingId.toString()))
                .andExpect(jsonPath("$.data.code").value("revoked"));
    }

    @Test
    void grantCapabilityBindingAllowsOperationsAdmin() throws Exception {
        AuthenticatedUser currentUser = authenticatedUser(UserRole.OPERATIONS_ADMIN);
        UUID bindingId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        when(authorizationAdministrationService.grantCapabilityBinding(
                        eq(currentUser),
                        any(AuthorizationAdministrationService.GrantCapabilityBindingCommand.class)))
                .thenReturn(
                        new AuthorizationAdministrationService.AuthorizationMutationResult(
                                bindingId,
                                "capability_binding",
                                targetUserId,
                                "cap.export_sensitive",
                                OffsetDateTime.parse("2026-03-24T10:15:30+08:00"),
                                null,
                                null));

        mockMvc.perform(
                        post("/api/v1/admin/iam/capability-bindings")
                                .with(authentication(authenticationFor(currentUser)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new GrantCapabilityBindingRequest(
                                                        targetUserId,
                                                        "cap.export_sensitive",
                                                        "support shift",
                                                        null,
                                                        null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(bindingId.toString()))
                .andExpect(jsonPath("$.data.code").value("cap.export_sensitive"));

        verify(authorizationAdministrationService)
                .grantCapabilityBinding(
                        eq(currentUser),
                        argThat(
                                command ->
                                        targetUserId.equals(command.targetUserId())
                                                && "cap.export_sensitive"
                                                        .equals(command.capabilityCode())
                                                && "support shift".equals(command.reason())));
    }

    @Test
    void revokeCapabilityBindingAllowsOperationsAdmin() throws Exception {
        AuthenticatedUser currentUser = authenticatedUser(UserRole.OPERATIONS_ADMIN);
        UUID bindingId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        when(authorizationAdministrationService.revokeCapabilityBinding(
                        eq(currentUser), eq(bindingId), eq("expired")))
                .thenReturn(
                        new AuthorizationAdministrationService.AuthorizationMutationResult(
                                bindingId,
                                "capability_binding",
                                targetUserId,
                                "revoked",
                                OffsetDateTime.parse("2026-03-24T10:15:30+08:00"),
                                null,
                                OffsetDateTime.parse("2026-03-24T11:15:30+08:00")));

        mockMvc.perform(
                        post("/api/v1/admin/iam/capability-bindings/{bindingId}/revoke", bindingId)
                                .with(authentication(authenticationFor(currentUser)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new RevokeAuthorizationRequest("expired"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(bindingId.toString()))
                .andExpect(jsonPath("$.data.code").value("revoked"));
    }

    @Test
    void revokeCapabilityBindingValidatesReason() throws Exception {
        mockMvc.perform(
                        post("/api/v1/admin/iam/capability-bindings/{bindingId}/revoke", UUID.randomUUID())
                                .with(authentication(authenticationFor(UserRole.OPERATIONS_ADMIN)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void grantTemporaryAccessAllowsOperationsAdmin() throws Exception {
        AuthenticatedUser currentUser = authenticatedUser(UserRole.OPERATIONS_ADMIN);
        UUID grantId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID enterpriseId = UUID.randomUUID();
        when(authorizationAdministrationService.grantTemporaryAccess(
                        eq(currentUser),
                        any(AuthorizationAdministrationService.GrantTemporaryAccessCommand.class)))
                .thenReturn(
                        new AuthorizationAdministrationService.AuthorizationMutationResult(
                                grantId,
                                "access_grant",
                                targetUserId,
                                "access_grant:manage",
                                OffsetDateTime.parse("2026-03-24T10:15:30+08:00"),
                                null,
                                null));

        mockMvc.perform(
                        post("/api/v1/admin/iam/access-grants")
                                .with(authentication(authenticationFor(currentUser)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new GrantTemporaryAccessRequest(
                                                        targetUserId,
                                                        "access_grant:manage",
                                                        enterpriseId,
                                                        null,
                                                        null,
                                                        null,
                                                        null,
                                                        "incident response",
                                                        "TICKET-42",
                                                        OffsetDateTime.parse("2026-03-24T10:15:30+08:00"),
                                                        OffsetDateTime.parse("2026-03-24T12:15:30+08:00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(grantId.toString()))
                .andExpect(jsonPath("$.data.code").value("access_grant:manage"));

        verify(authorizationAdministrationService)
                .grantTemporaryAccess(
                        eq(currentUser),
                        argThat(
                                command ->
                                        targetUserId.equals(command.targetUserId())
                                                && "access_grant:manage"
                                                        .equals(command.permissionCode())
                                                && enterpriseId.equals(command.enterpriseId())
                                                && command.scopeType() == null
                                                && command.scopeValue() == null
                                                && command.resourceType() == null
                                                && command.resourceId() == null
                                                && "incident response".equals(command.reason())
                                                && "TICKET-42".equals(command.ticketNo())));
    }

    @Test
    void revokeTemporaryAccessAllowsOperationsAdmin() throws Exception {
        AuthenticatedUser currentUser = authenticatedUser(UserRole.OPERATIONS_ADMIN);
        UUID grantId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        when(authorizationAdministrationService.revokeTemporaryAccess(
                        eq(currentUser), eq(grantId), eq("ticket closed")))
                .thenReturn(
                        new AuthorizationAdministrationService.AuthorizationMutationResult(
                                grantId,
                                "access_grant",
                                targetUserId,
                                "access_grant:manage",
                                OffsetDateTime.parse("2026-03-24T10:15:30+08:00"),
                                null,
                                OffsetDateTime.parse("2026-03-24T11:15:30+08:00")));

        mockMvc.perform(
                        post("/api/v1/admin/iam/access-grants/{grantId}/revoke", grantId)
                                .with(authentication(authenticationFor(currentUser)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new RevokeAuthorizationRequest("ticket closed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(grantId.toString()))
                .andExpect(jsonPath("$.data.code").value("access_grant:manage"));
    }

    @Test
    void submitAccessGrantRequestAllowsOperationsAdmin() throws Exception {
        AuthenticatedUser currentUser = authenticatedUser(UserRole.OPERATIONS_ADMIN);
        UUID requestId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID enterpriseId = UUID.randomUUID();
        AccessGrantRequestEntity request = new AccessGrantRequestEntity();
        request.setRequestedByUserId(currentUser.userId());
        request.setTargetUserId(targetUserId);
        request.setTargetEnterpriseId(enterpriseId);
        request.setEnterpriseId(enterpriseId);
        request.setPermissionCode("access_grant:manage");
        request.setReason("incident response");
        request.setEffectiveFrom(OffsetDateTime.parse("2026-03-24T10:15:30+08:00"));
        request.setExpiresAt(OffsetDateTime.parse("2026-03-24T12:15:30+08:00"));
        request.setStatus("pending");
        setRequestId(request, requestId);
        when(accessGrantRequestService.submitRequest(
                        eq(currentUser), any(AccessGrantRequestService.SubmitAccessGrantRequestCommand.class)))
                .thenReturn(request);

        mockMvc.perform(
                        post("/api/v1/admin/iam/access-grant-requests")
                                .with(authentication(authenticationFor(currentUser)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new SubmitAccessGrantRequestRequest(
                                                        targetUserId,
                                                        "access_grant:manage",
                                                        enterpriseId,
                                                        null,
                                                        null,
                                                        null,
                                                        null,
                                                        "incident response",
                                                        "INC-18",
                                                        OffsetDateTime.parse("2026-03-24T10:15:30+08:00"),
                                                        OffsetDateTime.parse("2026-03-24T12:15:30+08:00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(requestId.toString()))
                .andExpect(jsonPath("$.data.status").value("pending"));
    }

    @Test
    void submitAccessGrantRequestAllowsReviewer() throws Exception {
        AuthenticatedUser currentUser = authenticatedUser(UserRole.REVIEWER);
        UUID requestId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID enterpriseId = UUID.randomUUID();
        AccessGrantRequestEntity request = new AccessGrantRequestEntity();
        request.setRequestedByUserId(currentUser.userId());
        request.setTargetUserId(targetUserId);
        request.setTargetEnterpriseId(enterpriseId);
        request.setEnterpriseId(enterpriseId);
        request.setPermissionCode("ai_tool:writeback_ai");
        request.setReason("review support");
        request.setEffectiveFrom(OffsetDateTime.parse("2026-03-24T10:15:30+08:00"));
        request.setExpiresAt(OffsetDateTime.parse("2026-03-24T12:15:30+08:00"));
        request.setStatus("pending");
        setRequestId(request, requestId);
        when(accessGrantRequestService.submitRequest(
                        eq(currentUser), any(AccessGrantRequestService.SubmitAccessGrantRequestCommand.class)))
                .thenReturn(request);

        mockMvc.perform(
                        post("/api/v1/admin/iam/access-grant-requests")
                                .with(authentication(authenticationFor(currentUser)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new SubmitAccessGrantRequestRequest(
                                                        targetUserId,
                                                        "ai_tool:writeback_ai",
                                                        enterpriseId,
                                                        null,
                                                        null,
                                                        null,
                                                        null,
                                                        "review support",
                                                        "INC-77",
                                                        OffsetDateTime.parse("2026-03-24T10:15:30+08:00"),
                                                        OffsetDateTime.parse("2026-03-24T12:15:30+08:00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(requestId.toString()))
                .andExpect(jsonPath("$.data.status").value("pending"));
    }

    @Test
    void approveAccessGrantRequestAllowsOperationsAdmin() throws Exception {
        AuthenticatedUser currentUser = authenticatedUser(UserRole.OPERATIONS_ADMIN);
        UUID requestId = UUID.randomUUID();
        AccessGrantRequestEntity request = new AccessGrantRequestEntity();
        request.setRequestedByUserId(UUID.randomUUID());
        request.setTargetUserId(UUID.randomUUID());
        request.setPermissionCode("access_grant:manage");
        request.setReason("incident response");
        request.setEffectiveFrom(OffsetDateTime.parse("2026-03-24T10:15:30+08:00"));
        request.setExpiresAt(OffsetDateTime.parse("2026-03-24T12:15:30+08:00"));
        request.setStatus("approved");
        request.setDecisionComment("approved");
        request.setApprovedByUserId(currentUser.userId());
        request.setApprovedAt(OffsetDateTime.parse("2026-03-24T10:20:30+08:00"));
        request.setApprovedGrantId(UUID.randomUUID());
        setRequestId(request, requestId);
        when(accessGrantRequestService.approveRequest(eq(currentUser), eq(requestId), eq("approved")))
                .thenReturn(request);

        mockMvc.perform(
                        post("/api/v1/admin/iam/access-grant-requests/{requestId}/approve", requestId)
                                .with(authentication(authenticationFor(currentUser)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new AccessGrantRequestDecisionRequest("approved"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(requestId.toString()))
                .andExpect(jsonPath("$.data.status").value("approved"));
    }

    @Test
    void listAccessGrantRequestsAllowsOperationsAdmin() throws Exception {
        AuthenticatedUser currentUser = authenticatedUser(UserRole.OPERATIONS_ADMIN);
        UUID requestId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID enterpriseId = UUID.randomUUID();
        AccessGrantRequestEntity request = new AccessGrantRequestEntity();
        request.setRequestedByUserId(requesterId);
        request.setTargetUserId(UUID.randomUUID());
        request.setTargetEnterpriseId(enterpriseId);
        request.setPermissionCode("ai_tool:writeback_ai");
        request.setReason("incident response");
        request.setEffectiveFrom(OffsetDateTime.parse("2026-03-24T10:15:30+08:00"));
        request.setExpiresAt(OffsetDateTime.parse("2026-03-24T12:15:30+08:00"));
        request.setStatus("pending");
        setRequestId(request, requestId);
        when(accessGrantRequestService.listRequests(
                        eq(currentUser),
                        any(AccessGrantRequestService.ListAccessGrantRequestsQuery.class)))
                .thenReturn(
                        new AccessGrantRequestService.AccessGrantRequestListResult(
                                List.of(request), 1, 0, 20));

        mockMvc.perform(
                        get("/api/v1/admin/iam/access-grant-requests")
                                .with(authentication(authenticationFor(currentUser)))
                                .param("status", "pending")
                                .param("requestedByUserId", requesterId.toString())
                                .param("targetEnterpriseId", enterpriseId.toString())
                                .param("page", "0")
                                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.items[0].id").value(requestId.toString()));
    }

    @Test
    void listAccessGrantRequestsAllowsReviewer() throws Exception {
        AuthenticatedUser currentUser = authenticatedUser(UserRole.REVIEWER);
        when(accessGrantRequestService.listRequests(
                        eq(currentUser),
                        any(AccessGrantRequestService.ListAccessGrantRequestsQuery.class)))
                .thenReturn(
                        new AccessGrantRequestService.AccessGrantRequestListResult(
                                List.of(), 0, 0, 20));

        mockMvc.perform(
                        get("/api/v1/admin/iam/access-grant-requests")
                                .with(authentication(authenticationFor(currentUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void listAccessGrantRequestsReturnsForbiddenWhenAssignedDomainCheckFails() throws Exception {
        AuthenticatedUser currentUser = authenticatedUser(UserRole.REVIEWER);
        when(accessGrantRequestService.listRequests(
                        eq(currentUser),
                        any(AccessGrantRequestService.ListAccessGrantRequestsQuery.class)))
                .thenThrow(
                        new BizException(
                                ErrorCode.FORBIDDEN, "outside assigned enterprise scope"));

        mockMvc.perform(
                        get("/api/v1/admin/iam/access-grant-requests")
                                .with(authentication(authenticationFor(currentUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAuditLogsReturnsServicePayload() throws Exception {
        AuthenticatedUser currentUser = authenticatedUser(UserRole.OPERATIONS_ADMIN);
        IamAuditLogEntity log = new IamAuditLogEntity();
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        log.setActorUserId(actorId);
        log.setActorRole(UserRole.OPERATIONS_ADMIN.getCode());
        log.setActionCode("role_binding.granted");
        log.setTargetType("role_binding");
        log.setTargetId(targetId);
        log.setSummary("Granted role template operations_admin");
        log.setRequestId("req_test");
        when(authorizationAdministrationService.listRecentAuditLogs(eq(currentUser)))
                .thenReturn(List.of(log));

        mockMvc.perform(
                        get("/api/v1/admin/iam/audit-logs")
                                .with(authentication(authenticationFor(currentUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items[0].actorUserId").value(actorId.toString()))
                .andExpect(jsonPath("$.data.items[0].actionCode").value("role_binding.granted"))
                .andExpect(jsonPath("$.data.items[0].targetId").value(targetId.toString()))
                .andExpect(jsonPath("$.data.items[0].requestId").value("req_test"));
    }

    private UsernamePasswordAuthenticationToken authenticationFor(UserRole role) {
        return authenticationFor(authenticatedUser(role));
    }

    private UsernamePasswordAuthenticationToken authenticationFor(AuthenticatedUser currentUser) {
        return new UsernamePasswordAuthenticationToken(
                currentUser, null, currentUser.getAuthorities());
    }

    private AuthenticatedUser authenticatedUser(UserRole role) {
        return new AuthenticatedUser(
                UUID.randomUUID(),
                role,
                role == UserRole.ENTERPRISE_OWNER ? UUID.randomUUID() : null,
                null,
                "tester",
                "org",
                0);
    }

    private void setRequestId(AccessGrantRequestEntity request, UUID requestId) throws Exception {
        setEntityId(request, requestId);
    }

    private void setEntityId(Object entity, UUID id) throws Exception {
        Class<?> type = entity.getClass();
        java.lang.reflect.Field field = type.getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}
