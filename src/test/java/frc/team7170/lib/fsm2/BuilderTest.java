package frc.team7170.lib.fsm2;

import frc.team7170.lib.TestUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static frc.team7170.lib.TestUtil.assertNPE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BuilderTest {

    private enum T1 {
        X
    }

    private enum S1 implements State<S1, T1> {
        X
    }

    private static final String[] SS = {"A/B", "A/B/C"};

    @Test
    void nullInputs() {
        // This test works for FSMs with enum states too since BuilderFromEnum does not override or add new methods.
        FSM.BuilderFromStrings<String> b = FSM.builder(SS);
        assertAll(
                // Factory methods
                () -> assertNPE(FSM::builder, "A"),
                () -> assertNPE(FSM::builder, T1.class, "A"),
                () -> assertNPE(FSM::builder, S1.class),
                () -> assertNPE(FSM::builder, T1.class, S1.class),

                // Instance methods
                () -> assertNPE((TestUtil.M1<Runnable>) b::afterAll, () -> {}),
                () -> assertNPE((TestUtil.M1<Consumer<Event<String, String>>>) b::afterAll, e -> {}),
                () -> assertNPE((TestUtil.M1<Runnable>) b::beforeAll, () -> {}),
                () -> assertNPE((TestUtil.M1<Consumer<Event<String, String>>>) b::beforeAll, e -> {}),
                () -> assertNPE((TestUtil.M1<EventFunction<String, String>>) b::beforeAll, e -> true),
                () -> assertNPE(b::transition, "B2C", "A/B", "A/B/C"),
                () -> assertNPE(b::transition, "B2C", List.of("A/B"), "A/B/C"),
                () -> assertNPE(b::internalTransition, "B2C", "A/B"),
                () -> assertNPE(b::internalTransition, "B2C", List.of("A/B")),
                () -> assertNPE(b::reflexiveTransition, "B2C", "A/B"),
                () -> assertNPE(b::reflexiveTransition, "B2C", List.of("A/B")),
                () -> assertNPE(b::ignoreInvalidTriggers, "A/B"),
                () -> assertNPE((TestUtil.M2<String, Runnable>) b::onEnter, "A/B", () -> {}),
                () -> assertNPE((TestUtil.M2<String, Consumer<Event<String, String>>>) b::onEnter, "A/B", e -> {}),
                () -> assertNPE((TestUtil.M2<String, Runnable>) b::onExit, "A/B", () -> {}),
                () -> assertNPE((TestUtil.M2<String, Consumer<Event<String, String>>>) b::onExit, "A/B", e -> {}),
                // build must be last.
                () -> assertThrows(NullPointerException.class, () -> b.build(null))
        );
    }

    @Test
    void unknownState_strStates() {
        final String UNKNOWN = "UNKNOWN";
        FSM.BuilderFromStrings<String> b = FSM.builder(SS);
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> b.transition("X", "A/B", UNKNOWN)),
                () -> assertThrows(IllegalArgumentException.class, () -> b.transition("X", UNKNOWN, "A/B")),
                () -> assertThrows(IllegalArgumentException.class, () -> b.transition("X", List.of("A/B"), UNKNOWN)),
                () -> assertThrows(IllegalArgumentException.class, () -> b.transition("X", List.of(UNKNOWN), "A/B")),
                () -> assertThrows(IllegalArgumentException.class, () -> b.internalTransition("X", UNKNOWN)),
                () -> assertThrows(IllegalArgumentException.class, () -> b.internalTransition("X", List.of(UNKNOWN))),
                () -> assertThrows(IllegalArgumentException.class, () -> b.reflexiveTransition("X", UNKNOWN)),
                () -> assertThrows(IllegalArgumentException.class, () -> b.reflexiveTransition("X", List.of(UNKNOWN))),
                () -> assertThrows(IllegalArgumentException.class, () -> b.ignoreInvalidTriggers(UNKNOWN)),
                () -> assertThrows(IllegalArgumentException.class, () -> b.onEnter(UNKNOWN, () -> {})),
                () -> assertThrows(IllegalArgumentException.class, () -> b.onEnter(UNKNOWN, e -> {})),
                () -> assertThrows(IllegalArgumentException.class, () -> b.onExit(UNKNOWN, () -> {})),
                () -> assertThrows(IllegalArgumentException.class, () -> b.onExit(UNKNOWN, e -> {})),
                // build must be last.
                () -> assertThrows(IllegalArgumentException.class, () -> b.build(UNKNOWN))
        );
    }

    @Test
    void inaccessibleState() {
        final String INACC = "A";
        // This test works for FSMs with enum states too since BuilderFromEnum does not override or add new methods.
        FSM.BuilderFromStrings<String> b = FSM.builder(SS);
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> b.transition("X", INACC, "A/B")),
                () -> assertThrows(IllegalArgumentException.class, () -> b.transition("X", "A/B", INACC)),
                () -> assertThrows(IllegalArgumentException.class, () -> b.transition("X", List.of(INACC), "A/B")),
                () -> assertThrows(IllegalArgumentException.class, () -> b.transition("X", List.of("A/B"), INACC)),
                () -> assertThrows(IllegalArgumentException.class, () -> b.internalTransition("X", INACC)),
                () -> assertThrows(IllegalArgumentException.class, () -> b.internalTransition("X", List.of(INACC))),
                () -> assertThrows(IllegalArgumentException.class, () -> b.reflexiveTransition("X", INACC)),
                () -> assertThrows(IllegalArgumentException.class, () -> b.reflexiveTransition("X", List.of(INACC))),
                // build must be last.
                () -> assertThrows(IllegalArgumentException.class, () -> b.build(INACC))
        );
    }

    @Test
    void reuse() {
        // This test works for FSMs with enum states too since BuilderFromEnum does not override or add new methods.
        FSM.BuilderFromStrings<String> b = FSM.builder(SS);
        b.build("A/B");
        assertAll(
                () -> assertThrows(IllegalStateException.class, () -> b.build("A/B")),
                () -> assertThrows(IllegalStateException.class, b::ignoreInvalidTriggers),
                () -> assertThrows(IllegalStateException.class, () -> b.afterAll(() -> {})),
                () -> assertThrows(IllegalStateException.class, () -> b.afterAll(e -> {})),
                () -> assertThrows(IllegalStateException.class, () -> b.beforeAll(() -> {})),
                () -> assertThrows(IllegalStateException.class, () -> b.beforeAll(e -> {})),
                () -> assertThrows(IllegalStateException.class, () -> b.beforeAll(e -> true)),
                () -> assertThrows(IllegalStateException.class, () -> b.transition("X", "A/B", "A/B/C")),
                () -> assertThrows(IllegalStateException.class, () -> b.internalTransition("X", "A/B")),
                () -> assertThrows(IllegalStateException.class, () -> b.reflexiveTransition("X", "A/B")),
                () -> assertThrows(IllegalStateException.class, () -> b.ignoreInvalidTriggers("A/B")),
                () -> assertThrows(IllegalStateException.class, () -> b.onEnter("A/B", () -> {})),
                () -> assertThrows(IllegalStateException.class, () -> b.onEnter("A/B", e -> {})),
                () -> assertThrows(IllegalStateException.class, () -> b.onExit("A/B", () -> {})),
                () -> assertThrows(IllegalStateException.class, () -> b.onExit("A/B", e -> {}))
        );
    }
}
