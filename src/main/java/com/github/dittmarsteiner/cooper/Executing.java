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
import static java.util.Objects.nonNull;

/**
 * <p>Utilizes {@link Process}.</p>
 * <p><b>Usage examples:</b></p>
 *
 * <pre>{@code
 *     // simple out
 *     var echoed = Executing.call(new String[]{"echo", "-n", "Hi, there!"})
 *                           .orElseThrow();
 *
 *     // simple in/out
 *     var there = "There";
 *     var base64 =
 *             Executing.call(there, new String[]{"base64", "-w", "0"})
 *                      .orElseThrow();
 *     var andBackAgain =
 *             Executing.call(base64, new String[]{"base64", "-d"})
 *                      .orElseThrow();
 *
 *     // advanced
 *     var there = "datadatadatadatadatadatadatadatadatadatadatadatadatadatadata";
 *     var comressThenBase64 =
 *         Executing.call(
 *             () -> new ByteArrayInputStream(there.getBytes(UTF_8)),
 *             Executing::asString, // InputStream to String
 *             new String[]{
 *                 "bash", "-c",
 *                 "gzip -9 | base64 -w 0"
 *             }
 *         ).orElseThrow();
 *
 *     var andBackAgain =
 *         Executing.call(
 *                 () -> Executing.asInputStream(comressThenBase64),
 *             Executing::asString, // InputStream to String
 *             new String[]{
 *                 "bash", "-c",
 *                 "base64 -d | gzip -d"
 *             }
 *         ).orElseThrow();
 * }
 * </pre>
 */
public class Executing {

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

    public static <T> Resulting<String> call(String[] command)
            throws RuntimeException {

        return call(null, command);
    }

    public static <T> Resulting<String> call(String input, String[] command)
            throws RuntimeException {

        return call(input,
                    Executing::toString,
                    command);
    }

    public static <T> Resulting<T> call(
            String input,
            Function<InputStream, T> transformer,
            String[] command)
                throws RuntimeException {

        Supplier<InputStream> in = nonNull(input)
                ? () -> new ByteArrayInputStream(input.getBytes(UTF_8))
                : null;
        return call(null, in, transformer, command);
    }

    public static <T> Resulting<T> call(
            Supplier<InputStream> in,
            Function<InputStream, T> transformer,
            String[] command)
                throws RuntimeException {
        return call(null, in, transformer, command);
    }

    public static <T> Resulting<T> call(
            Path workDir,
            Supplier<InputStream> input,
            Function<InputStream, T> transformer,
            String[] command)
                throws RuntimeException {
        Process process = null;
        try {
            process = new ProcessBuilder()
                    .command(command)
                    .directory(nonNull(workDir) ? workDir.toFile() : null)
                    .start();

            // in
            if (nonNull(input)) {
                try (
                     var in = input.get();
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

    public static String toString(InputStream in) throws RuntimeException {
        return new String(readAllBytes(in), UTF_8);
    }

    public static InputStream toInputStream(String value)
            throws RuntimeException {
        return new ByteArrayInputStream(value.getBytes(UTF_8));
    }

    public static String toBase64Url(InputStream in) throws RuntimeException {
        return getUrlEncoder().encodeToString(readAllBytes(in))
                              .replaceAll("[=]+$", "");
    }

    public static InputStream fromBase64Url(String encoded) {
        return new ByteArrayInputStream(getUrlDecoder().decode(encoded));
    }

    public static byte[] readAllBytes(InputStream in) throws RuntimeException {
        try {
            return in.readAllBytes();
        }
        catch (IOException e) {
            throw asRuntimeException(e);
        }
    }
}
