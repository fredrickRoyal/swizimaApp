package com.swizima.clientapi.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Stores a {@code List<String>} as a JSON array string, e.g. {@code ["Car wash & detailing","Car park & parking"]}.
 * Empty/null lists are stored as SQL NULL.
 */
@Converter
public class StringListJsonConverter implements AttributeConverter<List<String>, String> {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(values);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Could not serialise list to JSON", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, LIST_OF_STRINGS);
        } catch (JacksonException e) {
            throw new IllegalStateException("Stored value is not a JSON string array: " + json, e);
        }
    }
}
