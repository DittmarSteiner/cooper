/*
 * Copyright (c) 2020, Dittmar Steiner <dittmar.steiner@gmail.com>
 * ISC License http://opensource.org/licenses/isc-license.txt
 */
package com.github.dittmarsteiner.cooper;

import java.util.*;

import static java.util.Objects.*;

/**
 * <p>A {@link Resulting} has either a value OR an error. A container class
 * inspired by <code>Either</code> or <code>Try</code> and
 * {@link java.util.Optional Optional}. Since {@link java.util.Optional
 * Optional} does not include {@link Exception}s
 * and other implementations just want too much, {@link Resulting} might be a
 * handy container. For more sophisticated features like filter, map or
 * stream, you can use
 * {@link Resulting#ofValue(Object)} or {@link Resulting#ofError(Throwable)}.
 * </p>
 *
 * <b>Usage as a return type:</b>
 * <pre>{@code
 * public Resulting<String> someMethodThatCanFail() {
 *    try {
 *        return Resulting.ofValue(returnsAStringButCanItFail());
 *    }
 *    catch (Throwable e) {
 *        return Resulting.ofError(e);
 *    }
 * }
 * }</pre>
 *
 * <b>Usage of the return value</b>
 * <pre>{@code
 * // simple - may re-throw the contained Throwable as RuntimeException if value is missing
 * String str = someMethodThatCanFail().value();
 *
 * // classic
 * String str1 = someMethodThatCanFail().isValue() ? resulting1.value() : "not found";
 *
 * // utilizing Optional
 * String str2 = someMethodThatCanFail().optValue().orElse("not found");
 *
 * // utilizing Optional map
 * int count = someMethodThatCanFail().optValue().map(Integer::valueOf).orElse(0);
 *
 * // utilizing Optional for Exception handling
 * Resulting<String> resulting = someMethodThatCanFail();
 * resulting.optError().ifPresent(this::log); // void log(Throwable e) {...}
 * var value = resulting.optValue() ...
 * }</pre>
 *
 * @param <T> whatever
 */
public class Resulting<T> {
    private final T value;
    private final Throwable throwable;

    private Resulting(T value, Throwable throwable) {
        this.value = value;
        this.throwable = throwable;
    }

    /**
     *
     * @return if {@code value} is preset
     */
    public boolean isValue() {
        return nonNull(value);
    }

    /**
     *
     * @return if {@code throwable} is preset
     */
    public boolean isError() {
        return nonNull(throwable);
    }

    /**
     *
     * @return the {@code value} if present
     * @throws RuntimeException the contained {@link Throwable} (optionally
     * wrapped in a {@link RuntimeEception} if {@code value} is not present.
     * Yes, it actually should throw a {@link NoSuchElementException}, so the
     * semantics is wrong, but the logging is easier since we already have a
     * {@code throwable}…
     */
    public T value() throws RuntimeException {
        if (value != null) {
            return value;
        }
        throw asRuntimeException(throwable);
    }

    /**
     *
     * @return if {@code throwable} is present
     * @throws NoSuchElementException if {@code throwable} is not present
     */
    public RuntimeException error() throws NoSuchElementException {
        if (value != null) {
            throw new NoSuchElementException("No throwable present");
        }
        return asRuntimeException(throwable);
    }

    /**
     * {@code Optional.ofNullable(value)} does not throw a {@link
     * RuntimeException}, but you migth ignore it.
     *
     * @return {@code value} wrapped
     */
    public Optional<T> optValue() {
        return Optional.ofNullable(value);
    }

    /**
     * {@code Optional.ofNullable(throwable)} does not throw a {@link
     * RuntimeException}, but you migth ignore it.
     *
     * @return {@code value} wrapped
     */
    public Optional<? extends Throwable> optError() {
        return Optional.ofNullable(throwable);
    }

    /**
     *
     * @param value must not be {@code null}
     * @param <T> whatever
     * @return with a {@code value} set
     * @throws NullPointerException if value is {@code null}
     */
    public static <T> Resulting<T> ofValue(T value)
            throws NullPointerException {
        return new Resulting<>(
                requireNonNull(value, "value must be present"),
                null);
    }

    /**
     *
     * @param throwable must not be {@code null}
     * @param <T> whatever
     * @return with a {@code throwable} set
     * @throws NullPointerException if throwable is {@code null}
     */
    public static <T> Resulting<T> ofError(Throwable throwable)
            throws NullPointerException {
        return new Resulting<T>(
                null,
                requireNonNull(throwable, "throwable must be present"));
    }

    /**
     * Convenient method ideal for static impoerts. In Java functional
     * programming you will need this al the time.
     * Wraps a {@link Throwable} into a
     * {@link RuntimeException} or casts to {@link RuntimeException}.
     *
     * @param throwable
     * @return wrapped or casted
     */
    public static RuntimeException asRuntimeException(Throwable throwable) {
        if (throwable instanceof RuntimeException) {
            return (RuntimeException) throwable;
        }

        return new RuntimeException(throwable.getMessage(), throwable);
    }
}
