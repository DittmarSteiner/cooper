/*
 * Copyright (c) 2020, Dittmar Steiner <dittmar.steiner@gmail.com>
 * ISC License http://opensource.org/licenses/isc-license.txt
 */
package com.github.dittmarsteiner.cooper;

import java.util.NoSuchElementException;

import static java.util.Objects.*;

/**
 * <p>A container inspired by <code>Either</code> or <code>Try</code> and
 * {@link java.util.Optional Optional}.
 * Since {@link java.util.Optional Optional} does not include {@link Exception}s
 * and other implementations just want too much, {@link Resulting} might be a
 * handy container. {@link Resulting} has either a value OR an error. For more
 * sophisticated features like filter, map or stream, you can store the
 * {@link Resulting} into an {@link java.util.Optional Optional}.
 * </p>
 *
 * <b>Usage as a return type:</b>
 * <pre>{@code
 * public Resulting<String> someMethodResultingThatCanFail() {
 *    Resulting resulting;
 *    try {
 *        resulting = Resulting.ofValue(returnsAStringButCanFail());
 *    }
 *    catch (Throwable e) {
 *        resulting = Resulting.ofError(e);
 *    }
 *
 *    return resulting;
 * }
 * }</pre>
 *
 * <b>Usage of the return value</b>
 * <pre>{@code
 * // simple
 * Resulting<String> resulting1 = someMethodResultingThatCanFail();
 * String str1 = resulting1.isValue() ? resulting1.orElseThrow() : "not found";
 *
 * // risky
 * String str2 = someMethodResultingThatCanFail().orElseThrow();
 *
 * // advanced
 * var resulting2 = someMethodResultingThatCanFail();
 * String str3;
 * if (resulting2.isValue()) {
 *     str3 = resulting2.orElseThrow();
 * }
 * else {
 *     LOG.warn(resulting2.getError().getMessage());
 *     str3 = "not found";
 * }
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
    public T orElseThrow()  throws RuntimeException {
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
    public RuntimeException getError() throws NoSuchElementException {
        if (value != null) {
            throw new NoSuchElementException("No throwable present");
        }
        return asRuntimeException(throwable);
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
        return new Resulting<>(
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
