/*
 * Copyright (c) 2017, Dittmar Steiner <dittmar.steiner@gmail.com>
 * ISC License http://opensource.org/licenses/isc-license.txt
 */
package com.github.dittmarsteiner.cooper;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ OptionsResolver.class })
public class OptionsResolverTest {

    @Test
    public void testAllNullArgs() {
        PowerMockito.mockStatic(System.class);
        when(System.getenv("name")).thenReturn(null);
        when(System.getProperty("name")).thenReturn(null);

        String[] args = null;
        String nullArgs = OptionsResolver.resolve("name", 'n', args).orElse("default");
        assertThat(nullArgs, is("default"));
        nullArgs = OptionsResolver.resolve("name", null, args).orElse("default");
        assertThat(nullArgs, is("default"));
        nullArgs = OptionsResolver.resolve(null, 'n', args).orElse("default");
        assertThat(nullArgs, is("default"));

        String arg = null;
        String nullArg = OptionsResolver.resolve("name", 'n', arg).orElse("default");
        assertThat(nullArg, is("default"));
        nullArg = OptionsResolver.resolve("name", null, arg).orElse("default");
        assertThat(nullArg, is("default"));
        nullArg = OptionsResolver.resolve(null, 'n', arg).orElse("default");
        assertThat(nullArg, is("default"));
    }

    @Test
    public void testResolveEnvUsingOrElse() {
        PowerMockito.mockStatic(System.class);
        when(System.getenv("name")).thenReturn(null);
        when(System.getProperty("name")).thenReturn(null);

        String orElse = OptionsResolver.resolve("name", null, new String[]{}).orElse("default");
        assertThat(orElse, is("default"));
    }

    @Test
    public void testResolveEnv() {
        PowerMockito.mockStatic(System.class);
        when(System.getenv("name")).thenReturn("value");
        when(System.getProperty("name")).thenReturn(null);

        String orElse = OptionsResolver.resolve("name", null,
                new String[]{}).orElse("default");
        assertThat(orElse, is("value"));
    }

    @Test
    public void testResolveProp() {
        PowerMockito.mockStatic(System.class);
        when(System.getenv("name")).thenReturn(null);
        when(System.getProperty("name")).thenReturn("value");

        String orElse = OptionsResolver.resolve("name", null,
                new String[]{}).orElse("default");
        assertThat(orElse, is("value"));
    }

    @Test
    public void testResolveArg() {
        PowerMockito.mockStatic(System.class);
        when(System.getenv("name")).thenReturn(null);
        when(System.getProperty("name")).thenReturn(null);

        String orElse = OptionsResolver.resolve("name", null,
                new String[]{"--name", "value"}).orElse("default");
        assertThat(orElse, is("value"));
    }

    @Test
    public void testResolveLetter() {
        PowerMockito.mockStatic(System.class);
        when(System.getenv("name")).thenReturn(null);
        when(System.getProperty("name")).thenReturn(null);

        String orElse = OptionsResolver.resolve("name", 'n',
                new String[]{"-n", "value"}).orElse("default");
        assertThat(orElse, is("value"));
    }

    @Test
    public void testResolveArgVsLetter() {
        PowerMockito.mockStatic(System.class);
        when(System.getenv("name")).thenReturn(null);
        when(System.getProperty("name")).thenReturn(null);

        String orElse = OptionsResolver.resolve("name", 'n',
                new String[]{"--name", "value", "-n", "xxx"}).orElse("default");
        assertThat(orElse, is("value")); // --name was first
    }

    @Test
    public void testResolveLetterVsArg() {
        PowerMockito.mockStatic(System.class);
        when(System.getenv("name")).thenReturn(null);
        when(System.getProperty("name")).thenReturn(null);

        String orElse = OptionsResolver.resolve("name", 'n',
                new String[]{"-n", "flag", "--name", "arg"}).orElse("default");
        assertThat(orElse, is("flag")); // -n was first
    }

    @Test
    public void testResolveEnvFirst() {
        PowerMockito.mockStatic(System.class);
        when(System.getenv("name")).thenReturn("sysEnv");
        when(System.getProperty("name")).thenReturn(null);

        String orElse = OptionsResolver.resolve("name", 'n',
                new String[]{"-n", "flag", "--name", "arg"}).orElse("default");
        assertThat(orElse, is("sysEnv")); // env was first
    }

    @Test
    public void testResolvePropertySecond() {
        PowerMockito.mockStatic(System.class);
        when(System.getenv("name")).thenReturn(null);
        when(System.getProperty("name")).thenReturn("sysProp");

        String orElse = OptionsResolver.resolve("name", 'n',
                new String[]{"-n", "flag", "--name", "arg"}).orElse("default");
        assertThat(orElse, is("sysProp")); // sysProp was first
    }

    @Test
    public void testResolveStringNoValueArgument() {
        PowerMockito.mockStatic(System.class);
        when(System.getenv("something")).thenReturn(null);
        when(System.getProperty("something")).thenReturn(null);

        Optional<String> value1 = OptionsResolver.resolve(null, 'f',
                new String[]{"something", "-xfc"});
        assertFalse(value1.isPresent());

        // -f isn’t the char in -fxs, so the value fileName is not evaluated
        Optional<String> value2 = OptionsResolver.resolve(null, 'f',
                new String[]{"-fxs", "filename", "--something"});
        assertFalse(value2.isPresent());

        Optional<String> value3 = OptionsResolver.resolve("something", null,
                new String[]{"-s", "--nothing", "--something"});
        assertFalse(value3.isPresent());
    }

    @Test
    public void testResolveStringEmptyArgument() {
        PowerMockito.mockStatic(System.class);
        when(System.getenv("something")).thenReturn("");
        when(System.getProperty("something")).thenReturn("");

        Optional<String> value = OptionsResolver.resolve("something", 's',
                new String[]{"something", "", "-s", ""});
        assertFalse(value.isPresent());
    }

    @Test
    public void testResolveFlagNoneUsingDefault() {
        PowerMockito.mockStatic(System.class);
        when(System.getenv("flag")).thenReturn(null);
        when(System.getProperty("flag")).thenReturn(null);

        boolean orElse = OptionsResolver.resolveFlag("flag", null,
                new String[]{}).orElse(true);
        assertTrue(orElse);
        orElse = OptionsResolver.resolveFlag("flag", null,
                new String[]{}).orElse(false);
        assertFalse(orElse);
    }

    @Test
    public void testResolveFlagEmptyEnv() {
        PowerMockito.mockStatic(System.class);
        when(System.getenv("flag")).thenReturn("");
        when(System.getProperty("flag")).thenReturn(null);

        Optional<Boolean> flag = OptionsResolver.resolveFlag("flag", null,
                new String[]{});
        assertTrue(flag.get());
    }

    @Test
    public void testResolveFlagEmptyProp() {
        PowerMockito.mockStatic(System.class);
        when(System.getenv("flag")).thenReturn(null);
        when(System.getProperty("flag")).thenReturn("");

        Optional<Boolean> flag = OptionsResolver.resolveFlag("flag", null,
                new String[]{});
        assertTrue(flag.get());
    }

    @Test
    public void testResolveFlagEmptyLetter() {
        PowerMockito.mockStatic(System.class);
        when(System.getenv(null)).thenReturn(null);
        when(System.getProperty(null)).thenReturn(null);

        Optional<Boolean> flag = OptionsResolver.resolveFlag(null, 'f',
                new String[]{"-f"});
        assertTrue(flag.get());
    }

    @Test
    public void testResolveFlagEmptyArg() {
        PowerMockito.mockStatic(System.class);
        when(System.getenv("flag")).thenReturn(null);
        when(System.getProperty("flag")).thenReturn(null);

        Optional<Boolean> flag = OptionsResolver.resolveFlag("flag", null,
                new String[]{"--flag"});
        assertTrue(flag.get());
    }

    @Test
    public void testResolveFlagOneOfMany() {
        PowerMockito.mockStatic(System.class);
        when(System.getenv(null)).thenReturn(null);
        when(System.getProperty(null)).thenReturn(null);

        Optional<Boolean> flag = OptionsResolver.resolveFlag(null, 'f',
                new String[]{"something", "-xfc", "somethingElse"});
        assertTrue(flag.get());
    }

    @Test
    public void testResolveFlagOneOfManyFalse() {
        PowerMockito.mockStatic(System.class);
        when(System.getenv(null)).thenReturn(null);
        when(System.getProperty(null)).thenReturn(null);

        Optional<Boolean> flag1 = OptionsResolver.resolveFlag(null, 'f',
                new String[]{"something", "-false", "somethingElse"});
        assertTrue(flag1.get());

        Optional<Boolean> flag2 = OptionsResolver.resolveFlag(null, 'f',
                new String[]{"something", "-true", "somethingElse"});
        assertFalse(flag2.isPresent());
    }

    @Test
    public void testMatchesName() {
        assertTrue(OptionsResolver.matches("flag", 'f', "--flag", false));
    }

    @Test
    public void testMatchesLetter() {
        assertTrue(OptionsResolver.matches("flag", 'f', "-f", false));
    }

    @Test
    public void testMatchesNone() {
        assertFalse(OptionsResolver.matches("xxx", 'x', "-f", false));
    }
}
