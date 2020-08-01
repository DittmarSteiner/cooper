/*
 * Copyright (c) 2020, Dittmar Steiner <dittmar.steiner@gmail.com>
 * ISC License http://opensource.org/licenses/isc-license.txt
 */
package com.github.dittmarsteiner.cooper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptingTest {

    @Test
    void encryptDecryptExample() {
        var input = "x".repeat(100_000);
        char[] password = "password".toCharArray();
        var encypted = Encrypting.encrypt(input, password);
        var decypted = Encrypting.decrypt(encypted, password);
        assertEquals(input, decypted);
        assertNotSame(input, decypted);
    }

    @Test
    void sha1sumExample() {
        var sum = Encrypting.sha1sum("xxxxxxxxxx");
        assertEquals("ff9ee043d85595eb255c05dfe32ece02a53efbb2", sum);
    }

    @Test
    void sha256sumExample() {
        var sum = Encrypting.sha256sum("xxxxxxxxxx");
        assertEquals("fc11d6f28e59d3cc33c0b14ceb644bf0902ebd63d61218dffe9e7dac7c254542", sum);
    }

    @Test
    void sha512sumExample() {
        var sum = Encrypting.sha512sum("xxxxxxxxxx");
        assertEquals("099d63f8dc9b3d22f013ca7f831ba2c8ce97b51736ddeccfdca61b3da5b535458697b083a196b05c38aacdc3f956f20e2aac571c15b0bcc3fcb57cf8cb056f91", sum);
    }
}