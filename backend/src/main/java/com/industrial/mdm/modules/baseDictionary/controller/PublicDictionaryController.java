package com.industrial.mdm.modules.baseDictionary.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.modules.baseDictionary.application.BaseDictionaryService;
import com.industrial.mdm.modules.baseDictionary.dto.DictTypeResponse;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dictionaries")
public class PublicDictionaryController {

    private final BaseDictionaryService baseDictionaryService;

    public PublicDictionaryController(BaseDictionaryService baseDictionaryService) {
        this.baseDictionaryService = baseDictionaryService;
    }

    @GetMapping("/{typeCode}")
    public ApiResponse<DictTypeResponse> getType(@PathVariable String typeCode) {
        return ApiResponse.success(
                baseDictionaryService.getType(typeCode, true),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
