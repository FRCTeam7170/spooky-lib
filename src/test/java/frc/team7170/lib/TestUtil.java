package frc.team7170.lib;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertAll;

public final class TestUtil {

    @FunctionalInterface
    public interface M1<A> {
        void call(A a);
    }

    /**
     * Assert that the given method throws a {@code NullPointerException} when its input is {@code null}, but not when
     * it is non-null.
     *
     * @param method the method to test.
     * @param a the default value for the parameter (must be non-null).
     * @param <A> the type of the parameter.
     * @throws NullPointerException if the default value is {@code null}.
     */
    public static <A> void assertNPE(M1<? super A> method, A a) {
        Objects.requireNonNull(a);
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> method.call(null)),
                () -> assertDoesNotThrow(() -> method.call(a))
        );
    }

    @FunctionalInterface
    public interface M2<A, B> {
        void call(A a, B b);
    }

    /**
     * Assert that the given method throws a {@code NullPointerException} when at least one of its inputs is null, but
     * not when none of them are null.
     *
     * @param method the method to test.
     * @param a the default value for the first parameter (must be non-null).
     * @param b the default value for the second parameter (must be non-null).
     * @param <A> the type of the first parameter.
     * @param <B> the type of the second parameter.
     * @throws NullPointerException if any default value is {@code null}.
     */
    public static <A, B> void assertNPE(M2<? super A, ? super B> method, A a, B b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> method.call(null, null)),
                () -> assertThrows(NullPointerException.class, () -> method.call(null, b)),
                () -> assertThrows(NullPointerException.class, () -> method.call(a, null)),
                () -> assertDoesNotThrow(() -> method.call(a, b))
        );
    }

    @FunctionalInterface
    public interface M3<A, B, C> {
        void call(A a, B b, C c);
    }

    /**
     * Assert that the given method throws a {@code NullPointerException} when at least one of its inputs is null, but
     * not when none of them are null.
     *
     * @param method the method to test.
     * @param a the default value for the first parameter (must be non-null).
     * @param b the default value for the second parameter (must be non-null).
     * @param c the default value for the third parameter (must be non-null).
     * @param <A> the type of the first parameter.
     * @param <B> the type of the second parameter.
     * @param <C> the type of the third parameter.
     * @throws NullPointerException if any default value is {@code null}.
     */
    public static <A, B, C> void assertNPE(M3<? super A, ? super B, ? super C> method, A a, B b, C c) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        Objects.requireNonNull(c);
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> method.call(null, null, null)),
                () -> assertThrows(NullPointerException.class, () -> method.call(null, null, c)),
                () -> assertThrows(NullPointerException.class, () -> method.call(null, b, null)),
                () -> assertThrows(NullPointerException.class, () -> method.call(null, b, c)),
                () -> assertThrows(NullPointerException.class, () -> method.call(a, null, null)),
                () -> assertThrows(NullPointerException.class, () -> method.call(a, null, c)),
                () -> assertThrows(NullPointerException.class, () -> method.call(a, b, null)),
                () -> assertDoesNotThrow(() -> method.call(a, b, c))
        );
    }
}
