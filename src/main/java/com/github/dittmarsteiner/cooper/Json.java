package com.github.dittmarsteiner.cooper;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.util.*;

import static com.github.dittmarsteiner.cooper.Resulting.asRuntimeException;
import static java.util.Objects.isNull;

public enum Json {

    JSON(false), PRETTY(true);

    public final ObjectMapper mapper;

    Json(boolean pretty) {
        var mapper = newBasicMapper();
        if (pretty) {
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        }

        this.mapper = mapper;
    }

    static ObjectMapper newBasicMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        mapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);

        return mapper;
    }

    public String marshal(Object obj) throws RuntimeException {
        try {
            return mapper.writeValueAsString(obj);
        }
        catch (Throwable e) {
            throw asRuntimeException(e);
        }
    }

    public <T> T marshal(T obj, OutputStream out)
            throws RuntimeException {
        try {
            mapper.writeValue(out, obj);
            return obj;
        }
        catch (Throwable e) {
            throw asRuntimeException(e);
        }
    }

    public <T> T unmarshal(String input, Class<T> valueType)
            throws RuntimeException {
        try {
            return mapper.readValue(input, valueType);
        }
        catch (Throwable e) {
            throw asRuntimeException(e);
        }
    }

    public <T> T unmarshal(InputStream input, Class<T> valueType)
            throws RuntimeException {
        try (input) {
            return mapper.readValue(input, valueType);
        }
        catch (Throwable e) {
            throw asRuntimeException(e);
        }
    }

    public <T> Optional<T> valueOf(Map<String, ?> map, String path) {
        return valueOf(map, (Object[]) path.split("\\."));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> valueOf(Map<?, ?> map, Object... path) {
        Object obj = map;
        for (Object key : path) {
            if (isNull(obj) || !(obj instanceof Map)) {
                return Optional.empty();
            }

            Map<Object, Object> m = (Map<Object, Object>) obj;
            obj = m.get(key);
        }

        return Optional.ofNullable((T) obj);
    }

    @SuppressWarnings("unchecked")
    public <T> T copy(T value) {
        if (isNull(value)) {
            return null;
        }

        try {
            Map<String, Object> map =
                    mapper.convertValue(value, Map.class);
            return (T) mapper.convertValue(map, value.getClass());
        }
        catch (Throwable e) {
            throw new RuntimeException("Failed to copy " + value, e);
        }
    }
}
