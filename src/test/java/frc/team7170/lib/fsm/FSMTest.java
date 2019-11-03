package frc.team7170.lib.fsm;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertAll;
import static frc.team7170.lib.TestUtil.assertNPE;

public class FSMTest {

    private static class CallbackSpy {

        private static class Callback {
            private final String type;
            private final int ordinal;
            private final String name;

            private Callback(String type, int ordinal) {
                this(type, ordinal, null);
            }

            private Callback(String type, int ordinal, String name) {
                this.type = type;
                this.ordinal = ordinal;
                this.name = name;
            }

            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof Callback)) {
                    return false;
                }
                Callback other = (Callback) obj;
                return type.equals(other.type) && ordinal == other.ordinal && Objects.equals(name, other.name);
            }

            @Override
            public String toString() {
                return name != null ? String.format("%s(%s):%d", type, name, ordinal) :
                        String.format("%s:%d", type, ordinal);
            }
        }

        private final List<Callback> callbacks = new ArrayList<>();

        private CallbackSpy beforeAll(int ordinal) {
            callbacks.add(new Callback("beforeAll", requireValidOrdinal(ordinal)));
            return this;
        }

        private CallbackSpy before(String name, int ordinal) {
            callbacks.add(new Callback("before", requireValidOrdinal(ordinal), name));
            return this;
        }

        private CallbackSpy onExit(String fullName, int ordinal) {
            callbacks.add(new Callback("onExit", requireValidOrdinal(ordinal), fullName));
            return this;
        }

        private CallbackSpy onEnter(String fullName, int ordinal) {
            callbacks.add(new Callback("onEnter", requireValidOrdinal(ordinal), fullName));
            return this;
        }

        private CallbackSpy after(String name, int ordinal) {
            callbacks.add(new Callback("after", requireValidOrdinal(ordinal), name));
            return this;
        }

        private CallbackSpy afterAll(int ordinal) {
            callbacks.add(new Callback("afterAll", requireValidOrdinal(ordinal)));
            return this;
        }

        private void verify(CallbackSpy expected) {
            if (!callbacks.equals(expected.callbacks)) {
                throw new AssertionError(
                        String.format("callback order mismatch: expected %s; got %s", expected.callbacks, callbacks)
                );
            }
        }

        private static int requireValidOrdinal(int ordinal) {
            if (ordinal < 0) {
                throw new IllegalArgumentException("invalid callback ordinal");
            }
            return ordinal;
        }
    }

    private enum SE implements State<SE, TE> {
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

        @Override
        public void onEnter(Event<SE, TE> event) {
            if (event.args.containsKey("spy")) {
                ((CallbackSpy) event.args.get("spy")).onEnter(State.fullName(this), 0);
            }
        }

        @Override
        public void onExit(Event<SE, TE> event) {
            if (event.args.containsKey("spy")) {
                ((CallbackSpy) event.args.get("spy")).onExit(State.fullName(this), 0);
            }
        }
    }

    private enum TE {
        T
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
    void getStateObj_initial() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class).build(SE.B);
        assertThat(m.getStateObj(), is(SE.B));
    }

    // --------
    // getState
    // --------

    @Test
    void getState_initial() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class).build(SE.B);
        assertThat(m.getState(), is(SE.B));
    }

    // --
    // in
    // --

    @Test
    void in_initial() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class).build(SE.B);
        assertAll(
                () -> assertThat(m.in(SE.B), is(true)),
                () -> assertThat(m.in(SE.A), is(true)),
                () -> assertThat(m.in(SE.C), is(false))
        );
    }

    @Test
    void in_nullInput() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class).build(SE.B);
        assertNPE(m::in, SE.B);
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
    void forceTo_valid() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class).build(SE.B);
        m.forceTo(SE.C);
        assertThat(m.getState(), is(SE.C));
    }

    @Test
    void forceTo_nullInput() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class).build(SE.B);
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> m.forceTo(null)),
                // null map is allowed!
                () -> assertDoesNotThrow(() -> m.forceTo(SE.C, null))
        );
    }

    @Test
    void forceTo_unknownState_strStates() {
        FSM<String, String> m = FSM.builder("A").build("A");
        assertThrows(IllegalArgumentException.class, () -> m.forceTo("X"));
    }

    @Test
    void forceTo_inaccessibleState() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class).build(SE.B);
        assertThrows(IllegalArgumentException.class, () -> m.forceTo(SE.A));
    }

    @Test
    void forceTo_currState() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class).build(SE.B);
        m.forceTo(SE.B);
        assertThat(m.getState(), is(SE.B));
    }

    @Test
    void forceTo_ignoreAbort() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class).beforeAll(e -> false).build(SE.B);
        m.forceTo(SE.C);
        assertThat(m.getState(), is(SE.C));
    }

    @Test
    @SuppressWarnings("unchecked")
    void forceTo_nonNullEventObj() {
        // Have to use array so event is effectively final.
        Event<SE, TE>[] event = new Event[1];
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class).afterAll(e -> event[0] = e).build(SE.B);
        m.forceTo(SE.C, null);
        assertAll(
                () -> assertThat(event[0].args, notNullValue()),
                // Maps generated by default must be mutable.
                () -> assertDoesNotThrow(() -> event[0].args.put("test", 1))
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void forceTo_eventObj() {
        Event<String, String>[] events = new Event[4];
        FSM<String, String> m = FSM.builder("A", "B")
                .beforeAll((Event<String, String> e) -> events[0] = e)
                .onExit("A", e -> events[1] = e)
                .onEnter("B", e -> events[2] = e)
                .afterAll(e -> events[3] = e)
                .build("A");
        m.forceTo("B", Map.of("arg", 1));
        assertAll(
                // Assert that the Event objects are all the same (and hence their fields are the same).
                () -> assertThat(events[0], is(events[1])),
                () -> assertThat(events[1], is(events[2])),
                () -> assertThat(events[2], is(events[3])),

                // Check Event object fields.
                () -> assertThat(events[0].args.size(), is(1)),
                () -> assertThat(events[0].args.get("arg"), is(1)),
                () -> assertThat(events[0].machine, is(m)),
                () -> assertThat(events[0].trigger, nullValue()),
                () -> assertThat(events[0].src.name(), is("A")),
                () -> assertThat(events[0].dst.name(), is("B"))
        );
    }

    @Test
    void forceTo_callbackOrder() {
        CallbackSpy spy = new CallbackSpy();
        FSM<String, String> m = FSM.builder("A", "A/B", "X", "X/Y")
                .beforeAll(() -> spy.beforeAll(0))
                .beforeAll(() -> spy.beforeAll(1))
                .onExit("A/B", () -> spy.onExit("A/B", 0))
                .onExit("A/B", () -> spy.onExit("A/B", 1))
                .onExit("A", () -> spy.onExit("A", 0))
                .onExit("A", () -> spy.onExit("A", 1))
                .onEnter("X", () -> spy.onEnter("X", 0))
                .onEnter("X", () -> spy.onEnter("X", 1))
                .onEnter("X/Y", () -> spy.onEnter("X/Y", 0))
                .onEnter("X/Y", () -> spy.onEnter("X/Y", 1))
                .afterAll(() -> spy.afterAll(0))
                .afterAll(() -> spy.afterAll(1))
                .build("A/B");
        m.forceTo("X/Y");
        spy.verify(new CallbackSpy()
                .beforeAll(0)
                .beforeAll(1)
                .onExit("A/B", 0)
                .onExit("A/B", 1)
                .onExit("A", 0)
                .onExit("A", 1)
                .onEnter("X", 0)
                .onEnter("X", 1)
                .onEnter("X/Y", 0)
                .onEnter("X/Y", 1)
                .afterAll(0)
                .afterAll(1)
        );
    }

    @Test
    void forceTo_queuing() {
        String A = State.fullName(SE.A);
        String B = State.fullName(SE.B);
        String C = State.fullName(SE.C);
        CallbackSpy spy = new CallbackSpy();
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class)
                .beforeAll(e -> {
                    // Condition to avoid infinite loop.
                    if (e.src == SE.B && e.trigger == null) {
                        e.machine.forceTo(SE.B, Map.of("spy", spy));
                    }
                })
                .beforeAll(e -> {
                    // Condition to avoid infinite loop.
                    if (e.src == SE.C) {
                        // trigger always returns true when it has to queue a transition.
                        assertThat(e.machine.trigger(TE.T, Map.of("spy", spy)), is(true));
                    }
                })
                .beforeAll(() -> spy.beforeAll(0))
                .afterAll(() -> spy.afterAll(0))
                .transition(TE.T, SE.B, SE.C).before(() -> spy.before("T0", 0)).build()
                .build(SE.B);
        m.forceTo(SE.C, Map.of("spy", spy));
        spy.verify(new CallbackSpy()
                // forceTo(C)
                .beforeAll(0)
                .onExit(B, 0)
                .onExit(A, 0)
                .onEnter(A, 0)
                .onEnter(B, 0)
                .onEnter(C, 0)
                .afterAll(0)

                // forceTo(B)
                .beforeAll(0)
                .onExit(C, 0)
                .onExit(B, 0)
                .onExit(A, 0)
                .onEnter(A, 0)
                .onEnter(B, 0)
                .afterAll(0)

                // trigger(T0)
                .beforeAll(0)
                .before("T0", 0)
                .onExit(B, 0)
                .onExit(A, 0)
                .onEnter(A, 0)
                .onEnter(B, 0)
                .onEnter(C, 0)
                .afterAll(0)
        );
    }

    // -------
    // trigger
    // -------

    @Test
    void trigger_validNormal() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class)
                .transition(TE.T, SE.B, SE.C).build()
                .build(SE.B);
        assertAll(
                () -> assertThat(m.getState(), is(SE.B)),
                () -> assertThat(m.trigger(TE.T), is(true)),
                () -> assertThat(m.getState(), is(SE.C))
        );
    }

    @Test
    void trigger_validInternal() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class)
                .internalTransition(TE.T, SE.B).build()
                .build(SE.B);
        assertAll(
                () -> assertThat(m.getState(), is(SE.B)),
                () -> assertThat(m.trigger(TE.T), is(true)),
                () -> assertThat(m.getState(), is(SE.B))
        );
    }

    @Test
    void trigger_validReflexive() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class)
                .reflexiveTransition(TE.T, SE.B).build()
                .build(SE.B);
        assertAll(
                () -> assertThat(m.getState(), is(SE.B)),
                () -> assertThat(m.trigger(TE.T), is(true)),
                () -> assertThat(m.getState(), is(SE.B))
        );
    }

    @Test
    void trigger_transitionResolution() {
        CallbackSpy spy = new CallbackSpy();
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class)
                .transition(TE.T, SE.B, SE.B).before(() -> spy.before("first", 0)).build()
                .internalTransition(TE.T, SE.B).before(() -> spy.before("second", 0)).build()
                .build(SE.B);
        m.trigger(TE.T);
        // The first transition should be chosen despite the second one also being valid.
        spy.verify(new CallbackSpy().before("first", 0));
    }

    @Test
    void trigger_nullInput() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class)
                .transition(TE.T, SE.B, SE.C).build()
                .build(SE.B);
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> m.trigger(null)),
                // null map is allowed!
                () -> assertDoesNotThrow(() -> m.trigger(TE.T, null))
        );
    }

    @Test
    void trigger_invalidTrigger() {
        // Note the FSM is initialized in state C -- it does not ignore invalid triggers.
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class)
                .transition(TE.T, SE.B, SE.C).build()
                .build(SE.C);
        assertThrows(IllegalStateException.class, () -> m.trigger(TE.T));
    }

    @Test
    void trigger_unknownTrigger() {
        // Note the FSM is initialized in state C -- it does not ignore invalid triggers.
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class).build(SE.C);
        // An unknown trigger should behave like an invalid trigger.
        assertThrows(IllegalStateException.class, () -> m.trigger(TE.T));
    }

    @Test
    void trigger_fsmIgnoreInvalidTrigger() {
        // Note the FSM is initialized in state C -- it does not ignore invalid triggers.
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class)
                .ignoreInvalidTriggers()
                .transition(TE.T, SE.B, SE.C).build()
                .build(SE.C);
        assertAll(
                () -> assertThat(assertDoesNotThrow(() -> m.trigger(TE.T)), is(false)),
                () -> assertThat(m.getState(), is(SE.C))  // Make sure state did not change.
        );
    }

    @Test
    void trigger_stateIgnoreInvalidTrigger() {
        // Note the FSM is initialized in state B -- it ignores invalid triggers.
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class)
                .transition(TE.T, SE.C, SE.B).build()
                .build(SE.B);
        assertAll(
                () -> assertThat(assertDoesNotThrow(() -> m.trigger(TE.T)), is(false)),
                () -> assertThat(m.getState(), is(SE.B))  // Make sure state did not change.
        );
    }

    @Test
    void trigger_beforeAllAbort() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class)
                .beforeAll(e -> false)
                .transition(TE.T, SE.B, SE.C).build()
                .build(SE.B);
        assertThat(m.trigger(TE.T), is(false));
    }

    @Test
    void trigger_beforeAbort() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class)
                .transition(TE.T, SE.B, SE.C).before(e -> false).build()
                .build(SE.B);
        assertThat(m.trigger(TE.T), is(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    void trigger_eventObj() {
        Event<String, String>[] events = new Event[6];
        FSM<String, String> m = FSM.builder("A", "B")
                .beforeAll((Event<String, String> e) -> events[0] = e)
                .onExit("A", e -> events[2] = e)
                .onEnter("B", e -> events[3] = e)
                .afterAll(e -> events[5] = e)
                .transition("T", "A", "B")
                    .before((Event<String, String> e) -> events[1] = e)
                    .after(e -> events[4] = e)
                    .build()
                .build("A");
        m.trigger("T", Map.of("arg", 1));
        assertAll(
                // Assert that the Event objects are all the same (and hence their fields are the same).
                () -> assertThat(events[0], is(events[1])),
                () -> assertThat(events[1], is(events[2])),
                () -> assertThat(events[2], is(events[3])),
                () -> assertThat(events[3], is(events[4])),
                () -> assertThat(events[4], is(events[5])),

                // Check Event object fields.
                () -> assertThat(events[0].args.size(), is(1)),
                () -> assertThat(events[0].args.get("arg"), is(1)),
                () -> assertThat(events[0].machine, is(m)),
                () -> assertThat(events[0].trigger, is("T")),
                () -> assertThat(events[0].src.name(), is("A")),
                () -> assertThat(events[0].dst.name(), is("B"))
        );
    }

    @Test
    void trigger_callbackOrder() {
        CallbackSpy spy = new CallbackSpy();
        FSM<String, String> m = FSM.builder("A", "A/B", "X", "X/Y")
                .beforeAll(() -> spy.beforeAll(0))
                .beforeAll(() -> spy.beforeAll(1))
                .onExit("A/B", () -> spy.onExit("A/B", 0))
                .onExit("A/B", () -> spy.onExit("A/B", 1))
                .onExit("A", () -> spy.onExit("A", 0))
                .onExit("A", () -> spy.onExit("A", 1))
                .onEnter("X", () -> spy.onEnter("X", 0))
                .onEnter("X", () -> spy.onEnter("X", 1))
                .onEnter("X/Y", () -> spy.onEnter("X/Y", 0))
                .onEnter("X/Y", () -> spy.onEnter("X/Y", 1))
                .afterAll(() -> spy.afterAll(0))
                .afterAll(() -> spy.afterAll(1))
                .transition("T", "A/B", "X/Y")
                    .before(() -> spy.before("T", 0))
                    .before(() -> spy.before("T", 1))
                    .after(() -> spy.after("T", 0))
                    .after(() -> spy.after("T", 1))
                    .build()
                .build("A/B");
        m.trigger("T");
        spy.verify(new CallbackSpy()
                .beforeAll(0)
                .beforeAll(1)
                .before("T", 0)
                .before("T", 1)
                .onExit("A/B", 0)
                .onExit("A/B", 1)
                .onExit("A", 0)
                .onExit("A", 1)
                .onEnter("X", 0)
                .onEnter("X", 1)
                .onEnter("X/Y", 0)
                .onEnter("X/Y", 1)
                .after("T", 0)
                .after("T", 1)
                .afterAll(0)
                .afterAll(1)
        );
    }

    @Test
    void trigger_callbackOrderInternal() {
        CallbackSpy spy = new CallbackSpy();
        FSM<String, String> m = FSM.builder("A")
                .beforeAll(() -> spy.beforeAll(0))
                .beforeAll(() -> spy.beforeAll(1))
                .onExit("A", () -> spy.onExit("A", 0))  // Should not be called.
                .onEnter("A", () -> spy.onEnter("A", 0))  // Should not be called.
                .afterAll(() -> spy.afterAll(0))
                .afterAll(() -> spy.afterAll(1))
                .internalTransition("T", "A")
                    .before(() -> spy.before("T", 0))
                    .before(() -> spy.before("T", 1))
                    .after(() -> spy.after("T", 0))
                    .after(() -> spy.after("T", 1))
                    .build()
                .build("A");
        m.trigger("T");
        spy.verify(new CallbackSpy()
                .beforeAll(0)
                .beforeAll(1)
                .before("T", 0)
                .before("T", 1)
                .after("T", 0)
                .after("T", 1)
                .afterAll(0)
                .afterAll(1)
        );
    }

    @Test
    void trigger_queuing() {
        String A = State.fullName(SE.A);
        String B = State.fullName(SE.B);
        String C = State.fullName(SE.C);
        CallbackSpy spy = new CallbackSpy();
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class)
                .beforeAll(e -> {
                    // Condition to avoid infinite loop.
                    if (e.src == SE.B && !e.args.containsKey("stop")) {
                        e.machine.forceTo(SE.B, Map.of("spy", spy));
                        // The stop flag is kinda weird but it works.
                        // trigger always returns true when it has to queue a transition.
                        assertThat(e.machine.trigger(TE.T, Map.of("spy", spy, "stop", new Object())), is(true));
                    }
                })
                .beforeAll(() -> spy.beforeAll(0))
                .afterAll(() -> spy.afterAll(0))
                .transition(TE.T, SE.B, SE.C).before(() -> spy.before("T0", 0)).build()
                .build(SE.B);
        m.trigger(TE.T, Map.of("spy", spy));
        spy.verify(new CallbackSpy()
                // trigger(T0)
                .beforeAll(0)
                .before("T0", 0)
                .onExit(B, 0)
                .onExit(A, 0)
                .onEnter(A, 0)
                .onEnter(B, 0)
                .onEnter(C, 0)
                .afterAll(0)

                // forceTo(B)
                .beforeAll(0)
                .onExit(C, 0)
                .onExit(B, 0)
                .onExit(A, 0)
                .onEnter(A, 0)
                .onEnter(B, 0)
                .afterAll(0)

                // trigger(T0)
                .beforeAll(0)
                .before("T0", 0)
                .onExit(B, 0)
                .onExit(A, 0)
                .onEnter(A, 0)
                .onEnter(B, 0)
                .onEnter(C, 0)
                .afterAll(0)
        );
    }

    @Test
    void trigger_queuing_deferredError() {
        FSM<SE, TE> m = FSM.builder(TE.class, SE.class)
                .beforeAll(e -> {
                    if (e.args.containsKey("do trigger")) {
                        e.machine.trigger(TE.T);
                    }
                })
                .transition(TE.T, SE.B, SE.C).build()
                .build(SE.B);
        assertDoesNotThrow(() -> m.trigger(TE.T));
        m.forceTo(SE.B);
        assertThrows(IllegalStateException.class, () -> m.trigger(TE.T, Map.of("do trigger", new Object())));
    }
}
