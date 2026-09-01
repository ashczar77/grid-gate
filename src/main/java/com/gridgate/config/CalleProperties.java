package com.gridgate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CALL-E Developer API settings bound from {@code calle.*} and environment variables.
 */
@ConfigurationProperties(prefix = "calle")
public record CalleProperties(
        String apiKey,
        String baseUrl,
        String webhookPublicUrl,
        String webhookSecret) {

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public boolean hasWebhookPublicUrl() {
        return webhookPublicUrl != null && !webhookPublicUrl.isBlank();
    }

    public boolean hasWebhookSecret() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }
}
