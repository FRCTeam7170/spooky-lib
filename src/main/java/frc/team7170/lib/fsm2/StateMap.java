package frc.team7170.lib.fsm2;

interface StateMap<S, T> {

    S state2s(State state);

    State s2state(S s);

    StateBundle<T> s2bundle(S s);
}
