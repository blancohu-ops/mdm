package com.industrial.mdm.modules.importtask.dto;

import java.util.UUID;

public record ImportTaskRowResponse(
        UUID id,
        int rowNo,
        String productName,
        String model,
        String result,
        String reason) {}
