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

/**
 * TODO: mention:
 *  based loosly on pytransitions
 *  complex build procedure
 *  transition resolution rules and performance
 *  state naming?
 *  types of transitions?
 *  callback order (onEnter/onExit callback invocation is naive (e.g. "A/B" -> "A/C" calls onExit and onEnter for "A"))
 *  example usage
 *
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
     * @apiNote Calling methods after {@link #build(Object) build} is prohibited in order to enforce the property that
     * {@code FSM}'s are immutable except for their current state.
     *
     * @param <S> the state type.
     * @param <T> the trigger type.
     * @param <I> this type.
     *
     * @author Robert Russell.
     * @see BuilderFromStrings
     * @see BuilderFromEnum
     */
    static abstract class Builder<S, T, I extends Builder<S, T, I>> {

        /**
         * Whether or not {@link #build(Object) build} has been called.
         */
        private boolean built = false;

        private boolean ignoreMistrigger;
        private Function<Event, Boolean> beforeAll;
        private Consumer<Event> afterAll;
        final StateMap<S, T> stateMap;

        Builder(StateMap<S, T> sm) {
            this.stateMap = sm;
        }

        /**
         * Convert the given state to its corresponding {@link StateBundle StateBundle}.
         *
         * @param state the state.
         * @return the resolved {@code StateBundle}.
         * @throws IllegalArgumentException if the given state does not belong to this FSM.
         */
        private StateBundle<T> resolveState(S state) {
            StateBundle<T> sb = stateMap.s2bundle(state);
            if (sb == null) {
                throw new IllegalArgumentException("state does not belong to this FSM");
            }
            return sb;
        }

        /**
         * Get this {@code Builder} as type {@code I}.
         *
         * @apiNote This is a weird way of doing this, but it avoids having to cast {@code this} to type {@code I} and
         * suppress unchecked warnings.
         *
         * @return this {@code Builder} as type {@code I}.
         */
        abstract I getThis();

        /**
         * Silently ignore invalid triggers regardless of the current state rather than throw an
         * {@link IllegalStateException IllegalStateException} if an invalid trigger occurs. By "invalid trigger", we
         * mean triggers that are either not associated with any transition in the FSM, or are associated with one or
         * more transitions in the FSM, but those transitions cannot execute in the current state.
         *
         * @return this builder.
         * @throws IllegalStateException if {@link #build build} has already been called.
         */
        public I ignoreMistrigger() {
            requireNotBuilt();
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
         * @return this builder.
         * @throws NullPointerException if the given callback is {@code null}.
         * @throws IllegalStateException if {@code beforeAll} has been called previously.
         * @throws IllegalStateException if {@link #build build} has already been called.
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
         * @return this builder.
         * @throws NullPointerException if the given callback is {@code null}.
         * @throws IllegalStateException if {@code beforeAll} has been called previously.
         * @throws IllegalStateException if {@link #build build} has already been called.
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
         * @return this builder.
         * @throws NullPointerException if the given callback is {@code null}.
         * @throws IllegalStateException if {@code beforeAll} has been called previously.
         * @throws IllegalStateException if {@link #build build} has already been called.
         */
        public I beforeAll(Function<Event, Boolean> callback) {
            requireNotBuilt();
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
         * @return this builder.
         * @throws NullPointerException if the given callback is {@code null}.
         * @throws IllegalStateException if {@code afterAll} has been called previously.
         * @throws IllegalStateException if {@link #build build} has already been called.
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
         * @return this builder.
         * @throws NullPointerException if the given callback is {@code null}.
         * @throws IllegalStateException if {@code afterAll} has been called previously.
         * @throws IllegalStateException if {@link #build build} has already been called.
         */
        public I afterAll(Consumer<Event> callback) {
            requireNotBuilt();
            if (afterAll != null) {
                throw new IllegalStateException("cannot register more than one afterAll callback");
            }
            afterAll = Objects.requireNonNull(callback, "callback must be non-null");
            return getThis();
        }

        /**
         * Convert the list of source states to their corresponding {@link StateBundle StateBundles}.
         *
         * @param srcs the list of source states.
         * @return the resolved list of {@code StateBundles}
         * @throws NullPointerException if the given list of source states or any contained source state is
         * {@code null}.
         * @throws IllegalArgumentException if any source state in the given list of source states does not belong to
         * this FSM.
         * @throws IllegalArgumentException if any source state in the given list of source states is inaccessible.
         */
        private List<StateBundle<T>> resolveSrcs(List<S> srcs) {
            if (Objects.requireNonNull(srcs, "srcs must be non-null").isEmpty()) {
                throw new IllegalArgumentException("transitions must have at least one src state");
            }
            return srcs.stream()
                    .map(s -> Objects.requireNonNull(s, "src must be non-null"))
                    .map(this::resolveState)
                    .map(this::requireAccessible)
                    .collect(Collectors.toUnmodifiableList());
        }

        /**
         * Require that the state in the given {@link StateBundle StateBundle} be
         * {@linkplain State#isAccessible() accessible} by throwing an error if it is not.
         *
         * @apiNote This method is meant to be similar to {@link Objects#requireNonNull(Object)}.
         *
         * @param sb the {@code StateBundle}.
         * @return the given {@code StateBundle}.
         * @throws IllegalArgumentException if the state in the given {@code StateBundle} is not accessible.
         */
        private StateBundle<T> requireAccessible(StateBundle<T> sb) {
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
         * This method is provided for convenience; it is exactly equivalent to calling {@link #transition(T, List, S)}
         * with the {@code src} argument wrapped in a list.
         * </p>
         *
         * @param trigger the trigger to associate with the transition.
         * @param src the source state for the transition.
         * @param dst the destination state for the transition.
         * @return a {@link Transition.Builder Transition.Builder} instance (to further customize the transition).
         * @throws NullPointerException if {@code trigger}, {@code src}, or {@code dst} is {@code null}.
         * @throws IllegalArgumentException if the given source or destination state does not belong to this FSM.
         * @throws IllegalArgumentException if the given source or destination state is not
         * {@link State#isAccessible() accessible}.
         * @throws IllegalStateException if {@link #build build} has already been called.
         */
        public Transition.Builder<S, T, I> transition(T trigger, S src, S dst) {
            return transition(trigger, List.of(src), dst);
        }

        /**
         * Add a new normal (non-internal and non-reflexive) transition to the FSM being built.
         *
         * @param trigger the trigger to associate with the transition.
         * @param srcs a list of source states for the transition.
         * @param dst the destination state for the transition.
         * @return a {@link Transition.Builder Transition.Builder} instance (to further customize the transition).
         * @throws NullPointerException if {@code trigger}, {@code srcs}, or {@code dst} is {@code null}.
         * @throws IllegalArgumentException if any of the given source states or the destination state does not belong
         * to this FSM.
         * @throws IllegalArgumentException if any of the given source states or the destination state is not
         * {@link State#isAccessible() accessible}.
         * @throws IllegalArgumentException if the given list of source states is empty.
         * @throws IllegalStateException if {@link #build build} has already been called.
         */
        public Transition.Builder<S, T, I> transition(T trigger, List<S> srcs, S dst) {
            requireNotBuilt();
            return Transition.Builder.normal(
                    Objects.requireNonNull(trigger, "trigger must be non-null"),
                    resolveSrcs(srcs),
                    requireAccessible(resolveState(Objects.requireNonNull(dst, "dst must be non-null"))),
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
         * @param trigger the trigger to associate with the transition.
         * @param src the source state for the transition.
         * @return a {@link Transition.Builder Transition.Builder} instance (to further customize the transition).
         * @throws NullPointerException if {@code trigger} or {@code src} is {@code null}.
         * @throws IllegalArgumentException if the given source state does not belong to this FSM.
         * @throws IllegalArgumentException if the given source state is not {@link State#isAccessible() accessible}.
         * @throws IllegalStateException if {@link #build build} has already been called.
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
         * @param trigger the trigger to associate with the transition.
         * @param srcs a list of source states for the transition.
         * @return a {@link Transition.Builder Transition.Builder} instance (to further customize the transition).
         * @throws NullPointerException if {@code trigger} or {@code srcs} is {@code null}.
         * @throws IllegalArgumentException if any of the given source states does not belong to this FSM.
         * @throws IllegalArgumentException if any of the given source states are not
         * {@link State#isAccessible() accessible}.
         * @throws IllegalArgumentException if the given list of source states is empty.
         * @throws IllegalStateException if {@link #build build} has already been called.
         */
        public Transition.Builder<S, T, I> internalTransition(T trigger, List<S> srcs) {
            requireNotBuilt();
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
         * @param trigger the trigger to associate with the transition.
         * @param src the source state for the transition.
         * @return a {@link Transition.Builder Transition.Builder} instance (to further customize the transition).
         * @throws NullPointerException if {@code trigger} or {@code src} is {@code null}.
         * @throws IllegalArgumentException if the given source state does not belong to this FSM.
         * @throws IllegalArgumentException if the given source state is not {@link State#isAccessible() accessible}.
         * @throws IllegalStateException if {@link #build build} has already been called.
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
         * @throws IllegalArgumentException if any of the given source states does not belong to this FSM.
         * @throws IllegalArgumentException if any of the given source states are not
         * {@link State#isAccessible() accessible}.
         * @throws IllegalArgumentException if the given list of source states is empty.
         * @throws IllegalStateException if {@link #build build} has already been called.
         */
        public Transition.Builder<S, T, I> reflexiveTransition(T trigger, List<S> srcs) {
            requireNotBuilt();
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
         * @param initial the initial state for the FSM.
         * @return the newly-constructed {@code FSM}.
         * @throws NullPointerException if the given initial state is {@code null}.
         * @throws IllegalArgumentException if the given initial state does not belong to the FSM being built.
         * @throws IllegalStateException if {@code build} has already been called.
         */
        public FSM<S, T> build(S initial) {
            requireNotBuilt();
            built = true;
            return new FSM<>(
                    this,
                    resolveState(Objects.requireNonNull(initial, "initial state must be non-null")
            ));
        }

        void requireNotBuilt() {
            if (built) {
                throw new IllegalStateException("build already invoked");
            }
        }
    }

    /**
     * A builder for {@code FSM}s that uses strings to represent states.
     *
     * @author Robert Russell
     */
    public static final class BuilderFromStrings<T> extends FSM.Builder<String, T, BuilderFromStrings<T>> {

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
         * {@link IllegalStateException IllegalStateException} if an invalid trigger occurs. By "invalid trigger", we
         * mean triggers that are either not associated with any transition in the FSM, or are associated with one or
         * more transitions in the FSM, but those transitions cannot execute in the current state.
         *
         * @param states an array of states to ignore invalid triggers on.
         * @return this builder.
         * @throws IllegalArgumentException if any of the given states does not belong to the FSM being built.
         * @throws NullPointerException if the given array of states or any contained state is {@code null}.
         * @throws IllegalStateException if {@link #build build} has already been called.
         */
        public BuilderFromStrings<T> ignoreMistrigger(String... states) {
            requireNotBuilt();
            for (String state : Objects.requireNonNull(states, "states must be non-null")) {
                str2state(Objects.requireNonNull(state, "cannot ignore invalid triggers on null state"))
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
         * @param state the state to register a callback with.
         * @param callback a callback to be run after the given state is entered.
         * @return this builder.
         * @throws NullPointerException if the given state or callback is {@code null}.
         * @throws IllegalArgumentException if the given state does not belong to the FSM being built.
         * @throws IllegalStateException if {@code onEnter} has been called previously with the given state.
         * @throws IllegalStateException if {@link #build build} has already been called.
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
         * @param state the state to register a callback with.
         * @param callback a callback to be run after the given state is entered.
         * @return this builder.
         * @throws NullPointerException if the given state or callback is {@code null}.
         * @throws IllegalArgumentException if the given state does not belong to the FSM being built.
         * @throws IllegalStateException if {@code onEnter} has been called previously with the given state.
         * @throws IllegalStateException if {@link #build build} has already been called.
         */
        public BuilderFromStrings<T> onEnter(String state, Consumer<Event> callback) {
            requireNotBuilt();
            BaseState s = str2state(Objects.requireNonNull(state, "cannot attach callback to null state"));
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
         * @param state the state to register a callback with.
         * @param callback a callback to be run before the given state is exited.
         * @return this builder.
         * @throws NullPointerException if the given state or callback is {@code null}.
         * @throws IllegalArgumentException if the given state does not belong to the FSM being built.
         * @throws IllegalStateException if {@code onExit} has been called previously with the given state.
         * @throws IllegalStateException if {@link #build build} has already been called.
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
         * @param state the state to register a callback with.
         * @param callback a callback to be run before the given state is exited.
         * @return this builder.
         * @throws NullPointerException if the given state or callback is {@code null}.
         * @throws IllegalArgumentException if the given state does not belong to the FSM being built.
         * @throws IllegalStateException if {@code onExit} has been called previously with the given state.
         * @throws IllegalStateException if {@link #build build} has already been called.
         */
        public BuilderFromStrings<T> onExit(String state, Consumer<Event> callback) {
            requireNotBuilt();
            BaseState s = str2state(Objects.requireNonNull(state, "cannot attach callback to null state"));
            if (s.onExit != null) {
                throw new IllegalStateException("cannot register more than one onExit callback per state");
            }
            s.onExit = Objects.requireNonNull(callback, "callback must be non-null");
            return this;
        }

        private BaseState str2state(String state) {
            return (BaseState) stateMap.s2state(state);
        }

    }

    /**
     * A builder for {@code FSM}s that uses the constants of an enum implementor of {@link State State} to represent
     * states.
     *
     * @author Robert Russell
     */
    public static final class BuilderFromEnum<S extends Enum<S> & State, T>
            extends Builder<S, T, BuilderFromEnum<S, T>> {

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
     * Get a {@linkplain BuilderFromStrings builder} for an {@code FSM} in which the states and triggers are represented
     * by strings.
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
     * @return a builder for an {@code FSM} in which the states and triggers are represented by strings.
     * @throws NullPointerException if the given string array or any contained string is {@code null}.
     */
    public static BuilderFromStrings<String> builder(String... states) {
        return new BuilderFromStrings<>(
                Objects.requireNonNull(states, "states must be non-null"),
                HashMap::new
        );
    }

    /**
     * <p>
     * Get a {@linkplain BuilderFromStrings builder} for an {@code FSM} in which the states are represented by strings
     * and the triggers are represented by enum constants.
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
     * @param <T> the trigger type.
     * @param triggerEnum the class object of the enum whose constants are to be used for triggers.
     * @param states an array of all the states for the FSM.
     * @return a builder for an {@code FSM} in which the states are represented by strings and the triggers are
     * represented by enum constants.
     * @throws NullPointerException if the given trigger enum class is {@code null}.
     * @throws NullPointerException if the given string array or any contained string is {@code null}.
     */
    public static <T extends Enum<T>> BuilderFromStrings<T> builder(Class<T> triggerEnum, String... states) {
        Objects.requireNonNull(triggerEnum, "triggerEnum must be non-null");
        return new BuilderFromStrings<>(
                Objects.requireNonNull(states, "states must be non-null"),
                () -> new EnumMap<>(triggerEnum)
        );
    }

    /**
     * Get a {@linkplain BuilderFromEnum builder} for an {@code FSM} in which the states are represented
     * by constants in the given enum implementing the {@link State State} interface and the triggers are represented by
     * strings.
     *
     * @param <S> the state type.
     * @param stateEnum the class object of the enum whose constants are to be used for states.
     * @return a builder for an {@code FSM} in which the states are represented by constants in the given enum
     * implementing the {@code State} interface and the triggers are represented by strings.
     * @throws NullPointerException if the given state enum class is {@code null}.
     */
    public static <S extends Enum<S> & State> BuilderFromEnum<S, String> builder(Class<S> stateEnum) {
        return new BuilderFromEnum<>(
                Objects.requireNonNull(stateEnum, "stateEnum must be non-null"),
                HashMap::new
        );
    }

    /**
     * Get a {@linkplain BuilderFromEnum builder} for an {@code FSM} in which the states are represented
     * by constants in the given enum implementing the {@link State State} interface and the triggers are represented by
     * constants in the other given enum.
     *
     * @param <S> the state type.
     * @param <T> the trigger type.
     * @param triggerEnum the class object of the enum whose constants are to be used for triggers.
     * @param stateEnum the class object of the enum whose constants are to be used for states.
     * @return a builder for an {@code FSM} in which the states are represented by constants in the given enum
     * implementing the {@code State} interface and the triggers are represented by constants in the other given enum.
     * @throws NullPointerException if the given trigger enum class is {@code null}.
     * @throws NullPointerException if the given state enum class is {@code null}.
     */
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

    /**
     * Get the state the {@code FSM} is currently in.
     *
     * @return the state the {@code FSM} is currently in.
     */
    public S getState() {
        return stateMap.state2s(currSB.state);
    }

    /**
     * Get the {@linkplain State state} the {@code FSM} is currently in.
     *
     * @return the {@linkplain State state} the {@code FSM} is currently in.
     */
    public State getStateObj() {
        return currSB.state;
    }

    /**
     * <p>
     * Get whether or not the {@code FSM} is in the given state.
     * </p>
     * <p>
     * An {@code FSM} is considered "in" a given state if that state is equal to or an ancestor of the {@code FSM}'s
     * {@linkplain #getStateObj() current state}.
     * </p>
     *
     * @param state the state to check if the {@code FSM} is in.
     * @return whether or not the {@code FSM} is in the given state.
     * @throws NullPointerException if the given state is {@code null}.
     * @throws IllegalArgumentException if the given state does not belong to this {@code FSM}.
     */
    public boolean in(S state) {
        return State.inLineage(
                currSB.state,
                stateMap.s2state(Objects.requireNonNull(state, "state must be non-null"))
        );
    }

    /**
     * <p>
     * Force the {@code FSM} into the given ({@linkplain State#isAccessible() accessible}) state even if a valid
     * transition from the current state to the given state does not exist.
     * </p>
     * <p>
     * Since use of this method bypasses the whole point of using a state machine, its use is discouraged except in
     * rare cases (e.g. if the {@code FSM} needs to undergo some sort of reset procedure and enter a certain state). If
     * this method is used, one must be sure to consider the implications of bypassing the predefined transitions.
     * </p>
     * <p>
     * Since a state change that occurs as a result of calling {@code forceTo} is not associated with any transition, no
     * transition callbacks (i.e. before and after callbacks) occur; however, beforeAll and afterAll callbacks on the
     * {@code FSM} and enter/exit callbacks on the appropriate states are invoked. The forced state change proceeds even
     * if one of the beforeAll callbacks returns false.
     * </p>
     * <p>
     * {@code forceTo} is a no-op if the given state is the current state.
     * </p>
     *
     * @param state the state to force the {@code FSM} into.
     * @throws NullPointerException if the given state is {@code null}.
     * @throws IllegalArgumentException if the given state does not belong to this {@code FSM}.
     * @throws IllegalArgumentException if the given state is inaccessible.
     */
    public void forceTo(S state) {
        forceTo(state, Map.of());
    }

    /**
     * <p>
     * Force the {@code FSM} into the given ({@linkplain State#isAccessible() accessible}) state even if a valid
     * transition from the current state to the given state does not exist.
     * </p>
     * <p>
     * Since use of this method bypasses the whole point of using a state machine, its use is discouraged except in
     * rare cases (e.g. if the {@code FSM} needs to undergo some sort of reset procedure and enter a certain state). If
     * this method is used, one must be sure to consider the implications of bypassing the predefined transitions.
     * </p>
     * <p>
     * Since a state change that occurs as a result of calling {@code forceTo} is not associated with any transition, no
     * transition callbacks (i.e. before and after callbacks) occur; however, beforeAll and afterAll callbacks on the
     * {@code FSM} and enter/exit callbacks on the appropriate states are invoked. The forced state change proceeds even
     * if one of the beforeAll callbacks returns false.
     * </p>
     * <p>
     * {@code forceTo} is a no-op if the given state is the current state.
     * </p>
     *
     * @param state the state to force the {@code FSM} into.
     * @param args arguments to put in the {@link Event Event} object so that they might be accessed from callbacks.
     * @throws NullPointerException if the given state is {@code null}.
     * @throws IllegalArgumentException if the given state does not belong to this {@code FSM}.
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
                sb.state,
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
                        String.format("cannot use trigger '%s' in state '%s'", trgr, State.fullName(currSB.state))
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

}
