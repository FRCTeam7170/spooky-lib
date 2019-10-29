package frc.team7170.lib.fsm2;

import java.util.Map;
import java.util.function.Supplier;

class StateBundle<T> {

    final State state;
    private final Map<T, Transition<T>> transitionTable;

    StateBundle(State state, Supplier<Map<T, Transition<T>>> mapSupplier) {
        this.state = state;
        this.transitionTable = mapSupplier.get();
    }

    void addTransition(T trigger, Transition<T> transition) {
        // putIfAbsent so first added transition with a certain trigger is preferred.
        transitionTable.putIfAbsent(trigger, transition);
    }

    Transition<T> resolveTransition(T trigger) {
        return transitionTable.get(trigger);
    }
}
