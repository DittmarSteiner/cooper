/*
 * Copyright (c) 2017, Dittmar Steiner <dittmar.steiner@gmail.com>
 * ISC License http://opensource.org/licenses/isc-license.txt
 */
package com.github.dittmarsteiner.cooper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * The motivation is to have a simple tool to resolve an application’s
 * parameters from the environment, the JVM args and at last the programm args:
 * <pre>{@code
 * $ export PORT=9999
 * $ java -Dproxy.name=Dale -jar app.jar -xvf file --no-proxy --alias Bob
 * }</pre>
 * 
 * It will resolve an option in the following order:
 * <ol>
 * <li>{@link System#getenv(String)}</li>
 * <li>{@link System#getProperty(String)}</li>
 * <li>any {@code String[]} like the {@code args} from in <br />
 * {@code public static void main(String... args)}</li>
 * </ol>
 * The first hit is returned as an {@link Optional}. If nothing could be
 * resolved the return value will be an {@link Optional#empty()}.
 * 
 * @version 2.0 – version 1.0 can be found
 * <a href="https://github.com/DittmarSteiner/OptionsResolver">here</a>.
 * 
 * @author <a href="mailto:dittmar.steiner@gmail.com">Dittmar Steiner</a>
 * 
 * @see #resolve(String, Character, String...)
 * @see #resolveFlag(String, Character, String...)
 */
public class OptionsResolver {

    /** 
     * Nothing to instatiate here.
     */
    private OptionsResolver() {}

    /**
     * Resolves a named value from the environment or commend line.
     * <pre>{@code public static void main(String... args) {
     *     Optional<String> name = OptionsResolver.resolve("name", 'n', args);
     *     if (name.isEmpty())
     *         System.out.println("No name arg");
     *     else
     *         System.out.println("Name: " + name.get());
     * 
     *     int port = OptionsResolver.resolve("PORT", 'p', args)
     *                .map(Integer::parseInt).orElse(8080);
     *     System.out.println("Port: " + port);
     * }}</pre>
     * <p>
     * The first hit will be returned.
     * 
     * @param name
     *            the name of the environment variable or System property or the
     *            command line arg like {@code "--name Cooper"}
     * @param letter
     *            a {@code char} to read like {@code 'n'} from
     *            {@code "-xvn Cooper"}. Must be the last and requires a value
     *            argument
     * @param args
     *            command line args
     * @return the value wrapped in an {@link Optional} or an empty
     *         {@code Optional} if the name, letter or value is not found
     */
    public static Optional<String> resolve(String name, Character letter,
            String... args) {
        // wraps the String value into an Optional or empty
        Function<String, Optional<String>> fn =
                (v) -> !v.isEmpty() ? Optional.of(v) : Optional.empty();

        return resolve(name, letter, fn, fn, true, args);
    }

    /**
     * Resolves a flag from the environment or commend line. <br />
     * From the envireonment an {@code "export verbose="} is {@code true}.
     * The same for VM parameters like {@code "-Dverbose"}. This meas everything
     * other than {@code "false"} or not present at all will be {@code true} <br />
     * On the command line it will ignore parameters, the pure presence is a
     * {@code true}.
     * <p>
     * The first hit will be returned.
     * <pre>{@code public static void main(String... args) {
     *     Optional<Boolean> verbose = OptionsResolver.resolve("verbose", 'v', args);
     *     System.out.println("I do not yet know what to say. " +
     *              "Some one else might decide on the (empty) Optional<Boolean>.");
     * 
     *     boolean force = OptionsResolver.resolve("force", 'f', args)
     *                     .orElse(false);
     *     System.out.println("Force mode: " + force);
     * }}</pre>
     * 
     * @param name
     *            the name of the environment variable or System property or the
     *            command line arg like {@code "--force"}
     * @param letter
     *            a {@code char} to read like {@code 'f'} from
     *            {@code "-vfn Cooper"} or just {@code "-f"}
     * @param args
     *            command line args
     * @return the {@code boolean} wrapped in an {@link Optional} or
     *         {@code Optional#empty()} if the name, letter or value is not found
     */
    public static Optional<Boolean> resolveFlag(String name, Character letter,
            String... args) {

        return resolve(name, letter,
            // true if the value is anything but "false" (even null or empty)
            (v) -> Optional.of(!"false".equalsIgnoreCase(v)),
            // always true because of the simple presence of the flag
            (v) -> Optional.of(true),
            false, args);
    }

    static <T> Optional<T> resolve(String name, Character letter,
            Function<String, Optional<T>> fnEnv,
            Function<String, Optional<T>> fnArgs,
            boolean hasValue, String... args) {

        if (Objects.isNull(args) ||
                (Objects.isNull(name) && Objects.isNull(letter))) {
            return Optional.empty();
        }

        // System env, property ////////////////////////////////////////////////
        if (Objects.nonNull(name)) {
            Optional<String> value = Arrays
                    .asList(System.getenv(name), System.getProperty(name))
                    .stream().filter(Objects::nonNull).findFirst();

            if (value.isPresent()) {
                return fnEnv.apply(value.get());
            }
        }

        // inspired by https://www.youtube.com/watch?v=2nup6Oizpcw&t=2717s
        List<String> argList = Arrays.asList(args);
        return IntStream.range(0, hasValue ? args.length -1 : args.length)
            .filter(i -> matches(name, letter, argList.get(i), hasValue))
            .map(i -> i + (hasValue ? 1 : 0)) // increment if has value arg
            .mapToObj(i -> fnArgs.apply(argList.get(i)))
            .findFirst().orElse(Optional.empty());
    }

    static boolean matches(String name, Character letter, String arg,
            boolean endsWith) {
        if (arg.equals("--" + name)) {
            return true;
        }

        if (Objects.nonNull(letter) && !arg.startsWith("--")
                && arg.startsWith("-")) {
            // if the letter requires a value argument it must be the last
            return endsWith ? arg.endsWith(letter.toString())
                    : arg.contains(letter.toString());
        }

        return false;
    }
}
