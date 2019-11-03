package frc.team7170.lib.fsm2;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static frc.team7170.lib.TestUtil.assertNPE;

public class StateTest {

    @Test
    void fullName_nullInput() {
        assertNPE(State::fullName, new StringState<>("A", null));
    }

    @Test
    void fullName_simple() {
        State<String, ?> state = new StringState<>("A", null);
        assertThat(State.fullName(state), is("A"));
    }

    @Test
    void fullName_nested() {
        State<String, ?> stateA = new StringState<>("A", null);
        State<String, ?> stateB = new StringState<>("B", stateA);
        assertThat(State.fullName(stateB), is("A/B"));
    }

    @Test
    void inLineage_nullInput() {
        State<String, String> s = new StringState<>("A", null);
        assertNPE(State::inLineage, s, s);
    }

    @Test
    void inLineage_equality() {
        State<String, String> state = new StringState<>("A", null);
        assertThat(State.inLineage(state, state), is(true));
    }

    @Test
    void inLineage_true() {
        State<String, String> parent = new StringState<>("A", null);
        State<String, String> child = new StringState<>("B", parent);
        assertThat(State.inLineage(child, parent), is(true));
    }

    @Test
    void inLineage_false() {
        State<String, String> parent = new StringState<>("A", null);
        State<String, String> child = new StringState<>("B", parent);
        assertThat(State.inLineage(parent, child), is(false));
    }
}
