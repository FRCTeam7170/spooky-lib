package frc.team7170.lib.fsm2;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertAll;

public class StringStateMapTest {

    private static StateMap<String, String> getStateMap() {
        return new StringStateMap<>(
                new String[]{"A", "A/B"},
                HashMap::new
        );
    }

    @Test
    void construction_noStates() {
        assertThrows(IllegalArgumentException.class, () -> new StringStateMap<>(new String[]{}, HashMap::new));
    }

    @Test
    void construction_nullState() {
        assertThrows(NullPointerException.class, () -> new StringStateMap<>(new String[]{"a", null}, HashMap::new));
    }

    @Test
    void construction_dupeState() {
        assertThrows(IllegalArgumentException.class, () -> new StringStateMap<>(new String[]{"a", "a"}, HashMap::new));
    }

    @Test
    void s2state_state2s() {
        StateMap<String, String> sm = getStateMap();
        assertAll(
                () -> assertThat(sm.state2s(sm.s2state("A")), is("A")),
                () -> assertThat(sm.state2s(sm.s2state("A/B")), is("A/B"))
        );
    }

    @Test
    void s2state_invalid() {
        assertThrows(IllegalArgumentException.class, () -> getStateMap().s2state("unknown state"));
    }

    @Test
    void s2bundle() {
        StateMap<String, String> sm = getStateMap();
        StateBundle<String> sb_a = sm.s2bundle("A");
        StateBundle<String> sb_ab = sm.s2bundle("A/B");
        assertAll(
                () -> assertThat(sb_a, notNullValue()),
                () -> assertThat(sb_ab, notNullValue()),
                () -> assertThat(sm.s2bundle("unknown state"), nullValue()),
                () -> assertThat(sb_a.state.name(), is("A")),
                () -> assertThat(sb_ab.state.name(), is("B"))
        );

    }
}
