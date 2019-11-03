package frc.team7170.lib.fsm;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EnumStateMapTest {

    private enum S0 implements State<S0, String> {}

    private enum S1 implements State<S1, String> {
        A
    }

    private static StateMap<S1, String> getStateMap() {
        return new EnumStateMap<>(S1.class, HashMap::new);
    }

    @Test
    void construction_noStates() {
        assertThrows(IllegalArgumentException.class, () -> new EnumStateMap<>(S0.class, HashMap::new));
    }

    @Test
    void s2state() {
        assertThat(getStateMap().s2state(S1.A), is(S1.A));
    }

    @Test
    void state2s() {
        assertThat(getStateMap().state2s(S1.A), is(S1.A));
    }

    @Test
    void s2bundle() {
        assertThat(getStateMap().s2bundle(S1.A).state, is(S1.A));
    }
}
