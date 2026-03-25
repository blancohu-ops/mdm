package com.industrial.mdm.modules.file.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.file.application.FileService;
import com.industrial.mdm.modules.file.dto.StoredFileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "Files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload a file")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'file_asset:upload')")
    public ApiResponse<StoredFileResponse> upload(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String accessScope,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(
                fileService.upload(currentUser, businessType, accessScope, file),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "Get stored file metadata")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'file_asset:read')")
    public ApiResponse<StoredFileResponse> metadata(
            @PathVariable UUID fileId, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                fileService.getMetadata(fileId, currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/{fileId}/download")
    @Operation(summary = "Download stored file")
    public ResponseEntity<Resource> download(
            @PathVariable UUID fileId, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return fileService.download(fileId, currentUser);
    }
}
