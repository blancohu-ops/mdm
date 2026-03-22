package com.industrial.mdm.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mdm.storage")
public record StorageProperties(@NotBlank String localRoot) {}
