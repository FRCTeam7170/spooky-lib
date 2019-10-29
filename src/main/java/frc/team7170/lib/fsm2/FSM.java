package frc.team7170.lib.fsm2;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// TODO: getter for ignoreMistrigger
// TODO: thread safety
// TODO: logging
// TODO: document transition resolution
// TODO: add static methods to State interface for comparing states (equality, super, sub, etc.)
// TODO: if a trigger occurs while a state change is in progress, queue it
// TODO: compose functional interfaces to allow multiple (andThen, etc.)
// TODO: document that onEnter/onExit callback invocation is naive (e.g. "A/B" -> "A/C" calls onExit and onEnter for
//       "A")

/**
 * TODO: mention based loosly on pytransitions
 *
 * @author Robert Russell
 */
public final class FSM<S, T> {

    /**
     * The string used to separate state names from their parents' names. For example, "A/B" refers to the state named
     * "B" that is a child of the top-level state "A".
     */
    public static final String SUB_STATE_SEP = "/";

    /**
     * A builder for {@code FSM}s.
     *
     * @apiNote This class is not strictly abstract, but it should not and cannot be instantiated directly.
     *
     * @author Robert Russell.
     * @see BuilderFromStrings
     * @see BuilderFromEnum
     */
    static abstract class Builder<S, T, I extends Builder<S, T, I>> {

        private boolean built = false;
        private boolean ignoreMistrigger;
        private Function<Event, Boolean> beforeAll;
        private Consumer<Event> afterAll;
        final StateMap<S, T> stateMap;

        Builder(StateMap<S, T> sm) {
            this.stateMap = sm;
        }

        private StateBundle<T> resolveState(S s) {
            StateBundle<T> sb = stateMap.s2bundle(s);
            if (sb == null) {
                throw new IllegalArgumentException("state does not belong to this FSM");
            }
            return sb;
        }

        abstract I getThis();

        /**
         * Silently ignore invalid triggers regardless of the current state rather than throw an
         * {@link IllegalStateException IllegalStateException}. By "invalid trigger", we mean triggers that are either
         * not associated with any transition in the FSM, or are associated with one or more transitions in the FSM, but
         * those transitions cannot execute in the current state.
         */
        public I ignoreMistrigger() {
            ignoreMistrigger = true;
            return getThis();
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
        public I beforeAll(Runnable callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            return beforeAll(event -> {
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
        public I beforeAll(Consumer<Event> callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            return beforeAll(event -> {
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
        public I beforeAll(Function<Event, Boolean> callback) {
            if (beforeAll != null) {
                throw new IllegalStateException("cannot register more than one beforeAll callback");
            }
            beforeAll = Objects.requireNonNull(callback, "callback must be non-null");
            return getThis();
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
        public I afterAll(Runnable callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            return afterAll(event -> callback.run());
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
        public I afterAll(Consumer<Event> callback) {
            if (afterAll != null) {
                throw new IllegalStateException("cannot register more than one afterAll callback");
            }
            afterAll = Objects.requireNonNull(callback, "callback must be non-null");
            return getThis();
        }

        private List<StateBundle<T>> resolveSrcs(List<S> srcs) {
            if (Objects.requireNonNull(srcs, "srcs must be non-null").isEmpty()) {
                throw new IllegalArgumentException("transitions must have at least one src state");
            }
            return srcs.stream()
                    .map(s -> Objects.requireNonNull(s, "src must be non-null"))
                    .map(this::resolveState)
                    .map(this::errIfInaccessible)
                    .collect(Collectors.toUnmodifiableList());
        }

        private StateBundle<T> errIfInaccessible(StateBundle<T> sb) {
            if (!sb.state.isAccessible()) {
                throw new IllegalArgumentException("state in transition must be accessible");
            }
            return sb;
        }

        /**
         * <p>
         * Add a new normal (non-internal and non-reflexive) transition to the FSM being built.
         * </p>
         * <p>
         * This method is provided for convenience; it is exactly equivalent to calling
         * {@link #transition(T, List, S)} with the {@code src} argument wrapped in a list.
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
        public Transition.Builder<S, T, I> transition(T trigger, S src, S dst) {
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
        public Transition.Builder<S, T, I> transition(T trigger, List<S> srcs, S dst) {
            return Transition.Builder.normal(
                    Objects.requireNonNull(trigger, "trigger must be non-null"),
                    resolveSrcs(srcs),
                    errIfInaccessible(resolveState(Objects.requireNonNull(dst, "dst must be non-null"))),
                    getThis()
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
         * {@link #internalTransition(T, List)} with the {@code src} argument wrapped in a list.
         * </p>
         *
         * @param trigger the trigger string to associate with the transition.
         * @param src the source state for the transition.
         * @return a {@link Transition.Builder Transition.Builder} instance (to further customize the transition).
         * @throws NullPointerException if {@code trigger} or {@code src} is {@code null}.
         * @throws IllegalArgumentException if the given source state is {@link State#isAccessible() inaccessible}.
         */
        public Transition.Builder<S, T, I> internalTransition(T trigger, S src) {
            return internalTransition(trigger, List.of(src));
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
        public Transition.Builder<S, T, I> internalTransition(T trigger, List<S> srcs) {
            return Transition.Builder.internal(
                    Objects.requireNonNull(trigger, "trigger must be non-null"),
                    resolveSrcs(srcs),
                    getThis()
            );
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
         * {@link #reflexiveTransition(T, List)} with the {@code src} argument wrapped in a list.
         * </p>
         *
         * @param trigger the trigger string to associate with the transition.
         * @param src the source state for the transition.
         * @return a {@link Transition.Builder Transition.Builder} instance (to further customize the transition).
         * @throws NullPointerException if {@code trigger} or {@code src} is {@code null}.
         * @throws IllegalArgumentException if the given source state is {@link State#isAccessible() inaccessible}.
         */
        public Transition.Builder<S, T, I> reflexiveTransition(T trigger, S src) {
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
        public Transition.Builder<S, T, I> reflexiveTransition(T trigger, List<S> srcs) {
            return Transition.Builder.reflexive(
                    Objects.requireNonNull(trigger, "trigger must be non-null"),
                    resolveSrcs(srcs),
                    getThis()
            );
        }

        /**
         * <p>
         * Build the {@code FSM} and initialize it in the given state.
         * </p>
         * <p>
         * Note that the {@link State#onEnter(Event) onEnter} callback is <em>not</em> invoked on the initial state as
         * the FSM does not enter the initial state; rather, the FSM comes into existence already in the initial state.
         * If the user needs code in the initial state's {@code onEnter} callback to run when the FSM is initialized, it
         * should be invoked manually.
         * </p>
         *
         * TODO: this makes no sense
         * @apiNote We prohibit calling {@code build} more than once because (a) it is almost certainly an error on the
         * part of the user and (b) it could cause issues because the same list of transitions used in the builder is
         * used in the FSM.
         *
         * @param initial the initial state for the FSM.
         * @return the newly-constructed {@code FSM}.
         * @throws NullPointerException if the given initial state is {@code null}.
         * @throws IllegalArgumentException if the given initial state does not belong to the FSM being built.
         * @throws IllegalStateException if {@code build} has already been called.
         */
        public FSM<S, T> build(S initial) {
            if (built) {
                throw new IllegalStateException("build already invoked");
            }
            built = true;
            return new FSM<>(
                    this,
                    resolveState(Objects.requireNonNull(initial, "initial state must be non-null")
            ));
        }
    }

    /**
     * A builder for {@code FSM}s that uses raw strings to represent states.
     *
     * @author Robert Russell
     */
    public static final class BuilderFromStrings<T> extends FSM.Builder<String, T, BuilderFromStrings<T>> {

        /**
         * @apiNote This constructor is not public so the user has to use the
         * {@linkplain #builder(String...) static factory method} instead, thereby encouraging the use of more
         * compact syntax (i.e.
         * <pre>{@code FSM.fromStrings(...)...}</pre>
         * instead of
         * <pre>{@code new FSM.BuilderFromStrings(...)...}</pre>
         * ).
         *
         * @param states an array of all the states for the FSM being built in the form of strings.
         */
        BuilderFromStrings(String[] states, Supplier<Map<T, Transition<T>>> mapSupplier) {
            // Static factory methods guarantee states is non-null.
            // StringStateMap constructor guarantees states is not empty.
            super(new StringStateMap<>(states, mapSupplier));
        }

        @Override
        BuilderFromStrings<T> getThis() {
            return this;
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
        public BuilderFromStrings<T> ignoreMistrigger(String... states) {
            for (String state : Objects.requireNonNull(states, "states must be non-null")) {
                // This null check is redundant, but makes the error message more descriptive.
                str2State(Objects.requireNonNull(state, "cannot ignore mistriggers on null state"))
                        .ignoreMistrigger = true;
            }
            return this;
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
        public BuilderFromStrings<T> onEnter(String state, Runnable callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            return onEnter(state, event -> callback.run());
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
        public BuilderFromStrings<T> onEnter(String state, Consumer<Event> callback) {
            // This null check is redundant, but makes the error message more descriptive.
            BaseState s = str2State(Objects.requireNonNull(state, "cannot attach callback to null state"));
            if (s.onEnter != null) {
                throw new IllegalStateException("cannot register more than one onEnter callback per state");
            }
            s.onEnter = Objects.requireNonNull(callback, "callback must be non-null");
            return this;
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
        public BuilderFromStrings<T> onExit(String state, Runnable callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            return onExit(state, event -> callback.run());
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
        public BuilderFromStrings<T> onExit(String state, Consumer<Event> callback) {
            // This null check is redundant, but makes the error message more descriptive.
            BaseState s = str2State(Objects.requireNonNull(state, "cannot attach callback to null state"));
            if (s.onExit != null) {
                throw new IllegalStateException("cannot register more than one onExit callback per state");
            }
            s.onExit = Objects.requireNonNull(callback, "callback must be non-null");
            return this;
        }

        private BaseState str2State(String state) {
            return (BaseState) stateMap.s2state(Objects.requireNonNull(state, "state must be non-null"));
        }

    }

    /**
     * A builder for {@code FSM}s that uses the constants of an enum implementor of {@link State State}
     * to represent states.
     *
     * @author Robert Russell
     */
    public static final class BuilderFromEnum<S extends Enum<S> & State, T>
            extends Builder<S, T, BuilderFromEnum<S, T>> {

        /**
         * @apiNote This constructor is not public so the user has to use the
         * {@linkplain #builder(Class) static factory method} instead, thereby encouraging the use of more
         * compact syntax (i.e.
         * <pre>{@code FSM.fromEnum(...)...}</pre>
         * instead of
         * <pre>{@code new FSM.BuilderFromEnum(...)...}</pre>
         * ).
         *
         * @param stateEnum the class of the enum containing all the states for the FSM being built.
         */
        BuilderFromEnum(Class<S> stateEnum, Supplier<Map<T, Transition<T>>> mapSupplier) {
            // EnumStateMap constructor guarantees stateEnum has > 0 constants.
            super(new EnumStateMap<>(stateEnum, mapSupplier));
        }

        @Override
        BuilderFromEnum<S, T> getThis() {
            return this;
        }
    }

    /**
     * <p>
     * Get a {@linkplain BuilderFromStrings builder} for a {@code FSM} in which the states are
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
    public static BuilderFromStrings<String> builder(String... states) {
        return new BuilderFromStrings<>(
                Objects.requireNonNull(states, "states must be non-null"),
                HashMap::new
        );
    }

    public static <T extends Enum<T>> BuilderFromStrings<T> builder(Class<T> triggerEnum, String... states) {
        Objects.requireNonNull(triggerEnum, "triggerEnum must be non-null");
        return new BuilderFromStrings<>(
                Objects.requireNonNull(states, "states must be non-null"),
                () -> new EnumMap<>(triggerEnum)
        );
    }

    /**
     * Get a {@linkplain BuilderFromEnum builder} for a {@code FSM} in which the states are represented
     * by constants in the given enum implementing the {@link State State} interface.
     *
     * @param stateEnum the enum class.
     * @param <T> the enum.
     * @return a builder for an FSM in which the states are represented by constants in the given enum implementing the
     * {@link State State} interface.
     * @throws NullPointerException if the given enum class is {@code null}.
     */
    public static <S extends Enum<S> & State> BuilderFromEnum<S, String> builder(Class<S> stateEnum) {
        return new BuilderFromEnum<>(
                Objects.requireNonNull(stateEnum, "stateEnum must be non-null"),
                HashMap::new
        );
    }

    public static <S extends Enum<S> & State, T extends Enum<T>> BuilderFromEnum<S, T> builder(
            Class<T> triggerEnum, Class<S> stateEnum
    ) {
        Objects.requireNonNull(triggerEnum, "triggerEnum must be non-null");
        return new BuilderFromEnum<>(
                Objects.requireNonNull(stateEnum, "stateEnum must be non-null"),
                () -> new EnumMap<>(triggerEnum)
        );
    }

    private final boolean ignoreMistrigger;
    private final Function<Event, Boolean> beforeAll;
    private final Consumer<Event> afterAll;
    private final StateMap<S, T> stateMap;
    private StateBundle<T> currSB;

    private FSM(Builder<S, T, ?> builder, StateBundle<T> initial) {
        ignoreMistrigger = builder.ignoreMistrigger;
        beforeAll = builder.beforeAll;
        afterAll = builder.afterAll;
        stateMap = builder.stateMap;
        currSB = initial;
    }

    public S getState() {
        return stateMap.state2s(currSB.state);
    }

    /**
     * Get the {@linkplain State state} the FSM is currently in.
     *
     * @return the {@linkplain State state} the FSM is currently in.
     */
    public State getStateObj() {
        return currSB.state;
    }

    /**
     * <p>
     * Get whether or not the FSM is in the given {@linkplain State state}.
     * </p>
     * <p>
     * An FSM is considered "in" a given state if that state is equal to or an ancestor of the FSM's
     * {@linkplain #getStateObj() current state}.
     * </p>
     *
     * @param state the state to check if the FSM is in.
     * @return whether or not the FSM is in the given state.
     * @throws NullPointerException if the given state is {@code null}.
     * @throws IllegalArgumentException if the given state does not belong to this FSM.
     */
    public boolean in(S state) {
        return inLineage(
                currSB.state,
                stateMap.s2state(Objects.requireNonNull(state, "state must be non-null"))
        );
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
    public void forceTo(S state) {
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
    public void forceTo(S state, Map<String, Object> args) {
        StateBundle<T> sb = stateMap.s2bundle(
                Objects.requireNonNull(state, "cannot force transition to null state")
        );
        if (sb == null) {
            throw new IllegalArgumentException("state does not belong to this FSM");
        }
        if (sb == currSB) {
            // no-op if the FSM is already in the given state.
            return;
        }
        if (!sb.state.isAccessible()) {
            throw new IllegalArgumentException("cannot enter inaccessible state");
        }
        if (args == null) {
            args = Map.of();
        }
        Event<S, T> event = new Event<>(
                this,
                currSB.state,
                Objects.requireNonNull(sb.state, "state must be non-null"),
                null,
                args
        );

        // Callbacks.
        beforeAll(event);  // Cannot abort even if this returns false.
        chainOnExitCallbacks(currSB.state, event);
        currSB = sb;
        chainOnEnterCallbacks(sb.state, event);
        afterAll(event);
    }

    public boolean trigger(T trgr) {
        return trigger(trgr, Map.of());
    }
    // TODO: make sure trigger is valid or document that this will fail if trigger is invalid

    public boolean trigger(T trgr, Map<String, Object> args) {
        Transition<T> transition = currSB.resolveTransition(
                Objects.requireNonNull(trgr, "trigger must be non-null")
        );
        if (transition == null) {
            if (!ignoreMistrigger && !currSB.state.getIgnoreMistrigger()) {
                throw new IllegalStateException(
                        String.format("cannot use trigger '%s' in state '%s'", trgr, FSM.fullName(currSB.state))
                );
            }
            return false;
        }
        return executeTransition(trgr, transition, args);
    }

    private boolean executeTransition(T trgr, Transition<T> transition, Map<String, Object> args) {
        StateBundle<T> dst = transition.resolveDst(currSB);

        // Prepare event object.
        if (args == null) {
            args = Map.of();
        }
        Event<S, T> event = new Event<>(this, currSB.state, dst.state, trgr, args);

        // Callbacks.
        if (!beforeAll(event)) {
            return false;  // Abort.
        }
        if (!transition.before(event)) {
            return false;  // Abort.
        }
        if (!transition.internal) {
            chainOnExitCallbacks(currSB.state, event);
            currSB = dst;
            chainOnEnterCallbacks(dst.state, event);
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
            sb.append(FSM.SUB_STATE_SEP);
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
}
