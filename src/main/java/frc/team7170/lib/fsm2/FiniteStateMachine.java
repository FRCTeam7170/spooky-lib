package frc.team7170.lib.fsm2;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: getter for ignoreMistrigger
// TODO: thread safety
// TODO: logging
// TODO: document transition resolution
public final class FiniteStateMachine {

    public static final String SUB_STATE_SEP = "/";

    static abstract class Builder {

        boolean ignoreMistrigger;
        private Function<Event, Boolean> beforeAll;
        private Consumer<Event> afterAll;
        final Map<String, State> stateMap;
        private final List<Transition> transitions = new ArrayList<>();

        Builder(Map<String, State> stateMap) {
            this.stateMap = stateMap;
        }

        public void ignoreMistrigger() {
            ignoreMistrigger = true;
        }

        public void beforeAll(Runnable callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            beforeAll(event -> {
                callback.run();
                return true;
            });
        }

        public void beforeAll(Consumer<Event> callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            beforeAll(event -> {
                callback.accept(event);
                return true;
            });
        }

        public void beforeAll(Function<Event, Boolean> callback) {
            if (beforeAll != null) {
                throw new IllegalStateException("cannot register more than one beforeAll callback");
            }
            beforeAll = Objects.requireNonNull(callback, "callback must be non-null");
        }

        public void afterAll(Runnable callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            afterAll(event -> callback.run());
        }

        public void afterAll(Consumer<Event> callback) {
            if (afterAll != null) {
                throw new IllegalStateException("cannot register more than one afterAll callback");
            }
            afterAll = Objects.requireNonNull(callback, "callback must be non-null");
        }

        void addTransition(Transition transition) {
            transitions.add(transition);
        }

        FiniteStateMachine build(State initial) {
            return new FiniteStateMachine(this, initial);
        }
    }

    public static final class BuilderFromStrings extends Builder {

        BuilderFromStrings(String... states) {
            super(BuilderFromStrings.strs2Map(
                    Objects.requireNonNull(states, "state machine must have at least one state")
            ));
        }

        public void ignoreMistrigger(String... states) {
            // Do not bother doing anything if we are ignoring mistriggers across the whole FSM.
            if (!ignoreMistrigger) {
                for (String state : Objects.requireNonNull(states, "states must be non-null")) {
                    // This null check is redundant, but makes the error message more descriptive.
                    str2State(Objects.requireNonNull(state, "cannot ignore mistriggers on null state"))
                            .ignoreMistrigger = true;
                }
            }
        }

        public void onEnter(String state, Runnable callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            onEnter(state, event -> callback.run());
        }

        public void onEnter(String state, Consumer<Event> callback) {
            // This null check is redundant, but makes the error message more descriptive.
            BaseState s = str2State(Objects.requireNonNull(state, "cannot attach callback to null state"));
            if (s.onEnter != null) {
                throw new IllegalStateException("cannot register more than one onEnter callback per state");
            }
            s.onEnter = Objects.requireNonNull(callback, "callback must be non-null");
        }

        public void onExit(String state, Runnable callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            onExit(state, event -> callback.run());
        }

        public void onExit(String state, Consumer<Event> callback) {
            // This null check is redundant, but makes the error message more descriptive.
            BaseState s = str2State(Objects.requireNonNull(state, "cannot attach callback to null state"));
            if (s.onExit != null) {
                throw new IllegalStateException("cannot register more than one onExit callback per state");
            }
            s.onExit = Objects.requireNonNull(callback, "callback must be non-null");
        }

        // TODO: rename this to avoid confusion?
        private BaseState str2State(String state) {
            return (BaseState) FiniteStateMachine.str2State(stateMap, state);
        }

        public Transition.Builder<BuilderFromStrings> transition(String trigger, String src, String dst) {
            return transition(trigger, List.of(src), dst);
        }

        public Transition.Builder<BuilderFromStrings> transition(String trigger, List<String> srcs, String dst) {
            return Transition.Builder.normal(
                    trigger,
                    srcStrs2States(srcs),
                    // This null check is redundant, but makes the error message more descriptive.
                    str2State(Objects.requireNonNull(dst, "dst must be non-null")),
                    this
            );
        }

        public Transition.Builder<BuilderFromStrings> internalTransition(String trigger, String src) {
            return internalTransition(trigger, List.of(src));
        }

        public Transition.Builder<BuilderFromStrings> internalTransition(String trigger, List<String> srcs) {
            return Transition.Builder.internal(trigger, srcStrs2States(srcs), this);
        }

        public Transition.Builder<BuilderFromStrings> reflexiveTransition(String trigger, String src) {
            return reflexiveTransition(trigger, List.of(src));
        }

        public Transition.Builder<BuilderFromStrings> reflexiveTransition(String trigger, List<String> srcs) {
            return Transition.Builder.reflexive(trigger, srcStrs2States(srcs), this);
        }

        public FiniteStateMachine build(String initialState) {
            return build(str2State(Objects.requireNonNull(initialState, "initialState must be non-null")));
        }

        private List<State> srcStrs2States(List<String> srcs) {
            return Objects.requireNonNull(srcs, "srcs must be non-null").stream()
                    // This null check is redundant, but makes the error message more descriptive.
                    .map(s -> Objects.requireNonNull(s, "transitions cannot have any null src states"))
                    .map(this::str2State)
                    .collect(Collectors.toList());
        }

        private static Map<String, State> strs2Map(String... states) {
            if (states.length == 0) {
                throw new IllegalArgumentException("state machine must have at least one state");
            }
            // Most applications will not nest states, so this initial size is appropriate.
            Map<String, State> stateMap = new HashMap<>(states.length);
            for (String state : states) {
                BaseState last = null;
                for (String seg : BuilderFromStrings.getLineage(
                        Objects.requireNonNull(state, "state must be null-null")
                )) {
                    if (!stateMap.containsKey(seg)) {
                        last = new BaseState(seg.substring(seg.lastIndexOf(FiniteStateMachine.SUB_STATE_SEP)+1), last);
                        stateMap.put(seg, last);
                    }
                }
                // getLineage always returns a list with at least one element (i.e. at least the leaf state); if that
                // state is already in the stateMap, then it must have been a duplicate and last will not have been set.
                if (last == null) {
                    throw new IllegalArgumentException(String.format("duplicate state '%s'", state));
                }
                // Make sure the leaf state is accessible (by default it is not so that parent states not explicitly
                // specified cannot be entered except through a child state).
                last.accessible = true;
            }
            return stateMap;
        }

        private static List<String> getLineage(String state) {
            List<String> list = new ArrayList<>();
            int idx = state.indexOf(FiniteStateMachine.SUB_STATE_SEP);
            while (idx != -1) {
                list.add(state.substring(0, idx));
                idx = state.indexOf(FiniteStateMachine.SUB_STATE_SEP, idx+1);
            }
            list.add(state);
            return list;
        }
    }

    public static final class BuilderFromEnum<T extends Enum<T> & State> extends Builder {

        BuilderFromEnum(Class<T> stateEnum) {
            super(Stream.of(stateEnum.getEnumConstants())
                    .collect(Collectors.<State, String, State>toUnmodifiableMap(FiniteStateMachine::fullName, s -> s)));
            if (stateEnum.getEnumConstants().length == 0) {
                throw new IllegalArgumentException("state machine must have at least one state");
            }
        }

        public Transition.Builder<BuilderFromEnum<T>> transition(String trigger, T src, T dst) {
            return Transition.Builder.normal(
                    trigger,
                    List.of(src),
                    Objects.requireNonNull(dst, "dst must be non-null"),
                    this
            );
        }

        public Transition.Builder<BuilderFromEnum<T>> transition(String trigger, List<T> srcs, T dst) {
            return Transition.Builder.normal(
                    trigger,
                    List.copyOf(srcs),
                    Objects.requireNonNull(dst, "dst must be non-null"),
                    this
            );
        }

        public Transition.Builder<BuilderFromEnum<T>> internalTransition(String trigger, T src) {
            return Transition.Builder.internal(trigger, List.of(src), this);
        }

        public Transition.Builder<BuilderFromEnum<T>> internalTransition(String trigger, List<T> srcs) {
            return Transition.Builder.internal(trigger, List.copyOf(srcs), this);
        }

        public Transition.Builder<BuilderFromEnum<T>> reflexiveTransition(String trigger, T src) {
            return Transition.Builder.reflexive(trigger, List.of(src), this);
        }

        public Transition.Builder<BuilderFromEnum<T>> reflexiveTransition(String trigger, List<T> srcs) {
            return Transition.Builder.reflexive(trigger, List.copyOf(srcs), this);
        }

        public FiniteStateMachine build(State initialState) {
            return super.build(Objects.requireNonNull(initialState, "initialState must be non-null"));
        }
    }

    public static BuilderFromStrings fromStrings(String... states) {
        return new BuilderFromStrings(states);
    }

    public static <T extends Enum<T> & State> BuilderFromEnum<T> fromEnum(Class<T> stateEnum) {
        return new BuilderFromEnum<>(stateEnum);
    }

    private final boolean ignoreMistrigger;
    private final Function<Event, Boolean> beforeAll;
    private final Consumer<Event> afterAll;
    private final Map<String, State> stateMap;
    private final List<Transition> transitions = new ArrayList<>();

    private State currState;

    private FiniteStateMachine(Builder builder, State initial) {
        ignoreMistrigger = builder.ignoreMistrigger;
        beforeAll = builder.beforeAll;
        afterAll = builder.afterAll;
        stateMap = builder.stateMap;
        currState = initial;
    }

    public State getState() {
        return currState;
    }

    public boolean in(String state) {
        return in(FiniteStateMachine.str2State(stateMap, state));
    }

    public boolean in(State state) {
        return inLineage(currState, Objects.requireNonNull(state, "state must be non-null"));
    }

    public void forceTo(String state) {
        forceTo(state, Map.of());
    }

    public void forceTo(String state, Map<String, Object> args) {
        forceTo(FiniteStateMachine.str2State(stateMap, state), args);
    }

    public void forceTo(State state) {
        forceTo(state, Map.of());
    }

    // TODO: make sure state is not current state
    public void forceTo(State state, Map<String, Object> args) {
        if (!state.isAccessible()) {
            throw new IllegalArgumentException("cannot enter inaccessible state");
        }
        if (args == null) {
            args = Map.of();
        }
        Event event = new Event(
                this,
                currState,
                Objects.requireNonNull(state, "state must be non-null"),
                null,
                null,
                args
        );

        // Callbacks.
        beforeAll(event);  // Cannot abort even if this returns false.
        currState.onExit(event);
        currState = state;
        state.onEnter(event);
        afterAll(event);
    }

    public boolean trigger(String trgr) {
        return trigger(trgr, Map.of());
    }

    // TODO: make sure trigger is valid or document that this will fail if trigger is invalid
    public boolean trigger(String trgr, Map<String, Object> args) {
        Transition transition = findValidTransition(Objects.requireNonNull(trgr, "trigger must be non-null"));
        if (transition == null) {
            if (!ignoreMistrigger && !currState.getIgnoreMistrigger()) {
                throw new IllegalStateException(
                        String.format("cannot use trigger '%s' in state '%s'", trgr, currState.name())
                );
            }
            return false;
        }
        return executeTransition(trgr, transition, args);
    }

    private Transition findValidTransition(String trigger) {
        for (Transition transition : transitions) {
            if (transition.canExecute(trigger, currState)) {
                return transition;
            }
        }
        return null;
    }

    private boolean executeTransition(String trgr, Transition transition, Map<String, Object> args) {
        // Determine the destination state.
        State dst;
        if (transition.isInternal() || transition.isReflexive()) {
            dst = currState;
        } else {
            dst = transition.getDst();
        }

        // Prepare event object.
        if (args == null) {
            args = Map.of();
        }
        Event event = new Event(this, currState, dst, transition, trgr, args);

        // Callbacks.
        if (!beforeAll(event)) {
            return false;  // Abort.
        }
        if (!transition.before(event)) {
            return false;  // Abort.
        }
        if (!transition.isInternal()) {
            currState.onExit(event);
            currState = dst;
            dst.onEnter(event);
        }
        transition.after(event);
        afterAll(event);
        return true;
    }

    private boolean beforeAll(Event event) {
        return beforeAll == null || beforeAll.apply(event);
    }

    private void afterAll(Event event) {
        if (afterAll != null) {
            afterAll.accept(event);
        }
    }

    public static String fullName(State state) {
        StringBuilder sb = new StringBuilder();
        sb.append(state.name());
        state = state.getParent();
        while (state != null) {
            sb.append(FiniteStateMachine.SUB_STATE_SEP);
            sb.append(state.name());
            state = state.getParent();
        }
        return sb.toString();
    }

    private static boolean inLineage(State q, State l) {
        for (; q != null; q = q.getParent()) {
            if (q == l) {
                return true;
            }
        }
        return false;
    }

    private static State str2State(Map<String, State> map, String state) {
        State s = map.get(Objects.requireNonNull(state, "state must be non-null"));
        if (s == null) {
            throw new IllegalArgumentException(String.format("no state named '%s'", state));
        }
        return s;
    }
}
