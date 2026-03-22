package com.industrial.mdm.modules.message.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.message.application.MessageService;
import com.industrial.mdm.modules.message.dto.EnterpriseMessageListResponse;
import com.industrial.mdm.modules.message.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enterprise/messages")
@Tag(name = "Enterprise / Messages")
@PreAuthorize("hasAuthority('enterprise_owner')")
public class EnterpriseMessageController {

    private final MessageService messageService;

    public EnterpriseMessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    @Operation(summary = "List enterprise messages")
    public ApiResponse<EnterpriseMessageListResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(
                messageService.listEnterpriseMessages(currentUser, type, status),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/{messageId}/mark-read")
    @Operation(summary = "Mark message as read")
    public ApiResponse<MessageResponse> markRead(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID messageId) {
        return ApiResponse.success(
                messageService.markRead(currentUser, messageId),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/mark-all-read")
    @Operation(summary = "Mark all messages as read")
    public ApiResponse<Map<String, Long>> markAllRead(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                messageService.markAllRead(currentUser), MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
