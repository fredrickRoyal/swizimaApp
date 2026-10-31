package com.swizima.clientapi.entity.converter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StringListJsonConverterTest {

    private final StringListJsonConverter converter = new StringListJsonConverter();

    @Test
    void writesJsonArray() {
        assertThat(converter.convertToDatabaseColumn(List.of("Car wash & detailing", "Car park & parking")))
                .isEqualTo("[\"Car wash & detailing\",\"Car park & parking\"]");
    }

    @Test
    void emptyOrNullBecomesNull() {
        assertThat(converter.convertToDatabaseColumn(List.of())).isNull();
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void readsJsonArray() {
        assertThat(converter.convertToEntityAttribute("[\"Fleet management\",\"Driving school\"]"))
                .containsExactly("Fleet management", "Driving school");
    }

    @Test
    void nullOrBlankReadsAsEmpty() {
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
        assertThat(converter.convertToEntityAttribute(" ")).isEmpty();
    }
}
