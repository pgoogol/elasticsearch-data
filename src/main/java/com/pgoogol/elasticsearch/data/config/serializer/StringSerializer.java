package com.pgoogol.elasticsearch.data.config.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class StringSerializer extends JsonSerializer<String> {

    @Override
    public boolean isEmpty(SerializerProvider provider, String value) {
        return value != null && value.isBlank();
    }

    @Override
    public void serialize(String value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (value.isEmpty()) {
            value = value.trim();
        }
        jsonGenerator.writeString(value);
    }
}
