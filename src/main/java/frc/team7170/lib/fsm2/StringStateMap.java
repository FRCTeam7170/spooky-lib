package frc.team7170.lib.fsm2;

import java.util.*;
import java.util.function.Supplier;

class StringStateMap<T> implements StateMap<String, T> {

    private final Map<String, StateBundle<T>> bundleMap;

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
            // getLineage always returns a list with at least one element (i.e. at least the leaf state); if that
            // state is already accessible, then it must have been a duplicate.
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
        return FSM.fullName(state);
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
