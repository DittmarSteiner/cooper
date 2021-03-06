/*
 * Copyright (c) 2017, Dittmar Steiner <dittmar.steiner@gmail.com>
 * ISC License http://opensource.org/licenses/isc-license.txt
 */
package com.github.dittmarsteiner.cooper;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.junit.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.dittmarsteiner.cooper.Config.Builder;

public class DemoTest {

    @Test
    public void testDemo() throws Exception {
        // these are the hard coded good defaults
        final String PROXY_USER = "Mark";
        final int PROXY_PORT = 7777;

        // now let’s begin
        // public static void main(String[] args) {...
        String[] args = {"--proxy-user", "Bob", "-a", "Coop", "--debug-mode"};

        // collect from the System.env, Systems.properties or args
        // this will find no 'name', so the Optional.isPresent is false
        Optional<String> name = OptionsResolver.resolve("name", 'n', args);
        // this will find an 'alias' 'Coop'
        Optional<String> alias = OptionsResolver.resolve("alias", 'a', args);
        // this will find a 'proxy-user' 'Bob'. Be careful and use 'orElse()'!
        String proxyUser = OptionsResolver.resolve("proxy-user", 'u', args).orElse(PROXY_USER);
        // now it gets fancy: the threads hold an Integer, parsed from a String, but might not be present!
        Optional<Integer> threads = OptionsResolver.resolve("proxy-threads", 't', args).map(Integer::getInteger);
        Optional<Boolean> debugMode = OptionsResolver.resolveFlag("debug-mode", 't', args);

        Config config = new Config.Builder(demo)
                // 1) will add or replace if present, but not nullify
                .putOf("name", name)
                // 2) will add or replace if present, but not nullify
                .putOf("alias", alias)
                // 3) will forcefully add or replace, even nullify!
                .put("proxy.user", proxyUser)
                // 4) will add or replace if present, but not nullify
                .putOf("proxy.threads", threads)
                // 5) will set only if not yet set in config, no overwrite
                .putIfEmpty("proxy.port", PROXY_PORT)
                // 6) will add or replace if present, but not nullify
                .putOf("debug-mode", debugMode)
                .build();

        assertThat(config.get("name").get(), is("Dale")); // not modified
        assertThat(config.get("alias").get(), is("Coop")); // from the args
        assertThat(config.get("proxy.user").get(), is("Bob")); // from the args
        assertThat(config.<Integer>get("proxy.port").get(), is(9999)); // not overwritten
        assertFalse(config.get("proxy.threads").isPresent()); // not set
        assertTrue(config.<Boolean>get("debug-mode").get()); // newly set true

        // but this is your responsibility – this will throw a ClassCastException
        try {
            @SuppressWarnings("unused")
            int number = config.<Integer>get("name").orElse(42).intValue();
            fail("The ship can’t sink!");
        }
        catch (ClassCastException e) {}

        // now let’s compare
//        System.out.println(mapper.writeValueAsString(demo));
//        System.out.println("--------------------------------------------------");
//        System.out.println(mapper.writeValueAsString(config.get("").get()));
    }

    /**
     * 
     */
    @Test
    public void testFirstSetDefaultsAndThenForceValues() {
        // hard-coded defaults
        final String NAME = "Windom";
        final String PROXY_NAME = "Bob";

        Builder builder = new Builder(root)
                // ensure defaults if not yet set
                .putIfEmpty("name", NAME)              // exists, no replace
                .putIfEmpty("proxy.name", PROXY_NAME); // will be added

        // not replaced because Cooper was already there
        assertThat(builder.get("name").get(), is("Cooper"));
        // added because the proxy had no name, yet
        assertThat(builder.get("proxy.name").get(), is("Bob"));

        // some environment parameters
        String name = "Laura";        // like System.getProperty("name")
        String proxyName = "Donna";   // like System.getProperty("proxy.name")
        String proxyAlias = "Maggie"; // like System.getProperty("proxy.alias")

        Config config = builder
                .put("name", name)
                .put("proxy.name", proxyName)
                .put("proxy.alias", proxyAlias)
                .build();

        // replaced
        assertThat(config.get("name").get(), is("Laura"));
        // replaced
        assertThat(config.get("proxy.name").get(), is("Donna"));
        // added
        assertThat(config.get("proxy.alias").get(), is("Maggie"));
    }

    /**
     * 
     */
    @Test
    public void testFirstSetDefaultsAndThenForceWithOptional() {
        // hard-coded defaults
        final String NAME = "Windom";
        final String PROXY_NAME = "Bob";

        Builder builder = new Builder(root)
                // ensure defaults if not yet exist
                .putIfEmpty("name", NAME)              // exists, no replace
                .putIfEmpty("proxy.name", PROXY_NAME); // will be added

        // not replaced because Cooper already exists
        assertThat(builder.get("name").get(), is("Cooper"));
        // added because the proxy had no name, yet
        assertThat(builder.get("proxy.name").get(), is("Bob"));

        // some environment parameters
        Optional<String> name = Optional.of("Laura");        // like resolve("name")
        Optional<String> proxyName = Optional.of("Donna");   // like resolve("proxy.name")
        Optional<String> proxyAlias = Optional.of("Maggie"); // like resolve("proxy.alias")
        Optional<Number> proxyPort = Optional.empty();       // like resolve("proxy.port")

        Config config = builder
                .putOf("name", name)              // will be replaced
                .putOf("proxy.name", proxyName)   // will be added
                .putOf("proxy.alias", proxyAlias) // will be added
                .putOf("proxy.port", proxyPort)   // will stay, because of empty
                .build();

        // replaced
        assertThat(config.get("name").get(), is("Laura"));
        // replaced
        assertThat(config.get("proxy.name").get(), is("Donna"));
        // added
        assertThat(config.get("proxy.alias").get(), is("Maggie"));
        // did not change
        assertThat(config.get("proxy.port").get(), is(9999));
    }

    /**
     * {@link Config} usage example.
     * <p>
     * Have a look a the files <br>
     * {@code src/test/resources/config.json} <br>
     * {@code src/test/resources/config.yml}
     */
    @Test
    public void testConfig() {
        files.forEach(file -> {
            ////////////////////////////////////////////////////////////////////
            // get your config resource file as a map
            Map<String, Object> map = load(file);

            // create an unmodifiable Config
            Config config = new Config(map);

            // read values from the Config
            Optional<String> on = config.get("name");
            String name = on.orElse("Default");
            assertThat(name, is("Cooper"));

            Optional<String> omn = config.get("missingName");
            String missingName = omn.orElse("DefaultName"); // uses else
            assertThat(missingName, is("DefaultName"));

            // going deeper in the tree
            Optional<Number> op = config.get("proxy.port"); // always us Number
            int port = op.orElse(80).intValue(); // no NPE or cast exception
            assertThat(port, is(9999));
            ////////////////////////////////////////////////////////////////////
        });
    }

    /**
     * {@link Builder Config.Builder} usage example.
     * <p>
     * Have a look a the files <br>
     * {@code src/test/resources/config.json} <br>
     * {@code src/test/resources/config.yml}
     */
    @Test
    public void testConfigBuilder() {
        files.forEach(file -> {
            ////////////////////////////////////////////////////////////////////
            // get your config resource file as a map
            Map<String, Object> map = load(file);

            // create a builder and customize values
            Config.Builder builder = new Config.Builder(map)
                    .put("name", "Bob")
                    .put("proxy.port", 80)
                    .put("notYetPresent", "New Value");

            // manipulate a list
            Optional<List<Integer>> op = builder.get("proxy.params");
            List<Integer> params = op.orElse(new ArrayList<Integer>());
            params.add(10);

            // build the unmodifiable config
            Config config = builder.build();

            // now let’s check if everything went well
            Optional<String> oName = config.get("name");
            assertThat(oName.get(), is("Bob"));

            Optional<Integer> oPort = config.get("proxy.port");
            assertThat(oPort.get(), is(80));

            Optional<String> oNewValue = config.get("notYetPresent");
            assertThat(oNewValue.get(), is("New Value"));

            Optional<List<Integer>> oParams = config.get("proxy.params"); // unmodifiable
            assertThat(oParams.get().size(), is(11));
            ////////////////////////////////////////////////////////////////////
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> load(String path) {
        try (BufferedReader reader =
                Files.newBufferedReader(Paths.get(path))) {
            ObjectMapper mapper = path.endsWith(".yml")
                        ? new ObjectMapper(new YAMLFactory())
                        : new ObjectMapper();
            return mapper.readValue(reader, Map.class);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    List<String> files = Arrays.asList(
            "src/test/resources/config.json", "src/test/resources/config.yml");

    Map<String, Object> root;
    Map<String, Object> demo;

    @Before
    public void initMap() {
        root = ConfigTest.createMap();

        demo = new LinkedHashMap<>();
        demo.put("name", "Dale");
        Map<String, Object> demoProxy = new LinkedHashMap<>();
        demoProxy.put("user", "David");
        demoProxy.put("port", 9999);
        demo.put("proxy", demoProxy);
    }
}
