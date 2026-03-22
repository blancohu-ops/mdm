package com.industrial.mdm.modules.importtask.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.importtask.application.ImportTaskService;
import com.industrial.mdm.modules.importtask.dto.ImportTaskCreateRequest;
import com.industrial.mdm.modules.importtask.dto.ImportTaskResponse;
import com.industrial.mdm.modules.importtask.dto.ImportTemplateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enterprise/import-tasks")
@Tag(name = "Enterprise / Import Tasks")
@PreAuthorize("hasAuthority('enterprise_owner')")
public class EnterpriseImportTaskController {

    private final ImportTaskService importTaskService;

    public EnterpriseImportTaskController(ImportTaskService importTaskService) {
        this.importTaskService = importTaskService;
    }

    @GetMapping("/template")
    @Operation(summary = "Get product import template metadata")
    public ApiResponse<ImportTemplateResponse> template() {
        return ApiResponse.success(
                importTaskService.getTemplate(), MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping
    @Operation(summary = "Create import validation task")
    public ApiResponse<ImportTaskResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ImportTaskCreateRequest request) {
        return ApiResponse.success(
                importTaskService.createValidationTask(currentUser, request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get import task detail")
    public ApiResponse<ImportTaskResponse> detail(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID taskId) {
        return ApiResponse.success(
                importTaskService.getTask(currentUser, taskId),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/{taskId}/error-report")
    @Operation(summary = "Download import error report")
    public ResponseEntity<Resource> errorReport(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID taskId) {
        return importTaskService.downloadErrorReport(currentUser, taskId);
    }

    @PostMapping("/{taskId}/confirm")
    @Operation(summary = "Confirm import task")
    public ApiResponse<ImportTaskResponse> confirm(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID taskId) {
        return ApiResponse.success(
                importTaskService.confirmImport(currentUser, taskId),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
