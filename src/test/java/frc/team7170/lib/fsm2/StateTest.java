package frc.team7170.lib.fsm2;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static frc.team7170.lib.TestUtil.assertNPE;

public class StateTest {

    @Test
    void fullName_nullInput() {
        assertNPE(State::fullName, new BaseState("A", null));
    }

    @Test
    void fullName_simple() {
        State state = new BaseState("A", null);
        assertThat(State.fullName(state), is("A"));
    }

    @Test
    void fullName_nested() {
        State stateA = new BaseState("A", null);
        State stateB = new BaseState("B", stateA);
        assertThat(State.fullName(stateB), is("A/B"));
    }

    @Test
    void inLineage_nullInput() {
        State s = new BaseState("A", null);
        assertNPE(State::inLineage, s, s);
    }

    @Test
    void inLineage_equality() {
        State state = new BaseState("A", null);
        assertThat(State.inLineage(state, state), is(true));
    }

    @Test
    void inLineage_true() {
        State parent = new BaseState("A", null);
        State child = new BaseState("B", parent);
        assertThat(State.inLineage(child, parent), is(true));
    }

    @Test
    void inLineage_false() {
        State parent = new BaseState("A", null);
        State child = new BaseState("B", parent);
        assertThat(State.inLineage(parent, child), is(false));
    }
}
