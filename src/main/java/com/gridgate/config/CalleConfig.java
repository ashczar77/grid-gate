package com.gridgate.config;

import com.gridgate.cascade.CascadeOrchestrator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(CalleProperties.class)
public class CalleConfig {

        @Bean
        public WebClient calleApiWebClient(CalleProperties properties, WebClient.Builder builder) {
                ObjectMapper snakeCaseMapper = JsonMapper.builder()
                                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                                .build();

                ExchangeStrategies strategies = ExchangeStrategies.builder()
                                .codecs(configurer -> {
                                        configurer
                                                        .defaultCodecs()
                                                        .jackson2JsonEncoder(
                                                                        new Jackson2JsonEncoder(snakeCaseMapper,
                                                                                        MediaType.APPLICATION_JSON));
                                        configurer
                                                        .defaultCodecs()
                                                        .jackson2JsonDecoder(
                                                                        new Jackson2JsonDecoder(snakeCaseMapper,
                                                                                        MimeTypeUtils.APPLICATION_JSON));
                                })
                                .build();

                WebClient.Builder webClientBuilder = builder
                                .baseUrl(properties.baseUrl())
                                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                .exchangeStrategies(strategies);

                if (properties.hasApiKey()) {
                        webClientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey());
                }

                return webClientBuilder.build();
        }

        @Bean
        public CascadeOrchestrator cascadeOrchestrator() {
                return new CascadeOrchestrator();
        }
}
