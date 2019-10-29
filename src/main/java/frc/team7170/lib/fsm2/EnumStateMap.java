package frc.team7170.lib.fsm2;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

class EnumStateMap<S extends Enum<S> & State, T> implements StateMap<S, T> {

    private final Map<S, StateBundle<T>> bundleMap;

    EnumStateMap(Class<S> stateEnum, Supplier<Map<T, Transition<T>>> mapSupplier) {
        S[] enumConstants = stateEnum.getEnumConstants();
        if (enumConstants.length == 0) {
            throw new IllegalArgumentException("state machines must have at least one state");
        }
        bundleMap = new EnumMap<>(stateEnum);
        for (S s : enumConstants) {
            bundleMap.put(s, new StateBundle<>(s, mapSupplier));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public S state2s(State state) {
        return (S) state;
    }

    @Override
    public State s2state(S s) {
        return s;
    }

    @Override
    public StateBundle<T> s2bundle(S s) {
        return bundleMap.get(s);
    }
}
