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
// TODO: add static methods to State interface for comparing states (equality, super, sub, etc.)
// TODO: if a trigger occurs while a state change is in progress, queue it
// TODO: make FSM class generic so Enum states can be compile time type safe?
// TODO: compose functional interfaces to allow multiple (andThen, etc.)
// TODO: option to have triggers be enum too--maybe make the builder not accept any params the have a states(...) method
//       overloaded to accept enum or strings then the returned builder has a narrower set of methods--do something
//       similar for set of triggers
// TODO: linear time algorithms involved with state changes are not great
// TODO: document that onEnter/onExit callback invocation is naive (e.g. "A/B" -> "A/C" calls onExit and onEnter for
//       "A")

/**
 * TODO: mention based loosly on pytransitions
 *
 * @author Robert Russell
 */
public final class FiniteStateMachine {

    /**
     * The string used to separate state names from their parents' names. For example, "A/B" refers to the state named
     * "B" that is a child of the top-level state "A".
     */
    public static final String SUB_STATE_SEP = "/";

    /**
     * A builder for {@code FiniteStateMachine}s.
     *
     * @apiNote This class is not strictly abstract, but it should not and cannot be instantiated directly.
     *
     * @author Robert Russell.
     * @see BuilderFromStrings
     * @see BuilderFromEnum
     */
    static abstract class Builder {

        boolean built = false;
        boolean ignoreMistrigger;
        private Function<Event, Boolean> beforeAll;
        private Consumer<Event> afterAll;

        /**
         * A map mapping states' fully qualified names to corresponding {@link State State} objects.
         */
        final Map<String, State> stateMap;

        /**
         * A list of {@link Transition Transitions} ordered by when they were declared (i.e. the first
         * element/transition in the list was declared first and the last element/transition in the list was declared
         * last). This order is important because it dictates which transition is executed if there are multiple valid
         * transitions for a given trigger and current state. This list is updated as new transitions are added during
         * the building procedure.
         */
        private final List<Transition> transitions = new ArrayList<>();

        /**
         * @param stateMap a map mapping states' fully qualified names to corresponding {@link State State} objects.
         */
        Builder(Map<String, State> stateMap) {
            this.stateMap = stateMap;
        }

        /**
         * Silently ignore invalid triggers regardless of the current state rather than throw an
         * {@link IllegalStateException IllegalStateException}. By "invalid trigger", we mean triggers that are either
         * not associated with any transition in the FSM, or are associated with one or more transitions in the FSM, but
         * those transitions cannot execute in the current state.
         */
        public void ignoreMistrigger() {
            ignoreMistrigger = true;
        }

        /**
         * <p>
         * Register a callback to be run before all transitions/state changes occur. The associated transition/state
         * change context (i.e. {@link Event Event} object) is ignored.
         * </p>
         * <p>
         * The callback should return quickly, lest the rest of the transition/state change procedure will be delayed.
         * </p>
         * <p>
         * Only one before all callback can be registered; multiple calls to any version of the {@code beforeAll} method
         * will result in an {@link IllegalStateException IllegalStateException} to prevent accidentally trying to
         * register multiple before all callbacks.
         * </p>
         *
         * @param callback a callback to be run before all transitions/state changes occur.
         * @throws NullPointerException if the given callback is {@code null}.
         * @throws IllegalStateException if {@code beforeAll} has been called previously.
         */
        public void beforeAll(Runnable callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            beforeAll(event -> {
                callback.run();
                return true;
            });
        }

        /**
         * <p>
         * Register a callback accepting an {@link Event Event} object to be run before all transitions/state changes
         * occur.
         * </p>
         * <p>
         * The callback should return quickly, lest the rest of the transition/state change procedure will be delayed.
         * </p>
         * <p>
         * Only one before all callback can be registered; multiple calls to any version of the {@code beforeAll} method
         * will result in an {@link IllegalStateException IllegalStateException} to prevent accidentally trying to
         * register multiple before all callbacks.
         * </p>
         *
         * @param callback a callback to be run before all transitions/state changes occur.
         * @throws NullPointerException if the given callback is {@code null}.
         * @throws IllegalStateException if {@code beforeAll} has been called previously.
         */
        public void beforeAll(Consumer<Event> callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            beforeAll(event -> {
                callback.accept(event);
                return true;
            });
        }

        /**
         * <p>
         * Register a callback accepting an {@link Event Event} object to be run before all transitions/state changes
         * occur. The callback returns a boolean indicating whether the transition/state change should proceed (true if
         * it should proceed, false if not), effectively allowing one to abort transitions/state changes.
         * </p>
         * <p>
         * The callback should return quickly, lest the rest of the transition/state change procedure will be delayed.
         * </p>
         * <p>
         * Only one before all callback can be registered; multiple calls to any version of the {@code beforeAll} method
         * will result in an {@link IllegalStateException IllegalStateException} to prevent accidentally trying to
         * register multiple before all callbacks.
         * </p>
         *
         * @param callback a callback to be run before all transitions/state changes occur.
         * @throws NullPointerException if the given callback is {@code null}.
         * @throws IllegalStateException if {@code beforeAll} has been called previously.
         */
        public void beforeAll(Function<Event, Boolean> callback) {
            if (beforeAll != null) {
                throw new IllegalStateException("cannot register more than one beforeAll callback");
            }
            beforeAll = Objects.requireNonNull(callback, "callback must be non-null");
        }

        /**
         * <p>
         * Register a callback to be run after all transitions/state changes occur. The associated transition/state
         * change context (i.e. {@link Event Event} object) is ignored.
         * </p>
         * <p>
         * The callback should return quickly, lest the rest of the transition/state change procedure will be delayed.
         * </p>
         * <p>
         * Only one after all callback can be registered; multiple calls to any version of the {@code afterAll} method
         * will result in an {@link IllegalStateException IllegalStateException} to prevent accidentally trying to
         * register multiple after all callbacks.
         * </p>
         *
         * @param callback a callback to be run after all transitions/state changes occur.
         * @throws NullPointerException if the given callback is {@code null}.
         * @throws IllegalStateException if {@code afterAll} has been called previously.
         */
        public void afterAll(Runnable callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            afterAll(event -> callback.run());
        }

        /**
         * <p>
         * Register a callback accepting an {@link Event Event} object to be run after all transitions/state changes
         * occur.
         * </p>
         * <p>
         * The callback should return quickly, lest the rest of the transition/state change procedure will be delayed.
         * </p>
         * <p>
         * Only one after all callback can be registered; multiple calls to any version of the {@code afterAll} method
         * will result in an {@link IllegalStateException IllegalStateException} to prevent accidentally trying to
         * register multiple after all callbacks.
         * </p>
         *
         * @param callback a callback to be run after all transitions/state changes occur.
         * @throws NullPointerException if the given callback is {@code null}.
         * @throws IllegalStateException if {@code afterAll} has been called previously.
         */
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
            if (built) {
                throw new IllegalStateException("build already invoked");
            }
            built = true;
            return new FiniteStateMachine(this, initial);
        }
    }

    /**
     * A builder for {@code FiniteStateMachine}s that uses raw strings to represent states.
     *
     * @author Robert Russell
     */
    public static final class BuilderFromStrings extends Builder {

        /**
         * @apiNote This constructor is not public so the user has to use the
         * {@linkplain #fromStrings(String...) static factory method} instead, thereby encouraging the use of more
         * compact syntax (i.e.
         * <pre>{@code FiniteStateMachine.fromStrings(...)...}</pre>
         * instead of
         * <pre>{@code new FiniteStateMachine.BuilderFromStrings(...)...}</pre>
         * ).
         *
         * @param states an array of all the states for the FSM being built in the form of strings.
         */
        BuilderFromStrings(String... states) {
            super(BuilderFromStrings.strs2Map(
                    Objects.requireNonNull(states, "state machine must have at least one state")
            ));
        }

        /**
         * Silently ignore invalid triggers on the given states rather than throw an
         * {@link IllegalStateException IllegalStateException}. By "invalid trigger", we mean triggers that are either
         * not associated with any transition in the FSM, or are associated with one or more transitions in the FSM, but
         * those transitions cannot execute in the current state.
         *
         * @param states an array of states to ignore invalid triggers on.
         * @throws IllegalArgumentException if any of the given states does not belong to the FSM being built.
         * @throws NullPointerException if the given array of states or any contained state is {@code null}.
         */
        public void ignoreMistrigger(String... states) {
            for (String state : Objects.requireNonNull(states, "states must be non-null")) {
                // This null check is redundant, but makes the error message more descriptive.
                str2State(Objects.requireNonNull(state, "cannot ignore mistriggers on null state"))
                        .ignoreMistrigger = true;
            }
        }

        /**
         * <p>
         * Register a callback to be run after the given state is entered. The associated transition/state change
         * context (i.e. {@link Event Event} object) is ignored.
         * </p>
         * <p>
         * The callback should return quickly, lest the rest of the transition/state change procedure will be delayed.
         * </p>
         * <p>
         * Only one on enter callback can be registered with any one state; multiple calls to any version of the
         * {@code onEnter} method with the same state will result in an
         * {@link IllegalStateException IllegalStateException} to prevent accidentally trying to register multiple on
         * enter callbacks with the same state.
         * </p>
         *
         * @param callback a callback to be run after the given state is entered.
         * @throws NullPointerException if the given state or callback is {@code null}.
         * @throws IllegalArgumentException if the given state does not belong to the FSM being built.
         * @throws IllegalStateException if {@code onEnter} has been called previously with the given state.
         */
        public void onEnter(String state, Runnable callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            onEnter(state, event -> callback.run());
        }

        /**
         * <p>
         * Register a callback accepting an {@link Event Event} object to be run after the given state is entered.
         * </p>
         * <p>
         * The callback should return quickly, lest the rest of the transition/state change procedure will be delayed.
         * </p>
         * <p>
         * Only one on enter callback can be registered with any one state; multiple calls to any version of the
         * {@code onEnter} method with the same state will result in an
         * {@link IllegalStateException IllegalStateException} to prevent accidentally trying to register multiple on
         * enter callbacks with the same state.
         * </p>
         *
         * @param callback a callback to be run after the given state is entered.
         * @throws NullPointerException if the given state or callback is {@code null}.
         * @throws IllegalArgumentException if the given state does not belong to the FSM being built.
         * @throws IllegalStateException if {@code onEnter} has been called previously with the given state.
         */
        public void onEnter(String state, Consumer<Event> callback) {
            // This null check is redundant, but makes the error message more descriptive.
            BaseState s = str2State(Objects.requireNonNull(state, "cannot attach callback to null state"));
            if (s.onEnter != null) {
                throw new IllegalStateException("cannot register more than one onEnter callback per state");
            }
            s.onEnter = Objects.requireNonNull(callback, "callback must be non-null");
        }

        /**
         * <p>
         * Register a callback to be run before the given state is exited. The associated transition/state change
         * context (i.e. {@link Event Event} object) is ignored.
         * </p>
         * <p>
         * The callback should return quickly, lest the rest of the transition/state change procedure will be delayed.
         * </p>
         * <p>
         * Only one on exit callback can be registered with any one state; multiple calls to any version of the
         * {@code onExit} method with the same state will result in an
         * {@link IllegalStateException IllegalStateException} to prevent accidentally trying to register multiple on
         * exit callbacks with the same state.
         * </p>
         *
         * @param callback a callback to be run before the given state is exited.
         * @throws NullPointerException if the given state or callback is {@code null}.
         * @throws IllegalArgumentException if the given state does not belong to the FSM being built.
         * @throws IllegalStateException if {@code onExit} has been called previously with the given state.
         */
        public void onExit(String state, Runnable callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            onExit(state, event -> callback.run());
        }

        /**
         * <p>
         * Register a callback accepting an {@link Event Event} object to be run before the given state is exited.
         * </p>
         * <p>
         * The callback should return quickly, lest the rest of the transition/state change procedure will be delayed.
         * </p>
         * <p>
         * Only one on exit callback can be registered with any one state; multiple calls to any version of the
         * {@code onExit} method with the same state will result in an
         * {@link IllegalStateException IllegalStateException} to prevent accidentally trying to register multiple on
         * exit callbacks with the same state.
         * </p>
         *
         * @param callback a callback to be run before the given state is exited.
         * @throws NullPointerException if the given state or callback is {@code null}.
         * @throws IllegalArgumentException if the given state does not belong to the FSM being built.
         * @throws IllegalStateException if {@code onExit} has been called previously with the given state.
         */
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

        /**
         * <p>
         * Add a new normal (non-internal and non-reflexive) transition to the FSM being built.
         * </p>
         * <p>
         * This method is provided for convenience; it is exactly equivalent to calling
         * {@link #transition(String, List, String)} with the {@code src} argument wrapped in a list.
         * </p>
         *
         * @param trigger the trigger string to associate with the transition.
         * @param src the source state for the transition.
         * @param dst the destination state for the transition.
         * @return a {@link Transition.Builder Transition.Builder} instance (to further customize the transition).
         * @throws NullPointerException if {@code trigger}, {@code src}, or {@code dst} is {@code null}.
         * @throws IllegalArgumentException if the given source or destination state is
         * {@link State#isAccessible() inaccessible}.
         */
        public Transition.Builder<BuilderFromStrings> transition(String trigger, String src, String dst) {
            return transition(trigger, List.of(src), dst);
        }

        /**
         * Add a new normal (non-internal and non-reflexive) transition to the FSM being built.
         *
         * @param trigger the trigger string to associate with the transition.
         * @param srcs a list of source states for the transition.
         * @param dst the destination state for the transition.
         * @return a {@link Transition.Builder Transition.Builder} instance (to further customize the transition).
         * @throws NullPointerException if {@code trigger}, {@code srcs}, or {@code dst} is {@code null}.
         * @throws IllegalArgumentException if any of the given source states or the destination state is
         * {@link State#isAccessible() inaccessible}.
         * @throws IllegalArgumentException if the given list of source states is empty.
         */
        public Transition.Builder<BuilderFromStrings> transition(String trigger, List<String> srcs, String dst) {
            return Transition.Builder.normal(
                    trigger,
                    srcStrs2States(srcs),
                    // This null check is redundant, but makes the error message more descriptive.
                    str2State(Objects.requireNonNull(dst, "dst must be non-null")),
                    this
            );
        }

        /**
         * <p>
         * Add a new internal transition to the FSM being built.
         * </p>
         * <p>
         * An internal transition is a transition in which no state change actually occurs (i.e. {@code onExit} and
         * {@code onEnter} callbacks are not called on any states). As such, internal transitions are only useful for
         * their side effects (callbacks other than {@code onExit} and {@code onEnter}).
         * </p>
         * <p>
         * This method is provided for convenience; it is exactly equivalent to calling
         * {@link #internalTransition(String, List)} with the {@code src} argument wrapped in a list.
         * </p>
         *
         * @param trigger the trigger string to associate with the transition.
         * @param src the source state for the transition.
         * @return a {@link Transition.Builder Transition.Builder} instance (to further customize the transition).
         * @throws NullPointerException if {@code trigger} or {@code src} is {@code null}.
         * @throws IllegalArgumentException if the given source state is {@link State#isAccessible() inaccessible}.
         */
        public Transition.Builder<BuilderFromStrings> internalTransition(String trigger, String src) {
            return internalTransition(trigger, List.of(src));
        }

        /**
         * <p>
         * Add a new internal transition to the FSM being built.
         * </p>
         * <p>
         * An internal transition is a transition in which no state change actually occurs (i.e. {@code onExit} and
         * {@code onEnter} callbacks are not called on any states). As such, internal transitions are only useful for
         * their side effects (callbacks other than {@code onExit} and {@code onEnter}).
         * </p>
         *
         * @param trigger the trigger string to associate with the transition.
         * @param srcs a list of source states for the transition.
         * @return a {@link Transition.Builder Transition.Builder} instance (to further customize the transition).
         * @throws NullPointerException if {@code trigger} or {@code srcs} is {@code null}.
         * @throws IllegalArgumentException if any of the given source states are
         * {@link State#isAccessible() inaccessible}.
         * @throws IllegalArgumentException if the given list of source states is empty.
         */
        public Transition.Builder<BuilderFromStrings> internalTransition(String trigger, List<String> srcs) {
            return Transition.Builder.internal(trigger, srcStrs2States(srcs), this);
        }

        /**
         * <p>
         * Add a new reflexive transition to the FSM being built.
         * </p>
         * <p>
         * A reflexive transition is a transition whose source and destination states are the same. As such, reflexive
         * transitions are only useful for their side effects (callbacks).
         * </p>
         * <p>
         * This method is provided for convenience; it is exactly equivalent to calling
         * {@link #reflexiveTransition(String, List)} with the {@code src} argument wrapped in a list.
         * </p>
         *
         * @param trigger the trigger string to associate with the transition.
         * @param src the source state for the transition.
         * @return a {@link Transition.Builder Transition.Builder} instance (to further customize the transition).
         * @throws NullPointerException if {@code trigger} or {@code src} is {@code null}.
         * @throws IllegalArgumentException if the given source state is {@link State#isAccessible() inaccessible}.
         */
        public Transition.Builder<BuilderFromStrings> reflexiveTransition(String trigger, String src) {
            return reflexiveTransition(trigger, List.of(src));
        }

        /**
         * <p>
         * Add a new reflexive transition to the FSM being built.
         * </p>
         * <p>
         * A reflexive transition is a transition whose source and destination states are the same. As such, reflexive
         * transitions are only useful for their side effects (callbacks).
         * </p>
         *
         * @param trigger the trigger string to associate with the transition.
         * @param srcs a list of source states for the transition.
         * @return a {@link Transition.Builder Transition.Builder} instance (to further customize the transition).
         * @throws NullPointerException if {@code trigger} or {@code srcs} is {@code null}.
         * @throws IllegalArgumentException if any of the given source states are
         * {@link State#isAccessible() inaccessible}.
         * @throws IllegalArgumentException if the given list of source states is empty.
         */
        public Transition.Builder<BuilderFromStrings> reflexiveTransition(String trigger, List<String> srcs) {
            return Transition.Builder.reflexive(trigger, srcStrs2States(srcs), this);
        }

        /**
         * <p>
         * Build the {@code FiniteStateMachine} and initialize it in the given state.
         * </p>
         * <p>
         * Note that the {@code onEnter} callback is <em>not</em> invoked on the initial state as the FSM does not enter
         * the initial state; rather, the FSM comes into existence already in the initial state. If the user needs code
         * in the initial state's {@code onEnter} callback to run when the FSM is initialized, it should be invoked
         * manually.
         * </p>
         *
         * @apiNote We prohibit calling {@code build} more than once because (a) it is almost certainly an error on the
         * part of the user and (b) it could cause issues because the same list of transitions used in the builder is
         * used in the FSM.
         *
         * @param initialState the initial state for the FSM.
         * @return the newly-constructed {@code FiniteStateMachine}.
         * @throws NullPointerException if the given initial state is {@code null}.
         * @throws IllegalArgumentException if the given initial state does not belong to the FSM being built.
         * @throws IllegalStateException if {@code build} has already been called.
         */
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
            // The constructor guarantees states is non-null.
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

    /**
     * A builder for {@code FiniteStateMachine}s that uses the constants of an enum implementor of {@link State State}
     * to represent states.
     *
     * @author Robert Russell
     */
    public static final class BuilderFromEnum<T extends Enum<T> & State> extends Builder {

        /**
         * @apiNote This constructor is not public so the user has to use the
         * {@linkplain #fromEnum(Class) static factory method} instead, thereby encouraging the use of more
         * compact syntax (i.e.
         * <pre>{@code FiniteStateMachine.fromEnum(...)...}</pre>
         * instead of
         * <pre>{@code new FiniteStateMachine.BuilderFromEnum(...)...}</pre>
         * ).
         *
         * @param stateEnum the class of the enum containing all the states for the FSM being built.
         */
        BuilderFromEnum(Class<T> stateEnum) {
            super(Stream.of(stateEnum.getEnumConstants())
                    .collect(Collectors.<State, String, State>toUnmodifiableMap(FiniteStateMachine::fullName, s -> s)));
            if (stateEnum.getEnumConstants().length == 0) {
                throw new IllegalArgumentException("state machine must have at least one state");
            }
        }

        /**
         * <p>
         * Add a new normal (non-internal and non-reflexive) transition to the FSM being built.
         * </p>
         * <p>
         * This method is provided for convenience; it is exactly equivalent to calling
         * {@link #transition(String, List, Enum)} with the {@code src} argument wrapped in a list.
         * </p>
         *
         * @param trigger the trigger string to associate with the transition.
         * @param src the source state for the transition.
         * @param dst the destination state for the transition.
         * @return a {@link Transition.Builder Transition.Builder} instance (to further customize the transition).
         * @throws NullPointerException if {@code trigger}, {@code src}, or {@code dst} is {@code null}.
         * @throws IllegalArgumentException if the given source or destination state is
         * {@link State#isAccessible() inaccessible}.
         */
        public Transition.Builder<BuilderFromEnum<T>> transition(String trigger, T src, T dst) {
            return Transition.Builder.normal(
                    trigger,
                    List.of(src),
                    Objects.requireNonNull(dst, "dst must be non-null"),
                    this
            );
        }

        /**
         * Add a new normal (non-internal and non-reflexive) transition to the FSM being built.
         *
         * @param trigger the trigger string to associate with the transition.
         * @param srcs a list of source states for the transition.
         * @param dst the destination state for the transition.
         * @return a {@link Transition.Builder Transition.Builder} instance (to further customize the transition).
         * @throws NullPointerException if {@code trigger}, {@code srcs}, or {@code dst} is {@code null}.
         * @throws IllegalArgumentException if any of the given source states or the destination state is
         * {@link State#isAccessible() inaccessible}.
         * @throws IllegalArgumentException if the given list of source states is empty.
         */
        public Transition.Builder<BuilderFromEnum<T>> transition(String trigger, List<T> srcs, T dst) {
            return Transition.Builder.normal(
                    trigger,
                    List.copyOf(srcs),
                    Objects.requireNonNull(dst, "dst must be non-null"),
                    this
            );
        }

        /**
         * <p>
         * Add a new internal transition to the FSM being built.
         * </p>
         * <p>
         * An internal transition is a transition in which no state change actually occurs (i.e.
         * {@link State#onExit(Event) onExit} and {@link State#onEnter(Event) onEnter} callbacks are not called on any
         * states). As such, internal transitions are only useful for their side effects (callbacks other than
         * {@code onExit} and {@code onEnter}).
         * </p>
         * <p>
         * This method is provided for convenience; it is exactly equivalent to calling
         * {@link #internalTransition(String, List)} with the {@code src} argument wrapped in a list.
         * </p>
         *
         * @param trigger the trigger string to associate with the transition.
         * @param src the source state for the transition.
         * @return a {@link Transition.Builder Transition.Builder} instance (to further customize the transition).
         * @throws NullPointerException if {@code trigger} or {@code src} is {@code null}.
         * @throws IllegalArgumentException if the given source state is {@link State#isAccessible() inaccessible}.
         */
        public Transition.Builder<BuilderFromEnum<T>> internalTransition(String trigger, T src) {
            return Transition.Builder.internal(trigger, List.of(src), this);
        }

        /**
         * <p>
         * Add a new internal transition to the FSM being built.
         * </p>
         * <p>
         * An internal transition is a transition in which no state change actually occurs (i.e.
         * {@link State#onExit(Event) onExit} and {@link State#onEnter(Event) onEnter} callbacks are not called on any
         * states). As such, internal transitions are only useful for their side effects (callbacks other than
         * {@code onExit} and {@code onEnter}).
         * </p>
         *
         * @param trigger the trigger string to associate with the transition.
         * @param srcs a list of source states for the transition.
         * @return a {@link Transition.Builder Transition.Builder} instance (to further customize the transition).
         * @throws NullPointerException if {@code trigger} or {@code srcs} is {@code null}.
         * @throws IllegalArgumentException if any of the given source states are
         * {@link State#isAccessible() inaccessible}.
         * @throws IllegalArgumentException if the given list of source states is empty.
         */
        public Transition.Builder<BuilderFromEnum<T>> internalTransition(String trigger, List<T> srcs) {
            return Transition.Builder.internal(trigger, List.copyOf(srcs), this);
        }

        /**
         * <p>
         * Add a new reflexive transition to the FSM being built.
         * </p>
         * <p>
         * A reflexive transition is a transition whose source and destination states are the same. As such, reflexive
         * transitions are only useful for their side effects (callbacks).
         * </p>
         * <p>
         * This method is provided for convenience; it is exactly equivalent to calling
         * {@link #reflexiveTransition(String, List)} with the {@code src} argument wrapped in a list.
         * </p>
         *
         * @param trigger the trigger string to associate with the transition.
         * @param src the source state for the transition.
         * @return a {@link Transition.Builder Transition.Builder} instance (to further customize the transition).
         * @throws NullPointerException if {@code trigger} or {@code src} is {@code null}.
         * @throws IllegalArgumentException if the given source state is {@link State#isAccessible() inaccessible}.
         */
        public Transition.Builder<BuilderFromEnum<T>> reflexiveTransition(String trigger, T src) {
            return Transition.Builder.reflexive(trigger, List.of(src), this);
        }

        /**
         * <p>
         * Add a new reflexive transition to the FSM being built.
         * </p>
         * <p>
         * A reflexive transition is a transition whose source and destination states are the same. As such, reflexive
         * transitions are only useful for their side effects (callbacks).
         * </p>
         *
         * @param trigger the trigger string to associate with the transition.
         * @param srcs a list of source states for the transition.
         * @return a {@link Transition.Builder Transition.Builder} instance (to further customize the transition).
         * @throws NullPointerException if {@code trigger} or {@code srcs} is {@code null}.
         * @throws IllegalArgumentException if any of the given source states are
         * {@link State#isAccessible() inaccessible}.
         * @throws IllegalArgumentException if the given list of source states is empty.
         */
        public Transition.Builder<BuilderFromEnum<T>> reflexiveTransition(String trigger, List<T> srcs) {
            return Transition.Builder.reflexive(trigger, List.copyOf(srcs), this);
        }

        // TODO: rename initialState to initial for consistency
        /**
         * <p>
         * Build the {@code FiniteStateMachine} and initialize it in the given state.
         * </p>
         * <p>
         * Note that the {@link State#onEnter(Event) onEnter} callback is <em>not</em> invoked on the initial state as
         * the FSM does not enter the initial state; rather, the FSM comes into existence already in the initial state.
         * If the user needs code in the initial state's {@code onEnter} callback to run when the FSM is initialized, it
         * should be invoked manually.
         * </p>
         *
         * @apiNote We prohibit calling {@code build} more than once because (a) it is almost certainly an error on the
         * part of the user and (b) it could cause issues because the same list of transitions used in the builder is
         * used in the FSM.
         *
         * @param initialState the initial state for the FSM.
         * @return the newly-constructed {@code FiniteStateMachine}.
         * @throws NullPointerException if the given initial state is {@code null}.
         * @throws IllegalStateException if {@code build} has already been called.
         */
        public FiniteStateMachine build(State initialState) {
            return super.build(Objects.requireNonNull(initialState, "initialState must be non-null"));
        }
    }

    /**
     * <p>
     * Get a {@linkplain BuilderFromStrings builder} for a {@code FiniteStateMachine} in which the states are
     * represented by strings.
     * </p>
     * <p>
     * Every given state name must be fully qualified (i.e. contain all its ancestors delimited by
     * {@value #SUB_STATE_SEP}). Ancestor states which are not explicitly provided are implied, but are considered
     * inaccessible (i.e. cannot be entered by the FSM). For example, calling this method with
     * {@code {"A", "A/B", "C/D"}} will create an FSM with four states ("A", "A/B", "C", and "C/D"), all of which are
     * accessible except for "C" (i.e. the FSM cannot (directly) assume state "C", though it <em>can</em> assume state
     * "C/D").
     * </p>
     *
     * @param states an array of all the states for the FSM.
     * @return a builder for an FSM in which the states are represented by strings.
     * @throws NullPointerException if the given string array or any contained string is {@code null}.
     */
    public static BuilderFromStrings fromStrings(String... states) {
        return new BuilderFromStrings(states);
    }

    /**
     * Get a {@linkplain BuilderFromEnum builder} for a {@code FiniteStateMachine} in which the states are represented
     * by constants in the given enum implementing the {@link State State} interface.
     *
     * @param stateEnum the enum class.
     * @param <T> the enum.
     * @return a builder for an FSM in which the states are represented by constants in the given enum implementing the
     * {@link State State} interface.
     * @throws NullPointerException if the given enum class is {@code null}.
     */
    public static <T extends Enum<T> & State> BuilderFromEnum<T> fromEnum(Class<T> stateEnum) {
        return new BuilderFromEnum<>(stateEnum);
    }

    private final boolean ignoreMistrigger;
    private final Function<Event, Boolean> beforeAll;
    private final Consumer<Event> afterAll;

    /**
     * A map mapping states' fully qualified names to corresponding {@link State State} objects.
     */
    private final Map<String, State> stateMap;

    /**
     * A list of {@link Transition Transitions} ordered by when they were declared (i.e. the first
     * element/transition in the list was declared first and the last element/transition in the list was declared
     * last). This order is important because it dictates which transition is executed if there are multiple valid
     * transitions for a given trigger and current state. This list is updated as new transitions are added during
     * the building procedure.
     */
    private final List<Transition> transitions = new ArrayList<>();

    private State currState;

    private FiniteStateMachine(Builder builder, State initial) {
        ignoreMistrigger = builder.ignoreMistrigger;
        beforeAll = builder.beforeAll;
        afterAll = builder.afterAll;
        stateMap = builder.stateMap;
        currState = initial;
    }

    /**
     * Get the {@linkplain State state} the FSM is currently in.
     *
     * @return the {@linkplain State state} the FSM is currently in.
     */
    public State getState() {
        return currState;
    }

    /**
     * <p>
     * Get whether or not the FSM is in the given state.
     * </p>
     * <p>
     * An FSM is considered "in" a given state if that state is equal to or an ancestor of the FSM's
     * {@linkplain #getState() current state}.
     * </p>
     *
     * @param state the state to check if the FSM is in.
     * @return whether or not the FSM is in the given state.
     * @throws NullPointerException if the given state is {@code null}.
     * @throws IllegalArgumentException if the given state does not belong to this FSM.
     */
    public boolean in(String state) {
        return in(FiniteStateMachine.str2State(stateMap, state));
    }

    /**
     * <p>
     * Get whether or not the FSM is in the given {@linkplain State state}.
     * </p>
     * <p>
     * An FSM is considered "in" a given state if that state is equal to or an ancestor of the FSM's
     * {@linkplain #getState() current state}.
     * </p>
     *
     * @param state the state to check if the FSM is in.
     * @return whether or not the FSM is in the given state.
     * @throws NullPointerException if the given state is {@code null}.
     */
    public boolean in(State state) {
        return inLineage(currState, Objects.requireNonNull(state, "state must be non-null"));
    }

    /**
     * <p>
     * Force the FSM into the given state even if a valid transition from the current state to the given state does not
     * exist.
     * </p>
     * <p>
     * Since use of this method bypasses the whole point of using a state machine, its use is discouraged except in
     * rare cases (e.g. if the FSM needs to undergo some sort of reset procedure and enter a certain state). If this
     * method is used, one must be sure to consider the implications of bypassing the predefined transitions.
     * </p>
     * <p>
     * Since a state change that occurs as a result of calling {@code forceTo} is not associated with any transition, no
     * transition callbacks (i.e. before and after callbacks) occur; however, beforeAll and afterAll callbacks on the
     * FSM and enter/exit callbacks on the appropriate states are invoked. The forced state change proceeds even if
     * one of the beforeAll callbacks returns false.
     * </p>
     * <p>
     * {@code forceTo} is a no-op if the given state is the current state.
     * </p>
     *
     * @param state the state to force the FSM into.
     * @throws NullPointerException if the given state is {@code null}.
     * @throws IllegalArgumentException if the given state does not belong to this FSM.
     * @throws IllegalArgumentException if the given state is inaccessible.
     */
    public void forceTo(String state) {
        forceTo(state, Map.of());
    }

    /**
     * <p>
     * Force the FSM into the given state even if a valid transition from the current state to the given state does not
     * exist.
     * </p>
     * <p>
     * Since use of this method bypasses the whole point of using a state machine, its use is discouraged except in
     * rare cases (e.g. if the FSM needs to undergo some sort of reset procedure and enter a certain state). If this
     * method is used, one must be sure to consider the implications of bypassing the predefined transitions.
     * </p>
     * <p>
     * Since a state change that occurs as a result of calling {@code forceTo} is not associated with any transition, no
     * transition callbacks (i.e. before and after callbacks) occur; however, beforeAll and afterAll callbacks on the
     * FSM and enter/exit callbacks on the appropriate states are invoked. The forced state change proceeds even if
     * one of the beforeAll callbacks returns false.
     * </p>
     * <p>
     * {@code forceTo} is a no-op if the given state is the current state.
     * </p>
     *
     * @param state the state to force the FSM into.
     * @param args arguments to put in the {@link Event Event} object so that they might be accessed from callbacks.
     * @throws NullPointerException if the given state is {@code null}.
     * @throws IllegalArgumentException if the given state does not belong to this FSM.
     * @throws IllegalArgumentException if the given state is inaccessible.
     */
    public void forceTo(String state, Map<String, Object> args) {
        forceTo(FiniteStateMachine.str2State(stateMap, state), args);
    }

    /**
     * <p>
     * Force the FSM into the given state even if a valid transition from the current state to the given state does not
     * exist.
     * </p>
     * <p>
     * Since use of this method bypasses the whole point of using a state machine, its use is discouraged except in
     * rare cases (e.g. if the FSM needs to undergo some sort of reset procedure and enter a certain state). If this
     * method is used, one must be sure to consider the implications of bypassing the predefined transitions.
     * </p>
     * <p>
     * Since a state change that occurs as a result of calling {@code forceTo} is not associated with any transition, no
     * transition callbacks (i.e. before and after callbacks) occur; however, beforeAll and afterAll callbacks on the
     * FSM and enter/exit callbacks on the appropriate states are invoked. The forced state change proceeds even if
     * one of the beforeAll callbacks returns false.
     * </p>
     * <p>
     * {@code forceTo} is a no-op if the given state is the current state.
     * </p>
     *
     * @param state the state to force the FSM into.
     * @throws NullPointerException if the given state is {@code null}.
     * @throws IllegalArgumentException if the given state does not belong to this FSM.
     * @throws IllegalArgumentException if the given state is inaccessible.
     */
    public void forceTo(State state) {
        forceTo(state, Map.of());
    }

    /**
     * <p>
     * Force the FSM into the given state even if a valid transition from the current state to the given state does not
     * exist.
     * </p>
     * <p>
     * Since use of this method bypasses the whole point of using a state machine, its use is discouraged except in
     * rare cases (e.g. if the FSM needs to undergo some sort of reset procedure and enter a certain state). If this
     * method is used, one must be sure to consider the implications of bypassing the predefined transitions.
     * </p>
     * <p>
     * Since a state change that occurs as a result of calling {@code forceTo} is not associated with any transition, no
     * transition callbacks (i.e. before and after callbacks) occur; however, beforeAll and afterAll callbacks on the
     * FSM and enter/exit callbacks on the appropriate states are invoked. The forced state change proceeds even if
     * one of the beforeAll callbacks returns false.
     * </p>
     * <p>
     * {@code forceTo} is a no-op if the given state is the current state.
     * </p>
     *
     * @param state the state to force the FSM into.
     * @param args arguments to put in the {@link Event Event} object so that they might be accessed from callbacks.
     * @throws NullPointerException if the given state is {@code null}.
     * @throws IllegalArgumentException if the given state does not belong to this FSM.
     * @throws IllegalArgumentException if the given state is inaccessible.
     */
    public void forceTo(State state, Map<String, Object> args) {
        if (Objects.requireNonNull(state, "cannot force transition to null state") == currState) {
            // no-op if the FSM is already in the given state.
            return;
        }
        if (!stateMap.containsValue(state)) {
            throw new IllegalArgumentException("state does not belong to this FSM");
        }
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
        chainOnExitCallbacks(currState, event);
        currState = state;
        chainOnEnterCallbacks(state, event);
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
            chainOnExitCallbacks(currState, event);
            currState = dst;
            chainOnEnterCallbacks(dst, event);
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

    private static void chainOnExitCallbacks(State state, Event event) {
        if (state != null) {
            // Call the ancestors' callbacks first, as per specification.
            chainOnExitCallbacks(state.getParent(), event);
            state.onExit(event);
        }
    }

    private static void chainOnEnterCallbacks(State state, Event event) {
        if (state != null) {
            // Call the ancestors' callbacks first, as per specification.
            chainOnEnterCallbacks(state.getParent(), event);
            state.onEnter(event);
        }
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
