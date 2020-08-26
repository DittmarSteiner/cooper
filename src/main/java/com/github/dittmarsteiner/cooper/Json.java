package com.github.dittmarsteiner.cooper;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.github.dittmarsteiner.cooper.Resulting.asRuntimeException;
import static java.util.Objects.isNull;

public class Json {

    private Json() {
    }

    public static final ObjectMapper mapper = new ObjectMapper();

    public static final ObjectMapper prettyMapper = new ObjectMapper();

    static {
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        mapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS,
                false);

        prettyMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        prettyMapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        prettyMapper.setSerializationInclusion(Include.NON_NULL);
        prettyMapper.registerModule(new JavaTimeModule());
        prettyMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                false);
        prettyMapper.configure(
                SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
        prettyMapper.configure(SerializationFeature.INDENT_OUTPUT, true); // :-)
    }

    public static String marshalToString(Object obj) throws RuntimeException {
        try {
            return mapper.writeValueAsString(obj);
        }
        catch (Throwable e) {
            throw asRuntimeException(e);
        }
    }

    public static <T> T marshal(OutputStream out, T obj)
            throws RuntimeException {
        try {
            mapper.writeValue(out, obj);
            return obj;
        }
        catch (Throwable e) {
            throw asRuntimeException(e);
        }
    }

    public static String marshalPrettyToString(Object obj)
            throws RuntimeException {
        try {
            return prettyMapper.writeValueAsString(obj);
        }
        catch (Throwable e) {
            throw asRuntimeException(e);
        }
    }

    public static void marshalPretty(OutputStream out, Object obj)
            throws RuntimeException {
        try {
            prettyMapper.writeValue(out, obj);
        }
        catch (Throwable e) {
            throw asRuntimeException(e);
        }
    }

    public static <T> T unmarshal(String input, Class<T> valueType)
            throws RuntimeException {
        try {
            return mapper.readValue(input, valueType);
        }
        catch (Throwable e) {
            throw asRuntimeException(e);
        }
    }

    public static <T> T unmarshal(InputStream input, Class<T> valueType)
            throws RuntimeException {
        try (input) {
            return mapper.readValue(input, valueType);
        }
        catch (Throwable e) {
            throw asRuntimeException(e);
        }
    }

    public static InputStream asInputStream(Object obj) {
        if (isNull(obj)) {
            return new ByteArrayInputStream(new byte[0]);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Json.marshal(out, obj);

        return new ByteArrayInputStream(out.toByteArray());
    }

    public static InputStream asInputStreamFromBytes(String value) {
        if (isNull(value)) {
            return new ByteArrayInputStream(new byte[0]);
        }

        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    public static <T> Optional<T> valueOf(Map<String, ?> map, String path) {
        return valueOf(map, (Object[]) path.split("\\."));
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> valueOf(Map<?, ?> map, Object... path) {
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
    public static <T> T copy(T value) {
        if (isNull(value)) {
            return null;
        }

        try {
            Map<String, Object> map =
                    Json.mapper.convertValue(value, Map.class);
            return (T) Json.mapper.convertValue(map, value.getClass());
        }
        catch (Throwable e) {
            throw new RuntimeException("Failed to copy " + value, e);
        }
    }
}
