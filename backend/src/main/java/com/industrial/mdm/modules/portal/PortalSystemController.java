package com.industrial.mdm.modules.portal;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import java.time.OffsetDateTime;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class PortalSystemController {

    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        return ApiResponse.success(
                Map.of("service", "mdm-backend", "status", "ok", "time", OffsetDateTime.now()),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
