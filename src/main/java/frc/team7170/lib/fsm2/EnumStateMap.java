package frc.team7170.lib.fsm2;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;


/**
 * An implementation of {@link StateMap StateMap} for states of an enum type.
 *
 * @param <S> the state type.
 * @param <T> the trigger type.
 *
 * @author Robert Russell
 */
class EnumStateMap<S extends Enum<S> & State, T> implements StateMap<S, T> {

    private final Map<S, StateBundle<T>> bundleMap;

    /**
     * @param stateEnum the class object of the enum whose constants are to be used for states.
     * @param mapSupplier a supplier returning maps appropriate for the trigger type.
     * @throws IllegalArgumentException if the given enum has no constants.
     */
    EnumStateMap(Class<S> stateEnum, Supplier<Map<T, Transition<T>>> mapSupplier) {
        S[] enumConstants = stateEnum.getEnumConstants();
        if (enumConstants.length == 0) {
            throw new IllegalArgumentException("state machines must have at least one state");
        }
        // Use EnumMap because it is fast.
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
