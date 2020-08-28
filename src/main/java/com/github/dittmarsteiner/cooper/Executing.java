/*
 * Copyright (c) 2020, Dittmar Steiner <dittmar.steiner@gmail.com>
 * ISC License http://opensource.org/licenses/isc-license.txt
 */
package com.github.dittmarsteiner.cooper;

import java.io.*;
import java.nio.file.Path;
import java.util.function.*;

import static com.github.dittmarsteiner.cooper.Resulting.asRuntimeException;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Base64.*;
import static java.util.Objects.*;

/**
 * <p>Utilizes {@link Process}.</p> For examples see methods.
 */
public class Executing {

    /**
     * Method params with {@code Supplier<String>} and {@code
     * Supplier<InputStream>} would clash – that’s why.
     */
    @FunctionalInterface
    public interface StringSupplier extends Supplier<String> {}

    /**
     * Method params with {@code Supplier<String>} and {@code
     * Supplier<InputStream>} would clash – that’s why.
     */
    @FunctionalInterface
    public interface InputStreamSupplier extends Supplier<InputStream> {}

    /**
     * A <i>marker</i> {@link Exception} without any stack trace, cause or
     * supressed exceptions. For example {@code $ gzip -c pattern} or {@code $
     * gzip -q 'pattern'} might return {@code 1} which is semantically correct
     * and we do not need further details.
     */
    public static class NonZeroExitCodeException extends RuntimeException {

        final int exitCode;

        public NonZeroExitCodeException(String message, int exitCode) {
            super(message, null, true, false);
            this.exitCode = exitCode;
        }

        public int getExitCode() {
            return exitCode;
        }
    }

    /**
     * <pre>{@code
     * // simple out
     * String echo = Executing.call("echo", "-n", "Hi, there!").value();
     * // optional
     * String tryEcho =
     *     Executing.call("echo", "-n", "Hi, there!")
     *              .optValue()
     *              .orElse("<no echo>");
     * }
     * </pre>
     * @param command
     * @return
     * @throws RuntimeException
     */
    public static Resulting<String> call(String... command)
            throws RuntimeException {

        return call(null, command);
    }

    /**
     * <pre>{@code
     * // simple in/out
     * String base64 =
     *     Executing.call(() -> "xxxxxxxxxx",
     *                    "base64", "-w", "0")
     *              .value();
     * }
     * </pre>
     *
     * @param input
     * @param command
     * @return
     * @throws RuntimeException
     */
    public static Resulting<String> call(
            StringSupplier input,
            String... command)
                throws RuntimeException {

        return call(input,
                    Executing::toString,
                    command);
    }

    /**
     * <pre>{@code
     * String base64 =
     *     Executing.call(() -> "xxxxxxxxxx",
     *                    Base64.getUrlDecoder()::decode
     *                    "gzip", "-9")
     *              .value();
     * }
     * </pre>
     *
     * @param input
     * @param transformer
     * @param command
     * @param <T>
     * @return
     * @throws RuntimeException
     */
    public static <T> Resulting<T> call(
            StringSupplier input,
            Function<InputStream, T> transformer,
            String... command)
                throws RuntimeException {
        InputStreamSupplier in = nonNull(input) && nonNull(input.get())
                ? () -> toInputStream(input.get())
                : null;
        return call(null, in, transformer, command);
    }

    /**
     * <pre>{@code
     * try (var in = getInputStream(path)) {
     *     String base64 =
     *         Executing.call(() -> in,
     *                        Base64.getUrlDecoder()::decode
     *                        "gzip", "-9")
     *                  .value();
     * }
     * }
     * </pre>
     *
     * @param in
     * @param transformer
     * @param command
     * @param <T>
     * @return
     * @throws RuntimeException
     */
    public static <T> Resulting<T> call(
            InputStreamSupplier in,
            Function<InputStream, T> transformer,
            String... command)
                throws RuntimeException {
        return call(null, in, transformer, command);
    }

    /**
     * <pre>{@code
     * try (var in = getInputStream(path)) {
     *     String base64 =
     *         Executing.call(path.getParent()
     *                        () -> in,
     *                        Base64.getUrlDecoder()::decode
     *                        "gzip", "-9")
     *                  .value();
     * }
     * }
     * </pre>
     *
     * @param workDir
     * @param input
     * @param transformer
     * @param command
     * @param <T>
     * @return
     * @throws RuntimeException
     */
    public static <T> Resulting<T> call(
            Path workDir,
            InputStreamSupplier input,
            Function<InputStream, T> transformer,
            String... command)
                throws RuntimeException {
        Process process = null;
        try {
            process = new ProcessBuilder()
                    .command(command)
                    .directory(nonNull(workDir) ? workDir.toFile() : null)
                    .start();

            // in
            if (nonNull(input) && nonNull(input.get())) {
                try (var in = input.get();
                     var out = process.getOutputStream() // also close this one!
                    ) {
                    in.transferTo(out);
                }
            }

            // out && err
            T result;
            String error;
            try (
                 var out = process.getInputStream();
                 var err = process.getErrorStream()
            ) {
                result = transformer.apply(out);
                error = toString(err);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return Resulting.ofError(new NonZeroExitCodeException(
                        String.join(" ", command) +
                        ": " + exitCode + " - " + error.strip(),
                        exitCode
                ));
            }

            return Resulting.ofValue(result);
        }
        catch (Throwable e) {
            return Resulting.ofError(e);
        }
        finally {
            if (nonNull(process)) {
                process.destroy();
            }
        }
    }

    /**
     * Simple transformer.
     *
     * @param in
     * @return
     * @throws RuntimeException
     * @see #call(StringSupplier, Function, String...)
     * @see #call(InputStreamSupplier, Function, String...)
     * @see #call(Path, InputStreamSupplier, Function, String...)
     */
    public static String toString(InputStream in) throws RuntimeException {
        return new String(readAllBytes(in), UTF_8);
    }

    public static InputStream toInputStream(String value)
            throws RuntimeException {
        return new ByteArrayInputStream(value.getBytes(UTF_8));
    }

    /**
     * URL-save Base64 without trailing {@code "="}.
     * @param in
     * @return
     * @throws RuntimeException
     */
    public static String toBase64Url(InputStream in) throws RuntimeException {
        return getUrlEncoder().encodeToString(readAllBytes(in))
                              .replaceAll("[=]+$", "");
    }

    /**
     * Reverse version of {@link #toBase64Url(InputStream)}.
     * @param encoded
     * @return
     */
    public static InputStream fromBase64Url(String encoded) {
        return new ByteArrayInputStream(getUrlDecoder().decode(encoded));
    }

    /**
     * Unchecked wrapper for {@link InputStream#readAllBytes()}.
     * @param in
     * @return
     * @throws RuntimeException
     */
    public static byte[] readAllBytes(InputStream in) throws RuntimeException {
        try {
            return in.readAllBytes();
        }
        catch (IOException e) {
            throw asRuntimeException(e);
        }
    }

    public static boolean isNonZeroExit(Throwable e) {
        requireNonNull(e, "Throwable accont be null");

        return e instanceof NonZeroExitCodeException;
    }
}
