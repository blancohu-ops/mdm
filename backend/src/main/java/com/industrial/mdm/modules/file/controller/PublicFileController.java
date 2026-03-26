package com.industrial.mdm.modules.file.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.modules.file.application.FileService;
import com.industrial.mdm.modules.file.dto.StoredFileResponse;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/public/files")
public class PublicFileController {

    private final FileService fileService;

    public PublicFileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ApiResponse<StoredFileResponse> upload(
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String accessScope,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(
                fileService.uploadPublic(businessType, accessScope, file),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
