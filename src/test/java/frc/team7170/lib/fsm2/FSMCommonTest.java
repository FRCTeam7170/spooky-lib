package frc.team7170.lib.fsm2;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertAll;
import static frc.team7170.lib.TestUtil.assertNPE;

public class FSMCommonTest {

    private enum SE implements State {
        A(null, false, false),
        B(A, true, true),
        C(B, true, false);

        private final SE parent;
        private final boolean accessible;
        private final boolean iit;

        SE(SE parent, boolean accessible, boolean iit) {
            this.parent = parent;
            this.accessible = accessible;
            this.iit = iit;
        }

        @Override
        public SE getParent() {
            return parent;
        }

        @Override
        public boolean isAccessible() {
            return accessible;
        }

        @Override
        public boolean getIgnoreInvalidTriggers() {
            return iit;
        }
    }

    private static final String[] SS = {"A/B", "A/B/C"};

    private enum TE {
        A2B, B2A, A2C, C2A, B2C, C2B, X
    }

    // -----------
    // getStateObj
    // -----------

    @Test
    void getStateObj_defaultSimpleStateProperties_strStates() {
        FSM<String, String> m = FSM.builder("A").build("A");
        assertThat(m.getStateObj().name(), is("A"));
        assertThat(m.getStateObj().getParent(), nullValue());
        assertThat(m.getStateObj().isAccessible(), is(true));
        assertThat(m.getStateObj().getIgnoreInvalidTriggers(), is(false));
    }

    @Test
    void getStateObj_defaultNestedStateProperties_strStates() {
        FSM<String, String> m = FSM.builder("A/B").build("A/B");
        assertThat(m.getStateObj().name(), is("B"));
        assertThat(m.getStateObj().isAccessible(), is(true));
        assertThat(m.getStateObj().getIgnoreInvalidTriggers(), is(false));
        assertThat(m.getStateObj().getParent().name(), is("A"));
        assertThat(m.getStateObj().getParent().getParent(), nullValue());
        assertThat(m.getStateObj().getParent().isAccessible(), is(false));
        // We do not assert anything regarding the parent state's ignoreInvalidTriggers (unspecified/does not matter).
    }

    @Test
    void getStateObj_equality_enumStates() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class).build(SE.B);
        assertThat(m.getStateObj(), is(SE.B));
    }

    // --------
    // getState
    // --------

    @Test
    void getState_initial_strStates() {
        FSM<String, String> m = FSM.builder("A").build("A");
        assertThat(m.getState(), is("A"));
    }

    @Test
    void getState_initial_enumStates() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class).build(SE.B);
        assertThat(m.getState(), is(SE.B));
    }

    // --
    // in
    // --

    @Test
    void in_initial_strStates() {
        FSM<String, String> m = FSM.builder(SS).build("A/B");
        assertAll(
                () -> assertThat(m.in("A/B"), is(true)),
                () -> assertThat(m.in("A"), is(true)),
                () -> assertThat(m.in("A/B/C"), is(false))
        );
    }

    @Test
    void in_initial_enumStates() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class).build(SE.B);
        assertAll(
                () -> assertThat(m.in(SE.B), is(true)),
                () -> assertThat(m.in(SE.A), is(true)),
                () -> assertThat(m.in(SE.C), is(false))
        );
    }

    @Test
    void in_nullInput() {
        FSM<String, String> m = FSM.builder("A").build("A");
        assertNPE(m::in, "A");
    }

    @Test
    void in_unknownState_strStates() {
        FSM<String, String> m = FSM.builder("A").build("A");
        assertThrows(IllegalArgumentException.class, () -> m.in("B"));
    }

    // -------
    // forceTo
    // -------

    @Test
    void forceTo_valid_strStates() {
        FSM<String, String> m = FSM.builder(SS).build("A/B");
        m.forceTo("A/B/C");
        assertThat(m.getState(), is("A/B/C"));
    }

    @Test
    void forceTo_valid_enumStates() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class).build(SE.B);
        m.forceTo(SE.C);
        assertThat(m.getState(), is(SE.C));
    }

    @Test
    void forceTo_nullInput() {
        FSM<String, String> m = FSM.builder(SS).build("A/B");
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> m.forceTo(null)),
                // null map is allowed!
                () -> assertDoesNotThrow(() -> m.forceTo("A/B/C", null))
        );
    }

    @Test
    void forceTo_unknownState_strStates() {
        FSM<String, String> m = FSM.builder("A").build("A");
        assertThrows(IllegalArgumentException.class, () -> m.forceTo("X"));
    }

    @Test
    void forceTo_inaccessibleState_strStates() {
        FSM<String, String> m = FSM.builder(SS).build("A/B");
        assertThrows(IllegalArgumentException.class, () -> m.forceTo("A"));
    }

    @Test
    void forceTo_inaccessibleState_enumStates() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class).build(SE.B);
        assertThrows(IllegalArgumentException.class, () -> m.forceTo(SE.A));
    }

    @Test
    void forceTo_currState_strStates() {
        FSM<String, String> m = FSM.builder(SS).build("A/B");
        m.forceTo("A/B");
        assertThat(m.getState(), is("A/B"));
    }

    @Test
    void forceTo_currState_enumStates() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class).build(SE.B);
        m.forceTo(SE.B);
        assertThat(m.getState(), is(SE.B));
    }

    // -------
    // trigger
    // -------

    // TODO

    // TODO: ignore invalid triggers
    // TODO: callbacks (order, different transition types, conditional, Event obj construction)
}
