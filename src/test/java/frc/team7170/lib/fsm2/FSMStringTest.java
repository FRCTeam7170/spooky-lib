package frc.team7170.lib.fsm2;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FSMStringTest {

    @Test
    void getStateObj_ignoreInvalidTriggers() {
        FSM<String, String> m = FSM.builder("a", "a/b").ignoreInvalidTriggers("a/b").build("a/b");
        assertThat(m.getStateObj().getIgnoreInvalidTriggers(), is(true));
        assertThat(m.getStateObj().getParent().getIgnoreInvalidTriggers(), is(false));
    }

    // TODO: onEnter, onExit during construction
}
