package com.gridgate.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CallePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of())
            .withBean(org.springframework.web.reactive.function.client.WebClient.Builder.class, () ->
                    org.springframework.web.reactive.function.client.WebClient.builder())
            .withUserConfiguration(CalleConfig.class);

    @Test
    void bindsFromPropertyValues() {
        contextRunner
                .withPropertyValues(
                        "calle.api-key=calle_test_key",
                        "calle.base-url=https://api.heycall-e.com",
                        "calle.webhook-public-url=https://example.com/calle/webhook",
                        "calle.webhook-secret=test_secret_123")
                .run(context -> {
                    CalleProperties properties = context.getBean(CalleProperties.class);
                    assertEquals("calle_test_key", properties.apiKey());
                    assertEquals("https://api.heycall-e.com", properties.baseUrl());
                    assertEquals("https://example.com/calle/webhook", properties.webhookPublicUrl());
                    assertEquals("test_secret_123", properties.webhookSecret());
                    assertTrue(properties.hasApiKey());
                    assertTrue(properties.hasWebhookPublicUrl());
                    assertTrue(properties.hasWebhookSecret());
                });
    }

    @Test
    void emptyApiKeyAndWebhookAreDetected() {
        contextRunner
                .withPropertyValues(
                        "calle.api-key=",
                        "calle.base-url=https://api.heycall-e.com",
                        "calle.webhook-public-url=",
                        "calle.webhook-secret=")
                .run(context -> {
                    CalleProperties properties = context.getBean(CalleProperties.class);
                    assertFalse(properties.hasApiKey());
                    assertFalse(properties.hasWebhookPublicUrl());
                    assertFalse(properties.hasWebhookSecret());
                });
    }
}
