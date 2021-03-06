/*
 * Copyright (c) 2017, Dittmar Steiner <dittmar.steiner@gmail.com>
 * ISC License http://opensource.org/licenses/isc-license.txt
 */
package com.github.dittmarsteiner.cooper;

import static java.util.stream.Collectors.*;

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
/**
 * {@code Config} is a smart and fast wrapper of a {@link Map Map&lt;String,
 * Object>} and provides easy access to all (hierarchical) entries via paths
 * like {@code "path.to.property.name"} in a safe way: if there is no value an
 * empty {@link Optional#empty() Optional} is returned to avoid
 * {@link NullPointerException}s. <br>
 * You can store all kind of types as value, especially {@code Map}s and
 * {@code List}s as structure elements, and return them in a type-safe way.
 * <p>
 * {@code Config} itself does not read configurations from sources. The Jackson
 * <a href=
 * "https://fasterxml.github.io/jackson-databind/javadoc/2.8/com/fasterxml/jackson/databind/ObjectMapper.html"
 * >ObjectMapper</a>, for exapmle, can read and convert any type to a
 * {@code Map}.
 * <p>
 * Use the {@link Builder Config.Builder} to customize your {@code Map} before
 * building an unmodifiable {@code Config}.
 * 
 * @version 1.1.2
 * 
 * @author <a href="mailto:dittmar.steiner@gmail.com">Dittmar Steiner</a>
 * 
 * @see Builder
 */
public class Config {

    private final Map<String, Object> root;
    private final Map<String, Object> cache;

    /**
     * Creates an unmodifialbe deep copy of the given {@link Map}.
     * 
     * @param root
     *            the underlying tree structure ({@code Map}s, {@code List}s and
     *            any type in a root {@code Map})
     * @since 1.0
     */
    public Config(Map<String, Object> root) {
        Objects.requireNonNull(root, "The root map cannot be null");

        Map<String, Object> cache = new LinkedHashMap<>();
        this.root = unmodifiable(root, "", cache);
        this.cache = Collections.unmodifiableMap(cache);
    }

    /**
     * 
     * @param properties
     * @since 1.1.2
     */
    public Config(Properties properties) {
        this(newBuilder(properties));
    }

    static Map<String, Object> newBuilder(Properties properties) {
        Objects.requireNonNull(properties, "The properties cannot be null");

        return new Builder(properties).root;
    }

    /**
     * Just to inspect what’s available.
     * 
     * @return all accessible paths
     */
    public Set<String> getPaths() {
        return cache.keySet();
    }

    /**
     * Access any available property by path.
     * 
     * <pre>{@code Config config = new Config(map);
     * 
     * // safe: is empty if not found
     * Optional<String> name = config.get("name");
     * 
     * // safe: uses the default orElse if not found
     * String proxyName = config.get("proxy.name").orElse("Dale");
     * 
     * // but you better know the type
     * int proxyPort = config.<Integer>get("proxy.port").orElse(9999);
     * 
     * // but this is your responsibility – this will throw a
     * // ClassCastException
     * int number = config.<Integer>get("name").orElse(42).intValue();
     * }</pre>
     * 
     * Hint: {@code config.get("")} (an empty {@code String}) returns the
     * internal unmodifiable {@code Map}.
     * 
     * @param propertyPath
     *            like {@code "path.to.property.name"}
     * @return an Optional of the requested type or {@link Optional#empty()} if
     *            not found
     * 
     * @throws NullPointerException if {@code propertyPath} is {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String propertyPath) {
        String path = clean(propertyPath);
        return !path.isEmpty()
                ? (Optional<T>) Optional.ofNullable(cache.get(path))
                : (Optional<T>) Optional.of(root);
    }

    static String clean(String input) {
        return Objects.nonNull(input) ? input.replaceAll("\\s+", "") : "";
    }

    static Map<String, Object> unmodifiable(Map<String, Object> map,
            String path, Map<String, Object> cache) {
        return map.entrySet().stream()
            .map(e -> wrap(e, path, cache))
            .filter(Objects::nonNull)
            .map(Map.Entry::getValue) // unwrap
            .collect(collectingAndThen(
                    toMap(Map.Entry::getKey, Map.Entry::getValue),
                    Collections::unmodifiableMap));
    }

    private static Map.Entry<String, Map.Entry<String, Object>> wrap(
            Map.Entry<String, Object> entry, String path,
            Map<String, Object> cache) {
        String pathExt = path.isEmpty() ? entry.getKey()
                : String.join(".", path, entry.getKey());

        Object unmodifiable = unmodifiable(entry.getValue(), pathExt, cache);

        if (unmodifiable == null) {
            return null;
        }

        // cache entry would look like {"proxy.map.name": {"name": "Harry"}}
        cache.put(pathExt, unmodifiable);

        // root entry would look like {"name": "Harry"}
        Map.Entry<String, Object> value = entry(entry.getKey(), unmodifiable);

        return entry(pathExt, value);
    }

    static <V> Map.Entry<String, V> entry(String key, V value) {
        return new SimpleImmutableEntry<String, V>(
                key, value);
    }

    static List<Object> unmodifiable(List<Object> list, String path,
            Map<String, Object> cache) {
        return list.stream().map(e -> unmodifiable(e, path, cache))
                .filter(Objects::nonNull)
                .collect(collectingAndThen(toList(),
                        Collections::unmodifiableList));
    }

    @SuppressWarnings("unchecked")
    static Object unmodifiable(Object value, String path,
            Map<String, Object> cache) {
        if (value == null) {
            return null;
        }

        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            return !map.isEmpty()
                    ? unmodifiable((Map<String, Object>) value, path, cache)
                    : null;
        }
        else if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            return !list.isEmpty()
                    ? unmodifiable((List<Object>) value, path, cache) : null;
        }

        return value;
    }

    /**
     * Since {@link Config} is unmodifiable this is a convenient way to prepare
     * the underlying modifiable {@link Map}.
     * 
     * <pre>{@code
     * Config config = new Config.Builder(map)
     *         // 1) will add or replace the target property
     *         .putOf("name", Optional.of("Cooper"))
     *         // 2) will add or replace with a Boolean
     *         .putOf("debug-mode", Optional.of(true))
     *         // 3) will add or replace with an Integer
     *         .putOf("proxy.threads", Optional.of(8))
     *         // 4) will keep the target property, does not nullify/delete
     *         .putOf("alias", null)
     *         // 5) will keep the target property, does not nullify/delete
     *         .putOf("alias", Optional.empty())
     *         // 6) will add or forcefully replace the target property
     *         .put("proxy.user", "Bob")
     *         // 7) will delete the target property (not only nullify)
     *         .put("proxy.user", null)
     *         // 8) will only add if target property if not yet present (does not overwrite)
     *         .putIfEmpty("proxy.port", 7777)
     *         .build();
     * }</pre>
     * 
     * @version 1.1.1
     * 
     * @author <a href="mailto:dsteiner@aptly.de">Dittmar Steiner</a>
     * 
     * @see Config
     */
    public static class Builder {
        private final Map<String, Object> root;

        /**
         * Creates a modifialbe deep copy of the given {@link Map}.
         * 
         * @param srcMap
         *            the underlying tree structure ({@code Map}s, {@code List}s and
         *            any type in a root {@code Map})
         * @since 1.0
         */
        public Builder(Map<String, Object> srcMap) {
            if (srcMap != null) {
                root = modifiable(srcMap);
            }
            else {
                root = new LinkedHashMap<String, Object>();
            }
        }

        /**
         * Creates a modifialbe &quot;deep&quot; copy of the given
         * {@link Properties}, following the dot separated paths;
         * 
         * @param srcProperties
         *            the underlying tree structure ({@code Map}s, {@code List}s
         *            and any type in a root {@code Map})
         * @since 1.1.1
         */
        public Builder(Properties  srcProperties) {
            root = new LinkedHashMap<String, Object>();

            if (srcProperties != null) {
                srcProperties.keySet()
                             .stream()
                             .forEach(k ->
                                put((String) k, srcProperties.get(k)));
            }
        }

        /**
         * @since 1.1.1
         */
        public Builder() {
            root = new LinkedHashMap<String, Object>();
        }

        /**
         * Similar to {@link Config#get(String)}.
         * 
         * @param propertyPath
         *            {@code "path.to.property.name"}
         * @return an Optional of the requested type, is empty if not found
         * 
         * @see Config#get(String)
         */
        public <T> Optional<T> get(String propertyPath) {
            return get(root, propertyPath);
        }

        @SuppressWarnings("unchecked")
        static <T> Optional<T> get(Map<String, Object> map,
                String propertyPath) {
            try {
                String path = clean(propertyPath);

                return !path.isEmpty()
                        ? Optional.ofNullable(get(map, parse(path), 0))
                        : (Optional<T>) Optional.of(map);
            }
            catch (Throwable th) {
                return Optional.empty();
            }
        }

        @SuppressWarnings("unchecked")
        private static <T> T get(Map<String, Object> map, String[] path,
                int index) {
            if (index < path.length - 1) {
                return get((Map<String, Object>) map.get(path[index]), path,
                        ++index);
            }

            return (T) map.get(path[index]);
        }

        private static String[] parse(String path) {
            String p = clean(path);
            if (p.isEmpty()) {
                throw new IllegalArgumentException("The path must not be empty");
            }

            return p.split("\\.");
        }

        /**
         * The hard way to set/replace a property, or delete if {@code value} is
         * {@code null}.
         * 
         * @param propertyPath
         *            {@code "path.to.property.name"}
         * @param value
         *            the value to set, {@code null} will delete the property
         * @return itself
         * 
         * @see #putOf(String, Optional)
         * @see #putIfEmpty(String, Object)
         */
        public <T> Builder put(String propertyPath, T value) {
            put(root, propertyPath, value);

            return this;
        }

        /**
         * Ideal for setting a property default value if it is not yet exist
         * some reason.
         * 
         * @param propertyPath
         *            {@code "path.to.property.name"}
         * @param value
         *            the value to set. If {@code null} nothing will happen
         * @return itself
         * 
         * @see #putOf(String, Optional)
         * @see #put(String, Object)
         */
        public <T> Builder putIfEmpty(String propertyPath, T value) {
            if (!get(propertyPath).isPresent() && value != null) {
                put(root, propertyPath, value);
            }

            return this;
        }

        /**
         * The prefered way to set a property value. It will not delete it
         * accidentally, because the parameter might come from a dynamic source,
         * like {@code Optional.ofNullable(System.getenv("NAME"))},
         * {@code Optional.ofNullable(System.getProperty("name"))} or
         * {@code public static void main(String... args)}.
         * 
         * @param propertyPath
         *            {@code "path.to.property.name"}
         * @param optional
         *            if {@code null} or empty, nothing will happen
         * @return itself
         * 
         * @see #putIfEmpty(String, Object)
         * @see #put(String, Object)
         */
        public <T> Builder putOf(String propertyPath, Optional<T> optional) {
            if (optional != null && optional.isPresent()) {
                put(root, propertyPath, optional.get());
            }

            return this;
        }

        static <T> Optional<T> put(Map<String, Object> map, String propertyPath,
                T value) {
            try {
                // avoid creating a map path without setting a value
                if (value == null
                        && !get(map, propertyPath).isPresent()) {
                    return Optional.empty();
                }

                String[] path = parse(propertyPath);
                String[] parent = Arrays.copyOf(path, path.length -1);
                String key = path[path.length -1];

                return Optional.of(put(map, parent, 0, key, value));
            }
            catch (Throwable th) {
                return Optional.empty();
            }
        }

        /**
         * 
         * @param map
         * @param path
         * @param index
         * @param key
         * @param value
         * @return for test reasons
         *         {@link Config.Builder#put(Map, String, Object)}
         */
        @SuppressWarnings("unchecked")
        private static <T> T put(Map<String, Object> map, String[] path,
                int index, String key, T value) {
            if (path.length == 0 || index == path.length) {
                map.put(key, value);
                return (T) map.get(key);
            }

            Map<String, Object> parent =
                    (Map<String, Object>) map.get(path[index]);
            if (parent == null) {
                parent = new LinkedHashMap<>();
                map.put(path[index], parent);
            }

            return put(parent, path, ++index, key, value);
        }

        /**
         * Finally builds an unmodifiable {@link Config}.
         * 
         * @return the unmodifiable {@link Config}
         */
        public Config build() {
            return new Config(root);
        }

        /**
         * Uses the {@link Config} to copy data from.
         * 
         * @param config
         *            to copy modifiable data from
         * @return a new {@link Builder}
         */
        public static Builder buildUpon(Config config) {
            return new Builder(config.root);
        }

        static Map<String, Object> modifiable(Map<String, Object> map) {
            Map<String, Object> copy =
                    new LinkedHashMap<String, Object>(map.size(), 1.0f);
            map.entrySet().stream().forEach(e ->
                copy.put(e.getKey(), modifiable(e.getValue())));

            return copy;
        }

        static List<Object> modifiable(List<Object> list) {
            List<Object> copy = list.stream().map(Builder::modifiable)
                    .filter(Objects::nonNull).collect(toList());

            return copy;
        }

        @SuppressWarnings("unchecked")
        static Object modifiable(Object value) {
            if (value == null) {
                return null;
            }

            if (value instanceof Map) {
                return modifiable((Map<String, Object>) value);
            }
            else if (value instanceof List) {
                return modifiable((List<Object>) value);
            }

            return value;
        }
    }
}
