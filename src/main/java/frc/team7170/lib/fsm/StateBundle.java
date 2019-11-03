package frc.team7170.lib.fsm;

import java.util.Map;
import java.util.function.Supplier;

/**
 * {@code StateBundle} is used internally to represent a {@link State State} and its associated trigger-to-transition
 * table.
 *
 * @param <S> the state type.
 * @param <T> the trigger type.
 *
 * @author Robert Russell
 */
class StateBundle<S, T> {

    final State<S, T> state;
    private final Map<T, Transition<S, T>> transitionTable;

    /**
     * @param state the {@link State State} associated with this {@code StateBundle}.
     * @param mapSupplier a supplier returning maps appropriate for the trigger type.
     */
    StateBundle(State<S, T> state, Supplier<Map<T, Transition<S, T>>> mapSupplier) {
        this.state = state;
        this.transitionTable = mapSupplier.get();
    }

    /**
     * <p>
     * Add a new transition to this {@code StateBundle}. If this method is called more than once on the same
     * {@code StateBundle} with the same trigger, only the first transition is considered.
     * </p>
     * <p>
     * This method is only called while an {@code FSM} is being built.
     * </p>
     *
     * @param trigger the trigger for the transition.
     * @param transition the transition.
     */
    void addTransition(T trigger, Transition<S, T> transition) {
        // putIfAbsent so first added transition with a certain trigger is preferred.
        transitionTable.putIfAbsent(trigger, transition);
    }

    /**
     * Get the transition that should be executed if this {@code StateBundle} is the current state in an {@code FSM} and
     * the given trigger occurs, or {@code null} if the given trigger is invalid.
     *
     * @param trigger the trigger.
     * @return the transition that should be executed if this {@code StateBundle} is the current state in an {@code FSM}
     * and the given trigger occurs, or {@code null} if the given trigger is invalid.
     */
    Transition<S, T> resolveTransition(T trigger) {
        return transitionTable.get(trigger);
    }
}
