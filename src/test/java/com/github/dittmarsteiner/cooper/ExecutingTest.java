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
                Executing.call(new String[]{"echo", "-n", what}).value();
        assertEquals(what, echoed);
        assertNotSame(what, echoed);
    }

    @Test
    void base64Example() {
        assumeTrue(isInstalled(new String[]{"base64", "--version"}));

        var there = "There";
        var base64 =
                Executing.call(there, new String[]{"base64", "-w", "0"})
                         .value();
        var andBackAgain =
                Executing.call(base64, new String[]{"base64", "-d"})
                         .value();
        assertEquals(there, andBackAgain);
        assertNotSame(there, andBackAgain);
    }

    @Test
    void withInputStream() {
        assumeTrue(
                isInstalled(new String[]{"base64", "--version"}) &&
                isInstalled(new String[]{"gzip", "--version"})
        );

        var there = "datadatadatadatadatadatadatadatadatadatadatadatadatadatadata";
        var comressThenBase64 =
            Executing.call(
                    () -> new ByteArrayInputStream(there.getBytes(UTF_8)),
                    Executing::toString, // InputStream to String
                    new String[]{
                    "bash", "-c",
                    "gzip -9 | base64 -w 0"
                }
            ).value();

        var andBackAgain =
            Executing.call(
                    () -> Executing.toInputStream(comressThenBase64),
                    Executing::toString, // InputStream to String
                    new String[]{
                    "bash", "-c",
                    "base64 -d | gzip -d"
                }
            ).value();

        assertEquals(there, andBackAgain);
        assertNotSame(there, andBackAgain);
    }

    boolean isInstalled(String[] cmd) {
        return Executing.call(cmd).isValue();
    }

    @Test
    void exampleEncryptDecrypt() throws Throwable {
        var input = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        var password = "password";
        var enc = Executing.call(
                input,
                Executing::toBase64Url,
                 new String[]{"bash", "-c", "gzip -9 | openssl aes-256-cbc -pbkdf2 -salt -k " + password}
        );
        var urlEncoded = enc.value();

        var dec = Executing.call(
                () -> Executing.fromBase64Url(urlEncoded),
                Executing::toString,
                 new String[]{"bash", "-c", "openssl aes-256-cbc -pbkdf2 -d -k " + password + " | gzip -d"}
        );
        var output = dec.value();
        assertEquals(input, output);
    }

    @Test
    void asBase64UrlIsStableWhenStrippedTailingEquals() {
        var input = "x";
        var bin = new ByteArrayInputStream(input.getBytes(UTF_8));
        var base64 = Executing.toBase64Url(bin);
        var decoded = new String(Base64.getUrlDecoder().decode(base64));
        assertEquals(input, decoded);
    }
}