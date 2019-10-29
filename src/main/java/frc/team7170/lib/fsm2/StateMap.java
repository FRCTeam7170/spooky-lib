package frc.team7170.lib.fsm2;

/**
 * {@code StateMap} is used internally as a means to have different ways of converting objects of the generic state type
 * to actual {@link State State} objects and vice versa depending on what the generic state type is.
 *
 * @param <S> the state type.
 * @param <T> the trigger type.
 *
 * @author Robert Russell
 * @see StringStateMap
 * @see EnumStateMap
 */
interface StateMap<S, T> {

    /**
     * Convert the given {@link State State} to the generic state type. This is only called with valid {@code States}
     * (i.e. states known to be representable as an object of the generic state type).
     *
     * @param state the state to convert.
     * @return the converted state.
     */
    S state2s(State state);

    /**
     * Convert the given state of the generic state type to a {@link State State}.
     *
     * @param s the state to convert.
     * @return the converted state.
     * @throws IllegalArgumentException if the given state of the generic state type cannot be converted to a
     * {@code State}.
     */
    State s2state(S s);

    /**
     * Convert the given state of the generic state type to a {@link StateBundle StateBundle}.
     *
     * @param s the state to convert.
     * @return the {@code StateBundle}, or {@code null} if the given state of the generic state type does not have an
     * associated {@code StateBundle} (i.e. does not belong to the {@link FSM FSM} containing this {@code StateMap}).
     */
    StateBundle<T> s2bundle(S s);
}
