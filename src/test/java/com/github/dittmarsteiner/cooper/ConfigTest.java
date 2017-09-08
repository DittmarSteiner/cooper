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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.github.dittmarsteiner.cooper.Config;

public class ConfigTest {
    Map<String, Object> root;

    @Before
    public void initMap() {
        root = createMap();
    }

    public static Map<String, Object> createMap() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("name", "Cooper");

        root.put("booleanFalse", false);
        root.put("booleanTrue", true);

        root.put("MAX_INTEGER", Integer.MAX_VALUE);
        root.put("MAX_LONG", Long.MAX_VALUE);
        root.put("MAX_FLOAT", Float.MAX_VALUE);
        root.put("MAX_DOUBLE", Double.MAX_VALUE);

        root.put("nullValue", null);

        Map<String, Object> proxy = new LinkedHashMap<>();
        proxy.put("port", 9999);
        List<Object> params = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        proxy.put("params", params);
        Map<String, Object> proxyMap = new LinkedHashMap<>();
        proxyMap.put("number", 1L);
        proxyMap.put("name", "Harry");
        proxy.put("map", proxyMap);

        root.put("proxy", proxy);

        return root;
    }

    // TODO Javadoc
    // TODO README.md

    @Test
    public void testConfigNullRoot() {
        try {
            @SuppressWarnings("unused")
            Config config = new Config(null);
            fail("NullPointerException expected");
        }
        catch (NullPointerException e) { }
    }

    @Test
    public void testConfigGetRoot() {
        Config config = new Config(root);
        root.remove("nullValue"); // 'null' value causes key to be removed during copying
        assertThat(root, equalTo(config.get("").get()));
    }

    @Test
    public void testGetPaths() {
        Config config = new Config(root);
        Set<String> paths = config.getPaths();
        assertTrue(paths.contains("name"));
        assertTrue(paths.contains("booleanFalse"));
        assertTrue(paths.contains("booleanTrue"));
        assertTrue(paths.contains("MAX_INTEGER"));
        assertTrue(paths.contains("MAX_LONG"));
        assertTrue(paths.contains("MAX_FLOAT"));
        assertTrue(paths.contains("MAX_DOUBLE"));
        assertFalse(paths.contains("nullValue"));
        assertTrue(paths.contains("proxy"));
        assertTrue(paths.contains("proxy.map"));
        assertTrue(paths.contains("proxy.map.name"));
        assertTrue(paths.contains("proxy.map.number"));
        assertTrue(paths.contains("proxy.params"));
        assertTrue(paths.contains("proxy.port"));
    }

    @Test
    public void testGetAll() {
        Config config = new Config(root);

        Optional<String> string = config.get("name");
        assertThat(string.get(), equalTo("Cooper"));

        Optional<Boolean> booleanFalse = config.get("booleanFalse");
        assertFalse(booleanFalse.get());

        Optional<Boolean> booleanTrue = config.get("booleanTrue");
        assertTrue(booleanTrue.get());

        Optional<Number> maxInteger = config.get("MAX_INTEGER");
        assertThat(maxInteger.get().intValue(), equalTo(Integer.MAX_VALUE));

        Optional<Number> maxLong = config.get("MAX_LONG");
        assertThat(maxLong.get().longValue(), equalTo(Long.MAX_VALUE));

        Optional<Number> maxFloat = config.get("MAX_FLOAT");
        assertThat(maxFloat.get().floatValue(), equalTo(Float.MAX_VALUE));

        Optional<Number> maxDouble = config.get("MAX_DOUBLE");
        assertThat(maxDouble.get().doubleValue(), equalTo(Double.MAX_VALUE));

        Optional<Map<String, Object>> proxy = config.get("proxy");
        assertTrue(proxy.get() instanceof Map);

        Optional<Number> proxyPort = config.get("proxy.port");
        assertThat(proxyPort.get().intValue(), equalTo(9999));

        List<Object> params = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        Optional<List<Object>> proxyParams = config.get("proxy.params");
        assertThat(proxyParams.get(), equalTo(params));
    }

    @Test
    public void testGetNotFound() {
        Config config = new Config(root);

        Optional<String> notFound = config.get("notFound");
        assertThat(notFound.orElse("Not Found"), equalTo("Not Found"));

        Optional<Number> proxyPort2 = config.get("proxy.port2");
        assertThat(proxyPort2.orElse(5555).intValue(), equalTo(5555));

        Optional<Number> paramBeyond = config.get("proxy.params[9999]");
        assertFalse(paramBeyond.isPresent());
    }

    @Test
    public void testGetCastWrongType() {
        Config config = new Config(root);

        try {
            Optional<Number> notANumber = config.get("name");
            @SuppressWarnings("unused")
            Number number = notANumber.get();
            fail("ClassCastException expected");
        }
        catch (ClassCastException e) { }
    }

    @Test
    public void testGetCastWrongTypeCaught() {
        Config config = new Config(root);

        int number = config.<Integer>get("not-available").orElse(-1).intValue();
        assertThat(number, is(-1));
    }

    @Test
    public void testTryToModifyRetrievedMap() {
        Config config = new Config(root);
        Optional<Map<String, Object>> proxy = config.get("proxy");

        try {
            proxy.get().put("key", "value");
            fail("UnsupportedOperationException exected");
        }
        catch (UnsupportedOperationException e) { }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUnmodifiableMapOfStringObject() {
        Map<String, Object> cache = new LinkedHashMap<>();
        Map<String, Object> root = Config.unmodifiable(this.root, "", cache);

        try {
            root.put("name", "New name");
            fail("UnsupportedOperationException expected");
        }
        catch (UnsupportedOperationException e) { }

        Map<String, Object> proxy = (Map<String, Object>) root.get("proxy");
        try {
            proxy.put("port", 8080);
            fail("UnsupportedOperationException expected");
        }
        catch (UnsupportedOperationException e) { }

        List<Object> proxyParams = (List<Object>) proxy.get("params");
        try {
            proxyParams.remove(0);
            fail("UnsupportedOperationException expected");
        }
        catch (UnsupportedOperationException e) { }
    }
}
