package frc.team7170.lib.fsm2;

import java.util.*;
import java.util.function.Supplier;

/**
 * An implementation of {@link StateMap StateMap} for states of type string.
 *
 * @param <T> the trigger type.
 *
 * @author Robert Russell
 */
class StringStateMap<T> implements StateMap<String, T> {

    private final Map<String, StateBundle<T>> bundleMap;

    /**
     * @param states an array of states.
     * @param mapSupplier a supplier returning maps appropriate for the trigger type.
     * @throws NullPointerException if any of the states in the given state array are {@code null}.
     * @throws IllegalArgumentException if the given state array is empty.
     * @throws IllegalArgumentException if the given state array contains duplicate elements.
     */
    StringStateMap(String[] states, Supplier<Map<T, Transition<T>>> mapSupplier) {
        if (states.length == 0) {
            throw new IllegalArgumentException("state machines must have at least one state");
        }
        // Most applications will not nest states, so this initial size is appropriate.
        bundleMap = new HashMap<>(states.length);
        for (String state : states) {
            BaseState last = null;
            for (String seg : StringStateMap.getLineage(
                    Objects.requireNonNull(state, "state must be non-null")
            )) {
                StateBundle<T> sb = bundleMap.get(seg);
                if (sb == null) {
                    last = new BaseState(seg.substring(seg.lastIndexOf(FSM.SUB_STATE_SEP)+1), last);
                    bundleMap.put(seg, new StateBundle<>(last, mapSupplier));
                } else {
                    last = (BaseState) sb.state;
                }
            }
            // getLineage always returns a list with at least one element (i.e. at least the leaf state); if the last
            // state in the list is already accessible, then it must have been a duplicate, which we prohibit to prevent
            // user errors.
            //noinspection ConstantConditions
            if (last.accessible) {
                throw new IllegalArgumentException(String.format("duplicate state '%s'", state));
            }
            // Make sure the leaf state is accessible (by default it is not so that parent states not explicitly
            // specified cannot be entered except through a child state).
            last.accessible = true;
        }
    }

    @Override
    public String state2s(State state) {
        return State.fullName(state);
    }

    @Override
    public State s2state(String s) {
        StateBundle<T> sb = bundleMap.get(s);
        if (sb == null) {
            throw new IllegalArgumentException(String.format("unknown state: '%s'", s));
        }
        return sb.state;
    }

    @Override
    public StateBundle<T> s2bundle(String s) {
        return bundleMap.get(s);
    }

    /**
     * <p>
     * Get a list of strings representing the lineage of the given state sorted in "parent before child" order.
     * </p>
     * <p>
     * For example, calling this method with {@code "a/b/c"} would return {@code {"a", "a/b", "a/b/c"}}.
     * </p>
     *
     * @param state the state to get the lineage of.
     * @return a list of strings representing the lineage of the given state sorted in "parent before child" order.
     */
    private static List<String> getLineage(String state) {
        List<String> list = new ArrayList<>();
        int idx = state.indexOf(FSM.SUB_STATE_SEP);
        while (idx != -1) {
            list.add(state.substring(0, idx));
            idx = state.indexOf(FSM.SUB_STATE_SEP, idx+1);
        }
        list.add(state);
        return list;
    }
}
