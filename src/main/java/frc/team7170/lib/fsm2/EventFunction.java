package frc.team7170.lib.fsm2;

import java.util.Objects;

/**
 * A functional interface accepting an {@link Event Event} as an argument and returning a boolean.
 *
 * @apiNote This custom interface is used instead of the {@link java.util.function.Function Function} interface in the
 * standard library so the {@link #seqCompose(EventFunction) seqCompose} method could be added. Not using the generic
 * version also has the added benefit of avoiding autoboxing/unboxing on the boolean return value.
 *
 * @author Robert Russell
 */
@FunctionalInterface
public interface EventFunction {

    /**
     * Apply the given input to this {@code EventFunction}.
     *
     * @param event the {@link Event Event} argument.
     * @return a boolean.
     */
    boolean apply(Event event);

    /**
     * Return an {@code EventFunction} that first applies its argument to this {@code EventFunction} and then to the
     * given one if and only if the first {@code EventFunction} returned true. The returned {@code EventFunction}
     * returns true if and only if both composed {@code EventFunction}s returned true.
     *
     * @param other the {@code EventFunction} to sequentially compose with this one.
     * @return an {@code EventFunction} sequentially composed of this {@code EventFunction} and the given one.
     * @throws NullPointerException if the given {@code EventFunction} is {@code null}.
     */
    default EventFunction seqCompose(EventFunction other) {
        Objects.requireNonNull(other);
        return event -> apply(event) && other.apply(event);
    }
}
