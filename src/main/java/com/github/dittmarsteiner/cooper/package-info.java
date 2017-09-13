/*
 * Copyright (c) 2017, Dittmar Steiner <dittmar.steiner@gmail.com>
 * ISC License http://opensource.org/licenses/isc-license.txt
 */
/**
 * <i>Cooper</i> is a set of utilities, usually useful during an application’s
 * init phase. <br>
 * Since all classes have no external dependencies or depend on each other, feel
 * free to simply copy the desired {@code *.java} files into your project.
 * 
 * <h3>Complete example</h3>
 * <pre>{@code // some defaults
 * final int DEFAULT_NUMBER_OF_THREADS = Runtime.getRuntime().availableProcessors() * 4;
 * final int DEFAULT_PORT = 7777;
 * 
 * // collect some properties from the environment or commend line
 * // public static void main(String[] args)
 * Optional<String> optionalName = OptionsResolver.resolve("name", 'n', args);
 * int proxyThreads = OptionsResolver.resolve("proxy.threads", 't', args).map(Integer::parseInt)
 *     .orElse(DEFAULT_NUMBER_OF_THREADS);
 * 
 * // load your config as a Map
 * Reader reader = Files.newBufferedReader(Paths.get("config.yml");
 * ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
 * Map<String, Object> map = mapper.readValue(reader, Map.class);
 * 
 * // adjust the config
 * Config config = new Config.Builder(map)
 *     // will add or replace the target, but not nullify
 *     .putOf("name", optionalName)
 *     // will add the target, but not overwrite existing
 *     .putIfEmpty("alias", alias)
 *     // will add or replace the target; or delete it if proxyThreads is 'null'
 *     .put("proxy.threads", proxyThreads)
 *     .build();
 * 
 * // ----------------------------------------------------------------
 * // now read from your config
 * // name is empty if not found
 * Optional<String> name = config.get("name");
 * 
 * // uses the default 'orElse' if not found
 * String proxyName = config.get("proxy.name").orElse("Dale");
 * 
 * // note: you better know the type
 * int proxyPort = config.<Integer> get("proxy.port").orElse(9999);
 * 
 * // throws a ClassCastException
 * int someNumber = config.<Integer>get("name").orElse(42).intValue();
 * }</pre>
 * <p>
 * 
 * @author <a href="mailto:dittmar.steiner@gmail.com">Dittmar Steiner</a>
 */
package com.github.dittmarsteiner.cooper;
