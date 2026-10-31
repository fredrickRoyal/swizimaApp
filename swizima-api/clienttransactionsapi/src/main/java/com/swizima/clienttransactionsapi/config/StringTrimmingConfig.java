package com.swizima.clienttransactionsapi.config;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.deser.jdk.StringDeserializer;
import tools.jackson.databind.module.SimpleModule;

import java.util.Locale;

/**
 * Trims leading/trailing whitespace from every incoming string:
 * JSON request bodies (Jackson) and query/path/form parameters (data binder).
 * Fields whose name contains "password" are left untouched.
 */
@Configuration
public class StringTrimmingConfig {

    @Bean
    public JacksonModule stringTrimmingModule() {
        return new SimpleModule("string-trimming").addDeserializer(String.class, new TrimmingStringDeserializer());
    }

    static final class TrimmingStringDeserializer extends StringDeserializer {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) {
            String value = super.deserialize(p, ctxt);
            if (value == null) return null;
            String field = p.currentName();
            if (field != null && field.toLowerCase(Locale.ROOT).contains("password")) return value;
            return value.strip();
        }
    }

    @ControllerAdvice
    static class TrimmingBinderAdvice {
        @InitBinder
        void trimStrings(WebDataBinder binder) {
            // false: keep "" as "" (services already treat blank as absent)
            binder.registerCustomEditor(String.class, new StringTrimmerEditor(false));
        }
    }
}
