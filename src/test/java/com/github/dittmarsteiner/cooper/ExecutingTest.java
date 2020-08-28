/*
 * Copyright (c) 2020, Dittmar Steiner <dittmar.steiner@gmail.com>
 * ISC License http://opensource.org/licenses/isc-license.txt
 */
package com.github.dittmarsteiner.cooper;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ExecutingTest {

    @Test
    void echoExample() {
        var what = "Hi, there!";
        var echoed =
                Executing.call("echo", "-n", what).value();
        assertEquals(what, echoed);
        assertNotSame(what, echoed);
    }

    @Test
    void base64Example() {
        assumeTrue(isInstalled("base64", "--version"));

        var there = "There";
        var base64 =
                Executing.call(() -> there, "base64", "-w", "0")
                         .value();
        var andBackAgain =
                Executing.call(() -> base64, "base64", "-d")
                         .value();
        assertEquals(there, andBackAgain);
        assertNotSame(there, andBackAgain);
    }

    @Test
    void withInputStream() {
        assumeTrue(
                isInstalled("base64", "--version") &&
                isInstalled("gzip", "--version")
        );

        var there = "datadatadatadatadatadatadatadatadatadatadatadatadatadatadata";
        var comressThenBase64 =
            Executing.call(
                    () -> new ByteArrayInputStream(there.getBytes(UTF_8)),
                    Executing::toString, // InputStream to String
                    "bash", "-c", "gzip -9 | base64 -w 0"
            ).value();

        var andBackAgain =
            Executing.call(
                    () -> Executing.toInputStream(comressThenBase64),
                    Executing::toString, // InputStream to String
                    "bash", "-c", "base64 -d | gzip -d"
            ).value();

        assertEquals(there, andBackAgain);
        assertNotSame(there, andBackAgain);
    }

    @Test
    void exampleEncryptDecrypt() {
        assumeTrue(isInstalled("openssl", "version"));
        var input = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        var password = "password";
        var enc = Executing.call(
                () -> input,
                Executing::toBase64Url,
                "bash", "-c", "gzip -9 | openssl aes-256-cbc -pbkdf2 -salt -k " + password
        );
        var urlEncoded = enc.value();

        var dec = Executing.call(
                () -> Executing.fromBase64Url(urlEncoded),
                Executing::toString,
                "bash", "-c", "openssl aes-256-cbc -pbkdf2 -d -k " + password + " | gzip -d"
        );
        var output = dec.value();
        assertEquals(input, output);
        assertNotSame(input, output);
    }

    @Test
    void asBase64UrlIsStableWhenStrippedTailingEquals() {
        var input = "x";
        var bin = new ByteArrayInputStream(input.getBytes(UTF_8));
        var base64 = Executing.toBase64Url(bin);
        var decoded = new String(Base64.getUrlDecoder().decode(base64));
        assertEquals(input, decoded);
    }

    boolean isInstalled(String... cmd) {
        return Executing.call(cmd).isValue();
    }
}