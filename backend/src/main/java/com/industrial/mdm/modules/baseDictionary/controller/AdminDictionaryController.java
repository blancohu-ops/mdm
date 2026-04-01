package com.industrial.mdm.modules.baseDictionary.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.modules.baseDictionary.application.BaseDictionaryService;
import com.industrial.mdm.modules.baseDictionary.dto.DictItemSaveRequest;
import com.industrial.mdm.modules.baseDictionary.dto.DictTypeResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dictionaries")
public class AdminDictionaryController {

    private final BaseDictionaryService baseDictionaryService;

    public AdminDictionaryController(BaseDictionaryService baseDictionaryService) {
        this.baseDictionaryService = baseDictionaryService;
    }

    @GetMapping
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'base_dict:read')")
    public ApiResponse<List<DictTypeResponse>> listTypes() {
        return ApiResponse.success(
                baseDictionaryService.listTypes(), MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/{typeCode}")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'base_dict:read')")
    public ApiResponse<DictTypeResponse> getType(@PathVariable String typeCode) {
        return ApiResponse.success(
                baseDictionaryService.getType(typeCode, false),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/{typeCode}/items")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'base_dict:create')")
    public ApiResponse<DictTypeResponse.DictItemResponse> createItem(
            @PathVariable String typeCode, @Valid @RequestBody DictItemSaveRequest request) {
        return ApiResponse.success(
                baseDictionaryService.createItem(typeCode, request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PutMapping("/{typeCode}/items/{itemId}")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'base_dict:update')")
    public ApiResponse<DictTypeResponse.DictItemResponse> updateItem(
            @PathVariable String typeCode,
            @PathVariable UUID itemId,
            @Valid @RequestBody DictItemSaveRequest request) {
        return ApiResponse.success(
                baseDictionaryService.updateItem(typeCode, itemId, request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @DeleteMapping("/{typeCode}/items/{itemId}")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'base_dict:delete')")
    public ApiResponse<Map<String, String>> deleteItem(
            @PathVariable String typeCode, @PathVariable UUID itemId) {
        baseDictionaryService.deleteItem(typeCode, itemId);
        return ApiResponse.success(
                Map.of("deletedItemId", itemId.toString()),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
