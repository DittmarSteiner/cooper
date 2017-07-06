/*
 * -----------------------------------------------------------------------------
 * ISC License http://opensource.org/licenses/isc-license.txt
 * -----------------------------------------------------------------------------
 * Copyright (c) 2017, Dittmar Steiner <dittmar.steiner@gmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.github.dittmarsteiner.cooper;

import static java.util.stream.Collectors.toList;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
 * @version 1.0
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
     */
    public Config(Map<String, Object> root) {
        Objects.requireNonNull(root, "The root map cannot be null");

        Map<String, Object> cache = new HashMap<>();
        this.root = unmodifiable(root, "", cache);
        this.cache = Collections.unmodifiableMap(cache);
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
        Map<String, Object> copy = new HashMap<>(map.size(), 1.0f);
        map.entrySet().stream()
            .map(e -> wrap(e, path, cache))
            .filter(Objects::nonNull)
            .map(Map.Entry::getValue) // unwrap
            .forEach(e -> copy.put(e.getKey(), e.getValue()));

        return !copy.isEmpty() ? Collections.unmodifiableMap(copy) : null;
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

        // would look like {"name": "Harry"}
        Map.Entry<String, Object> value = new SimpleEntry<String, Object>(
                entry.getKey(), unmodifiable);
        cache.put(pathExt, value.getValue());

        // would look like {"proxy.map.name": {"name": "Harry"}}
        return new SimpleEntry<String, Map.Entry<String, Object>>(pathExt,
                value);
    }

    static List<Object> unmodifiable(List<Object> list, String path,
            Map<String, Object> cache) {
        List<Object> copy = list.stream().map(e -> unmodifiable(e, path, cache))
                .filter(Objects::nonNull).collect(toList());

        return !copy.isEmpty() ? Collections.unmodifiableList(copy) : null;
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
     *         .setOf("name", Optional.of("Cooper"))
     *         // 2) will add or replace with a Boolean
     *         .setOf("debug-mode", Optional.of(true))
     *         // 3) will add or replace with an Integer
     *         .setOf("proxy.threads", Optional.of(8))
     *         // 4) will keep the target property, does not nullify/delete
     *         .setOf("alias", null)
     *         // 5) will keep the target property, does not nullify/delete
     *         .setOf("alias", Optional.empty())
     *         // 6) will add or forcefully replace the target property
     *         .set("proxy.user", "Bob")
     *         // 7) will delete the target property (not only nullify)
     *         .set("proxy.user", null)
     *         // 8) will only add if target property if not yet present (does not overwrite)
     *         .setIfEmpty("proxy.port", 7777)
     *         .build();
     * }</pre>
     * 
     * @version 1.0
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
         * @param root
         *            the underlying tree structure ({@code Map}s, {@code List}s and
         *            any type in a root {@code Map})
         */
        public Builder(Map<String, Object> root) {
            Objects.requireNonNull(root, "The root map cannot be null");
            this.root = modifiable(root);
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
         * @see #setOf(String, Optional)
         * @see #setIfEmpty(String, Object)
         */
        public <T> Builder set(String propertyPath, T value) {
            set(root, propertyPath, value);

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
         * @see #setOf(String, Optional)
         * @see #set(String, Object)
         */
        public <T> Builder setIfEmpty(String propertyPath, T value) {
            if (!get(propertyPath).isPresent() && value != null) {
                set(root, propertyPath, value);
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
         * @see #setIfEmpty(String, Object)
         * @see #set(String, Object)
         */
        public <T> Builder setOf(String propertyPath, Optional<T> optional) {
            if (optional != null && optional.isPresent()) {
                set(root, propertyPath, optional.get());
            }

            return this;
        }

        static <T> Optional<T> set(Map<String, Object> map, String propertyPath,
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

                return Optional.of(set(map, parent, 0, key, value));
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
         *         {@link Config.Builder#set(Map, String, Object)}
         */
        @SuppressWarnings("unchecked")
        private static <T> T set(Map<String, Object> map, String[] path,
                int index, String key, T value) {
            if (path.length == 0 || index == path.length) {
                map.put(key, value);
                return (T) map.get(key);
            }

            Map<String, Object> parent =
                    (Map<String, Object>) map.get(path[index]);
            if (parent == null) {
                parent = new HashMap<>();
                map.put(path[index], parent);
            }

            return set(parent, path, ++index, key, value);
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
                    new HashMap<String, Object>(map.size(), 1.0f);
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
