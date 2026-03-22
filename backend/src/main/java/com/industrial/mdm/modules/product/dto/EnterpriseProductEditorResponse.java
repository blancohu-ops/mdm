package com.industrial.mdm.modules.product.dto;

import java.util.List;

public record EnterpriseProductEditorResponse(
        ProductResponse product,
        List<String> categories,
        List<String> unitOptions,
        List<String> certificationOptions,
        List<HsSuggestionResponse> hsSuggestions) {}
