/*
 * Copyright (c) 2017, Dittmar Steiner <dittmar.steiner@gmail.com>
 * ISC License http://opensource.org/licenses/isc-license.txt
 */
package com.github.dittmarsteiner.cooper;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.github.dittmarsteiner.cooper.Config.Builder;

public class ConfigBuilderTest {
    Map<String, Object> root;

    @Before
    public void initMap() {
        root = ConfigTest.createMap();
    }

    @Test
    public void testStaticSetProperty() {
        Optional<String> bar = Config.Builder.put(root, "foo", "bar");
        assertTrue(bar.isPresent());
        assertThat(bar.get(), is("bar"));
        // check in depth
        assertThat(Config.Builder.get(root, "foo").get(), is("bar"));

        Optional<String> empty = Config.Builder.put(root, "", "Empty");
        assertFalse(empty.isPresent());

        Optional<Number> parent = Config.Builder.put(root, "\nproxy\t. map\r. number   ", 2L);
        assertTrue(parent.isPresent());
        assertThat(parent.get(), is(2L));

        Optional<String> holder = Config.Builder.put(root, "planets.earth.countries.de.capital", "Berlin");
        assertTrue(holder.isPresent());
        assertThat(holder.get(), is("Berlin"));
        // re-check
        Optional<String> berlin = Config.Builder.get(root, "planets.earth.countries.de.capital");
        assertTrue(berlin.isPresent());
        assertThat(berlin.get(), is("Berlin"));
    }

    @Test
    public void testBuilderNullRoot() {
        try {
            @SuppressWarnings("unused")
            Builder builder = new Builder(null);
            fail("NullPointerException expected");
        }
        catch (NullPointerException e) { }
    }

    @Test
    public void testBuild() {
        Config config = new Builder(root).build();
        assertThat(config.get("name").get(), equalTo(root.get("name")));
        assertThat(config.get("booleanTrue").get(), equalTo(root.get("booleanTrue")));
        assertThat(config.get("MAX_DOUBLE").get(), equalTo(root.get("MAX_DOUBLE")));
        assertThat(config.get("proxy").get(), equalTo(root.get("proxy")));
        assertFalse(config.get("nullValue").isPresent());
        // we skip the rest…
    }

    @Test
    public void testSetReplace() {
        Builder builder = new Builder(root)
                .put("name", "New name")
                .put("booleanFalse", true)
                .put("      nullValue      ", "Not null");

        Config config = builder.build();
        assertThat(config.get("name").get(), equalTo("New name"));
        assertThat(config.get("booleanFalse").get(), equalTo(true));
        assertThat(config.get("nullValue").get(), equalTo("Not null"));
    }

    @Test
    public void testGetOptional() {
        Builder builder = new Builder(root);
        builder.putOf("proxy.port", Optional.of(8080));
        Config build = builder.build();
        Optional<Number> port = build.get("proxy.port");
        assertThat(port.get().intValue(), is(8080));
    }

    @Test
    public void testGetOptionalEmpty() {
        Builder builder = new Builder(root);
        builder.putOf("proxy.port", Optional.empty());
        Config build = builder.build();
        Optional<Number> port = build.get("proxy.port");
        // not modified
        assertThat(port.get().intValue(), is(9999));
    }

    @Test
    public void testSetNull() {
        Config config = new Builder(root).put("name", null).build();
        assertFalse(config.get("name").isPresent());
    }

    @Test
    public void testSetNew() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("key", "value");
        List<Object> list = new ArrayList<Object>();
        list.add("element");

        Map<String, Object> uselessRoot = new LinkedHashMap<String, Object>();
        Map<String, Object> uselessChild1 = new LinkedHashMap<String, Object>();
        Map<String, Object> uselessChild2 = new LinkedHashMap<String, Object>();
        List<Object> uselessList = new ArrayList<Object>();
        uselessChild2.put("uselessList", uselessList);
        uselessChild1.put("uselessChild2", uselessChild2);
        uselessRoot.put("uselessChild1", uselessChild1);

        Builder builder = new Builder(root)
                .put("something", 1L)
                .put("flag", true)
                .put("map", map)
                .put("list", list)
                .put("emptyMap", new LinkedHashMap<String, Object>())
                .put("emptyList", new ArrayList<Object>())
                .put("uselessRoot", uselessRoot)
            ;

        Config config = builder.build();
        assertThat(config.get("something").get(), is(1L));
        assertThat(config.get("flag").get(), equalTo(true));
        assertThat(config.get("map").get(), equalTo(map));
        assertThat(config.get("list").get(), equalTo(list));
        assertFalse(config.get("emptyMap").isPresent());
        assertFalse(config.get("emptyList").isPresent());
        assertFalse(config
                .get("uselessRoot.uselessChild1.uselessChild2.uselessList")
                .isPresent());
        assertFalse("Parameter has been modified!", uselessRoot.isEmpty());
    }

    @Test
    public void testSetWithLeadingTrailingBlanks() {
        Builder builder = new Builder(root);
        assertThat(builder.get(" \t name      ").get(), equalTo("Cooper"));

        builder.put("    name ", "New name");
        assertThat(builder.get("     name \n").get(), equalTo("New name"));

        Config build = builder.build();
        assertThat(build.get(" \rname       ").get(), equalTo("New name"));
    }

    @Test
    public void testSetIfOptional() {
        Builder builder = new Builder(root)
                .putOf("proxy.map.name", Optional.of("Hawk"));
        assertThat(builder.get("proxy.map.name").get(), is("Hawk"));

        Config build = builder.build();
        assertThat(build.get("proxy.map.name").get(), is("Hawk"));
    }

    @Test
    public void testSetIfOptionalNoParentYet() {
        Builder builder = new Builder(root)
                .putOf("proxy.alias", Optional.of("Bob"));
        assertThat(builder.get("proxy.alias").get(), is("Bob"));

        Config build = builder.build();
        assertThat(build.get("proxy.alias").get(), is("Bob"));
    }

    @Test
    public void testSetIfOptionalNoParentYetAndIsEmpty() {
        Builder builder = new Builder(root)
                .putOf("proxy.alias", Optional.empty());
        assertFalse(builder.get("proxy.alias").isPresent());

        Config build = builder.build();
        assertFalse(build.get("proxy.alias").isPresent());
    }

    @Test
    public void testSetIfOptionalNoParentYetAndNull() {
        Builder builder = new Builder(root)
                .putOf("proxy.alias", null);
        assertFalse(builder.get("proxy.alias").isPresent());

        Config build = builder.build();
        assertFalse(build.get("proxy.alias").isPresent());
    }

    @Test
    public void testSetIfOptionalEmpty() {
        Builder builder = new Builder(root)
                .putOf("proxy.map.name", Optional.empty());
        assertThat(builder.get("proxy.map.name").get(), is("Harry"));

        Config build = builder.build();
        assertThat(build.get("proxy.map.name").get(), is("Harry"));
    }

    @Test
    public void testSetIfOptionalNull() {
        Builder builder = new Builder(root)
                .putOf("proxy.map.name", null);
        assertThat(builder.get("proxy.map.name").get(), is("Harry"));

        Config build = builder.build();
        assertThat(build.get("proxy.map.name").get(), is("Harry"));
    }

    @Test
    public void testSetIfOptionalEmptyNoParentYet() {
        Builder builder = new Builder(root)
                .putOf("proxy.alias", Optional.empty());
        assertFalse(builder.get("proxy.alias").isPresent());

        Config build = builder.build();
        assertFalse(build.get("proxy.alias").isPresent());
    }

    @Test
    public void testBuildUpon() {
        Config config = new Config(root);
        Config build = Builder.buildUpon(config).build();

        assertThat(build.get("name").get(), equalTo(config.get("name").get()));
        assertThat(build.get("booleanTrue").get(), equalTo(config.get("booleanTrue").get()));
        assertThat(build.get("MAX_DOUBLE").get(), equalTo(config.get("MAX_DOUBLE").get()));
        assertFalse(build.get("nullValue").isPresent());
        assertThat(build.get("proxy").get(), equalTo(config.get("proxy").get()));
        assertThat(build.get("proxy.port").get(), equalTo(config.get("proxy.port").get()));
        assertThat(build.get("proxy.params").get(), equalTo(config.get("proxy.params").get()));
    }

    @Test
    public void testModifiableMapOfStringObject() {
        Builder builder = new Builder(root);
        Optional<Map<String, Object>> proxy = builder.get("proxy");
        // modifying a contained Map
        proxy.get().put("key", "value");
        Config build = builder.build();
        assertThat(build.get("proxy.key").get(), equalTo("value"));
    }

    @Test
    public void testMapHasNotKey() {
        Builder builder = new Builder(root).put("proxy.noValue", null);
        Optional<Map<String, Object>> proxy = builder.get("proxy");
        // did not even add the key
        assertFalse(proxy.get().containsKey("noValue"));

        // double check
        Config build = builder.build();
        proxy = build.get("proxy");
        assertFalse(proxy.get().containsKey("noValue"));
    }

    @Test
    public void testModifiableListObject() {
        Builder builder = new Builder(root);
        Optional<List<Object>> proxyParams = builder.get("proxy.params");
        // get the list to modify
        List<Object> proxyParamsList = proxyParams.get();
        proxyParamsList.set(0, -1000);

        Config build = builder.build();
        // get the list again to check if modified
        Optional<List<Number>> params = build.get("proxy.params");
        assertThat(params.get().get(0), equalTo(-1000));
    }
}
