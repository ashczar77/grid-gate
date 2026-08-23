package com.gridgate.calle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * CALL-E recipient_result_schema definitions loaded from classpath resources.
 */
public final class RecipientResultSchemas {

    private static final String RECIPIENT_RESULT_SCHEMA = "/schemas/recipient-result.json";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RecipientResultSchemas() {}

    public static Map<String, Object> recipientResultSchema() {
        return loadSchema(RECIPIENT_RESULT_SCHEMA);
    }

    private static Map<String, Object> loadSchema(String classpathResource) {
        try (InputStream input = RecipientResultSchemas.class.getResourceAsStream(classpathResource)) {
            if (input == null) {
                throw new IllegalStateException("Schema not found on classpath: " + classpathResource);
            }
            return MAPPER.readValue(input, new TypeReference<>() {});
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load schema: " + classpathResource, ex);
        }
    }
}
