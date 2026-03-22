package com.industrial.mdm.modules.importtask.dto;

import java.util.List;

public record ImportTemplateResponse(
        String templateName,
        List<String> requiredColumns,
        List<String> optionalColumns,
        List<String> notes) {}
