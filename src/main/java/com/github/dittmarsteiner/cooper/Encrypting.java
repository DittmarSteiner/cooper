/*
 * Copyright (c) 2020, Dittmar Steiner <dittmar.steiner@gmail.com>
 * ISC License http://opensource.org/licenses/isc-license.txt
 */
package com.github.dittmarsteiner.cooper;

import java.io.InputStream;

import static com.github.dittmarsteiner.cooper.Executing.call;
import static com.github.dittmarsteiner.cooper.Resulting.asRuntimeException;
import static java.util.Objects.requireNonNull;

/**
 * <p>Encyption is not easy in Java, you can do things wrong in many places,
 * which weakens the encyption. But if you have installed {@code openssl} on
 * your machine (or in your container), this is a convenient way to encrypt and
 * decrypt using salted AES. Requires {@code gzip} installed as well.</p>
 *
 * <p>Since we are already using external commands, there is also
 * {@link Encrypting#sha1sum(String) sha1sum},
 * {@link Encrypting#sha256sum(String) sha256sum} and
 * {@link Encrypting#sha512sum(String) sha512sum} available.</p>
 *
 */
public class Encrypting {

    /**
     * <p>Gzip comressed, AES encyrpted and then
     * {@link Executing#toBase64Url(InputStream)} (without trailing {@code =}).
     * </p>
     * Implemented equivalent to
     * <pre>{@code
     * $ echo $data | bash -c "gzip -9 | openssl aes-256-cbc -pbkdf2 -salt -k $password"
     * }</pre>
     *
     * @param data
     * @param password
     * @return
     * @throws RuntimeException
     */
    public static String encrypt(String data, char[] password)
            throws RuntimeException {
        requireNonNull(data, "data cannot be null");
        requireNonNull(password, "password cannot be null");
        if (password.length == 0) {
            throw new IllegalArgumentException("password cannot be empty");
        }

        var enc = Executing.call(
                data,
                Executing::toBase64Url,
                new String[]{
                        "bash",
                        "-c",
                        "gzip -9 | openssl aes-256-cbc -pbkdf2 -salt -k " +
                        new String(password)
                }
        );

        return enc.orElseThrow();
    }

    /**
     * <p>Decoded from {@link Executing#fromBase64Url(String)}, AES decrypted and
     * then Gzip uncompressed. Input must be an output from
     * {@link Encrypting#encrypt(String, char[])}.</p>
     *
     * Implemented equivalent to
     * <pre>{@code
     * $ echo $bas64UrlDecoded | bash -c "openssl aes-256-cbc -pbkdf2 -k $password | gzip -d"
     * }</pre>
     *
     * @param bas64UrlEncoded
     * @param password
     * @return
     * @throws RuntimeException
     */
    public static String decrypt(String bas64UrlEncoded, char[] password)
            throws RuntimeException {
        requireNonNull(bas64UrlEncoded, "bas64UrlEncoded cannot be null");
        requireNonNull(password, "password cannot be null");
        if (password.length == 0) {
            throw new IllegalArgumentException("password cannot be empty");
        }

        var dec = call(
                () -> Executing.fromBase64Url(bas64UrlEncoded),
                Executing::toString,
                new String[]{
                        "bash",
                        "-c",
                        "openssl aes-256-cbc -pbkdf2 -d -k " + new String(password) +
                        " | gzip -d"
                }
        );

        return dec.orElseThrow();
    }

    public static String sha1sum(String value) throws RuntimeException {
        return sha("sha1sum", value);
    }

    public static String sha256sum(String value) throws RuntimeException {
        return sha("sha256sum", value);
    }

    public static String sha512sum(String value) throws RuntimeException {
        return sha("sha512sum", value);
    }

    static String sha(String sha, String value) throws RuntimeException {
        String[] sha1sum = { sha };

        try {
            var sum = call(value, sha1sum);
            return sum.orElseThrow()
                      .replaceAll("[ -]+$", "").strip();
        }
        catch (Throwable e) {
            throw asRuntimeException(e);
        }
    }
}
