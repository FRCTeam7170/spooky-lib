package frc.team7170.lib.fsm2;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// TODO: logging

/**
 * <p>
 * {@code FSM} is a general-purpose, finite state machine implementation. {@code FSM}s are constructed using a builder
 * and builders can be attained via the static methods named "{@code builder}".
 * </p>
 * <p>
 * {@code FSM}s are not thread-safe by default, thought they can be made thread-safe by externally synchronizing access
 * to all exposed (public) methods.
 * </p>
 * <p>
 * This class is loosely based on <a href="https://github.com/pytransitions/transitions">pytransitions</a>.
 * </p>
 *
 * <h2>Definitions/Concepts</h2>
 * <p>
 * {@code FSM}s have three main concepts:
 * <dl>
 *     <dt><strong>{@linkplain State State}</strong></dt>
 *     <dd>
 *         A state the {@code FSM} can assume. States are hierarchical in that a state may or may not have a single
 *         {@linkplain State#getParent() parent state}. A <em>top-level</em> state is one with no parent. An {@code FSM}
 *         is considered <em>{@link #in(Object) in}</em> any given state if the {@code FSM}'s current state is equal to
 *         the given state or is a descendent of the given state. Every state has a {@linkplain State#name() name}
 *         containing no slashes ({@value #SUB_STATE_SEP}). A state's
 *         <em>{@linkplain State#fullName(State) full name}</em> is a file path-like name consisting of its ancestors'
 *         names delimited by {@value #SUB_STATE_SEP} (e.g. the full name "a/b" refers to a state named "b" with a
 *         top-level state named "a" as its parent). A state's <em>{@linkplain State#isAccessible() accessibility}</em>
 *         refers to whether or not the state can be directly entered/assumed by its associated {@code FSM}. (Note an
 *         {@code FSM} can still be considered in a given inaccessible state if the {@code FSM}'s current state is an
 *         accessible descendent of the given state.) A given state may be set to
 *         <em>{@linkplain State#getIgnoreInvalidTriggers() ignore invalid triggers}</em>, meaning the {@code FSM} will
 *         ignore invalid triggers while the given state is the current state (see below for definition of "invalid
 *         trigger").
 *     </dd>
 *
 *     <dt><strong>Transition</strong></dt>
 *     <dd>
 *         A predefined mapping from one state (the "source" state) to another state (the "destination" state) that
 *         occurs as a result of a trigger. Two special types of transitions exist: (1) a <em>reflexive</em> transition
 *         is a transition in which the source and destination states are identical and (2) an <em>internal</em>
 *         transition is a transition in which the {@code FSM} undergoes no actual state change. (These special variants
 *         are only useful for their side effects, which are explained below.)
 *     </dd>
 *
 *     <dt><strong>Trigger</strong></dt>
 *     <dd>
 *         An external "stimulus" that activates a transition. A given trigger is considered <em>invalid</em> if no
 *         transition whose associated trigger is the given trigger and whose source state is the current state of the
 *         {@code FSM} exists. Invalid triggers either result in runtime exceptions, or can be ignored across the entire
 *         {@code FSM} or on a state-by-state basis.
 *     </dd>
 * </dl>
 * </p>
 *
 * <h2>State and Trigger Types</h2>
 * <p>
 * There are, roughly speaking, four different types of {@code FSM}s that can be created:
 * <ul>
 *     <li>one which uses strings to represent states and strings to represent triggers;</li>
 *     <li>one which uses strings to represent states and enum constants to represent triggers;</li>
 *     <li>one which uses enum constants to represent states and strings to represent triggers; and</li>
 *     <li>one which uses enum constants to represent states and enum constants to represent triggers</li>
 * </ul>
 * Note that when using enum constants to represent states, the enum must implement {@link State State}, while an enum
 * used to represent triggers can be any enum.
 * </p>
 * <p>
 * The main differences between using strings or enum constants to represent states and triggers are (a) convenience,
 * (b) compile-time safety, and (c) performance:
 * <ul>
 *     <li>strings are arguably more convenient;</li>
 *     <li>enum constants are compile-time safe and strings are not (i.e. you could use an arbitrary string as a
 *     state/trigger, even if it is not one, resulting in a runtime exception);</li>
 *     <li>using enum constants is slightly more performant (although transition resolution and state resolution happens
 *     in constant time (with respect to number of transitions and states) in either case).</li>
 * </ul>
 * </p>
 *
 * <h2>Callbacks</h2>
 * <p>
 * User code can be registered to be called when ever a particular state is entered and/or exited, before and/or after a
 * particular transitions occurs, and/or before and/or after each state change occurs. Callbacks can optionally be
 * passed an {@link Event Event} object, which contains context for the transition/state change. Moreover, certain
 * callbacks can be used to implement condition transitions (i.e. transitions which are aborted unless a certain
 * condition holds). See {@linkplain #trigger(Object, Map) here} for the order in which callbacks are called during
 * a transition and see {@linkplain #forceTo(Object, Map) here} for the order in which callbacks are called during a
 * forced state change.
 * </p>
 *
 * <h2>Advantages of using {@code FSM} to a "hand-coded" state machine:</h2>
 * <ul>
 *     <li>the declarative builder syntax effectively concisely documents the state machine's states and
 *     transitions</li>
 *     <li>having to explicitly define and give names to all the states and transitions reduces the chance of error</li>
 *     <li>all the relatively uninteresting logic guaranteeing valid transitions is handled internally</li>
 *     <li>this state machine implementation is already tested</li>
 * </ul>
 *
 * <h2>Basic Example Usage</h2>
 * <pre>{@code FSM<String, String> fsm = FSM.builder("solid", "liquid", "gas")
 *     .onEnter("solid", () -> System.out.println("in solid"))
 *     .onEnter("liquid", () -> System.out.println("in liquid"))
 *     .onEnter("gas", () -> System.out.println("in gas"))
 *     .transition("melt", "solid", "liquid").before(() -> System.out.println("melted")).build()
 *     .transition("evaporate", "liquid", "gas").before(() -> System.out.println("evaporated")).build()
 *     .transition("condense", "gas", "liquid").before(() -> System.out.println("condensed")).build()
 *     .transition("freeze", "liquid", "solid").before(() -> System.out.println("froze")).build()
 *     .build("solid");  // "solid" is the initial state.
 * fsm.trigger("melt");
 * // fsm.trigger("condense");  // Invalid trigger! Not in gas state.
 * fsm.trigger("evaporate");
 * fsm.forceTo("solid");
 * // Output:
 * // melted
 * // in liquid
 * // evaporated
 * // in gas
 * // in solid
 * }</pre>
 *
 * @apiNote The instantiation procedure for {@code FSM}s is as complicated as it is in order to facilitate the
 * "psuedo-immutable" property of {@code FSM}s (i.e. once an {@code FSM} is constructed, no new states or transitions
 * can be added and no existing states or transitions can be removed). The complex build procedure is also desirable
 * because it allows the user to define an entire state machine in one statement, which is nice for documentation
 * purposes.
 *
 * @param <S> the state type.
 * @param <T> the trigger type.
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

        private boolean ignoreInvalidTriggers;
        private EventFunction<S, T> beforeAll;
        private Consumer<Event<S, T>> afterAll;
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
        private StateBundle<S, T> resolveState(S state) {
            StateBundle<S, T> sb = stateMap.s2bundle(state);
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
        public I ignoreInvalidTriggers() {
            requireNotBuilt();
            ignoreInvalidTriggers = true;
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
         * Multiple before all callbacks can be added and they will be invoked in the order they were added. If any
         * before all callback returns false, the remaining before all callbacks will not be executed.
         * </p>
         *
         * @param callback a callback to be run before all transitions/state changes occur.
         * @return this builder.
         * @throws NullPointerException if the given callback is {@code null}.
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
         * Multiple before all callbacks can be added and they will be invoked in the order they were added. If any
         * before all callback returns false, the remaining before all callbacks will not be executed.
         * </p>
         *
         * @param callback a callback to be run before all transitions/state changes occur.
         * @return this builder.
         * @throws NullPointerException if the given callback is {@code null}.
         * @throws IllegalStateException if {@link #build build} has already been called.
         */
        public I beforeAll(Consumer<Event<S, T>> callback) {
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
         * Multiple before all callbacks can be added and they will be invoked in the order they were added. If any
         * before all callback returns false, the remaining before all callbacks will not be executed.
         * </p>
         *
         * @param callback a callback to be run before all transitions/state changes occur.
         * @return this builder.
         * @throws NullPointerException if the given callback is {@code null}.
         * @throws IllegalStateException if {@link #build build} has already been called.
         */
        public I beforeAll(EventFunction<S, T> callback) {
            requireNotBuilt();
            Objects.requireNonNull(callback, "callback must be non-null");
            if (beforeAll == null) {
                beforeAll = callback;
            } else {
                beforeAll = beforeAll.seqCompose(callback);
            }
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
         * Multiple after all callbacks can be added and they will be invoked in the order they were added.
         * </p>
         *
         * @param callback a callback to be run after all transitions/state changes occur.
         * @return this builder.
         * @throws NullPointerException if the given callback is {@code null}.
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
         * Multiple after all callbacks can be added and they will be invoked in the order they were added.
         * </p>
         *
         * @param callback a callback to be run after all transitions/state changes occur.
         * @return this builder.
         * @throws NullPointerException if the given callback is {@code null}.
         * @throws IllegalStateException if {@link #build build} has already been called.
         */
        public I afterAll(Consumer<Event<S, T>> callback) {
            requireNotBuilt();
            Objects.requireNonNull(callback, "callback must be non-null");
            if (afterAll == null) {
                afterAll = callback;
            } else {
                afterAll = afterAll.andThen(callback);
            }
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
        private List<StateBundle<S, T>> resolveSrcs(List<S> srcs) {
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
        private StateBundle<S, T> requireAccessible(StateBundle<S, T> sb) {
            if (!sb.state.isAccessible()) {
                throw new IllegalArgumentException("state must be accessible");
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
         * @throws IllegalArgumentException if the given initial state is not {@link State#isAccessible() accessible}.
         * @throws IllegalStateException if {@code build} has already been called.
         */
        public FSM<S, T> build(S initial) {
            requireNotBuilt();
            built = true;
            return new FSM<>(
                    this,
                    requireAccessible(resolveState(
                            Objects.requireNonNull(initial, "initial state must be non-null")
                    )
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

        BuilderFromStrings(String[] states, Supplier<Map<T, Transition<String, T>>> mapSupplier) {
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
        public BuilderFromStrings<T> ignoreInvalidTriggers(String... states) {
            requireNotBuilt();
            for (String state : Objects.requireNonNull(states, "states must be non-null")) {
                str2state(Objects.requireNonNull(state, "cannot ignore invalid triggers on null state"))
                        .ignoreInvalidTriggers = true;
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
         * Multiple on enter callbacks can be added and they will be invoked in the order they were added.
         * </p>
         *
         * @param state the state to register a callback with.
         * @param callback a callback to be run after the given state is entered.
         * @return this builder.
         * @throws NullPointerException if the given state or callback is {@code null}.
         * @throws IllegalArgumentException if the given state does not belong to the FSM being built.
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
         * Multiple on enter callbacks can be added and they will be invoked in the order they were added.
         * </p>
         *
         * @param state the state to register a callback with.
         * @param callback a callback to be run after the given state is entered.
         * @return this builder.
         * @throws NullPointerException if the given state or callback is {@code null}.
         * @throws IllegalArgumentException if the given state does not belong to the FSM being built.
         * @throws IllegalStateException if {@link #build build} has already been called.
         */
        public BuilderFromStrings<T> onEnter(String state, Consumer<Event<String, T>> callback) {
            requireNotBuilt();
            Objects.requireNonNull(callback, "callback must be non-null");
            StringState<T> s = str2state(Objects.requireNonNull(state, "cannot attach callback to null state"));
            if (s.onEnter == null) {
                s.onEnter = callback;
            } else {
                s.onEnter = s.onEnter.andThen(callback);
            }
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
         * Multiple on exit callbacks can be added and they will be invoked in the order they were added.
         * </p>
         *
         * @param state the state to register a callback with.
         * @param callback a callback to be run before the given state is exited.
         * @return this builder.
         * @throws NullPointerException if the given state or callback is {@code null}.
         * @throws IllegalArgumentException if the given state does not belong to the FSM being built.
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
         * Multiple on exit callbacks can be added and they will be invoked in the order they were added.
         * </p>
         *
         * @param state the state to register a callback with.
         * @param callback a callback to be run before the given state is exited.
         * @return this builder.
         * @throws NullPointerException if the given state or callback is {@code null}.
         * @throws IllegalArgumentException if the given state does not belong to the FSM being built.
         * @throws IllegalStateException if {@link #build build} has already been called.
         */
        public BuilderFromStrings<T> onExit(String state, Consumer<Event<String, T>> callback) {
            requireNotBuilt();
            Objects.requireNonNull(callback, "callback must be non-null");
            StringState<T> s = str2state(Objects.requireNonNull(state, "cannot attach callback to null state"));
            if (s.onExit == null) {
                s.onExit = callback;
            } else {
                s.onExit = s.onExit.andThen(callback);
            }
            return this;
        }

        private StringState<T> str2state(String state) {
            return (StringState<T>) stateMap.s2state(state);
        }

    }

    /**
     * A builder for {@code FSM}s that uses the constants of an enum implementor of {@link State State} to represent
     * states.
     *
     * @author Robert Russell
     */
    public static final class BuilderFromEnum<S extends Enum<S> & State<S, T>, T>
            extends Builder<S, T, BuilderFromEnum<S, T>> {

        BuilderFromEnum(Class<S> stateEnum, Supplier<Map<T, Transition<S, T>>> mapSupplier) {
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
     * @throws IllegalArgumentException if the given string array is empty.
     * @throws IllegalArgumentException if the given string array contains duplicate elements.
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
     * @throws IllegalArgumentException if the given string array is empty.
     * @throws IllegalArgumentException if the given string array contains duplicate elements.
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
     * @throws IllegalArgumentException if the given state enum has zero constants.
     */
    public static <S extends Enum<S> & State<S, String>> BuilderFromEnum<S, String> builder(Class<S> stateEnum) {
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
     * @throws IllegalArgumentException if the given state enum has zero constants.
     */
    public static <S extends Enum<S> & State<S, T>, T extends Enum<T>> BuilderFromEnum<S, T> builder(
            Class<T> triggerEnum, Class<S> stateEnum
    ) {
        Objects.requireNonNull(triggerEnum, "triggerEnum must be non-null");
        return new BuilderFromEnum<>(
                Objects.requireNonNull(stateEnum, "stateEnum must be non-null"),
                () -> new EnumMap<>(triggerEnum)
        );
    }

    private final boolean ignoreInvalidTriggers;
    private final EventFunction<S, T> beforeAll;
    private final Consumer<Event<S, T>> afterAll;
    private final StateMap<S, T> stateMap;
    private StateBundle<S, T> currSB;

    /**
     * {@code stateChanging} is true if a state change/transition is in process; false otherwise.
     */
    private boolean stateChanging = false;

    /**
     * {@code queue} is queue of forceTo/trigger calls made from callbacks.
     */
    private final Deque<Runnable> queue = new ArrayDeque<>();

    private FSM(Builder<S, T, ?> builder, StateBundle<S, T> initial) {
        ignoreInvalidTriggers = builder.ignoreInvalidTriggers;
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
    public State<S, T> getStateObj() {
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
     * Callbacks occur in this order:
     * <ol>
     *     <li>the {@code FSM}'s before all callbacks in the order they were added (unable to abort state change);</li>
     *     <li>state on exit callbacks:
     *         <ol type="i">
     *             <li>the current states's on exit callbacks in the order they were added;</li>
     *             <li>the current states's nearest ancestor's on exit callbacks in the order they were added;</li>
     *             <li>...</li>
     *             <li>the current states's furthest ancestor's on exit callbacks in the order they were added;</li>
     *         </ol>
     *     </li>
     *     <li><strong>STATE CHANGE</strong></li>
     *     <li>state on enter callbacks:
     *         <ol type="i">
     *             <li>the new (destination) states's furthest ancestor's on enter callbacks in the order they were
     *             added;</li>
     *             <li>...</li>
     *             <li>the new (destination) states's nearest ancestor's on enter callbacks in the order they were
     *             added;</li>
     *             <li>the new (destination) states's on enter callbacks in the order they were added;</li>
     *         </ol>
     *     </li>
     *     <li>the {@code FSM}'s after all callbacks in the order they were added.</li>
     * </ol>
     * </p>
     * <p>
     * If any callback activates a trigger (i.e. calls {@code trigger}) or forces the {@code FSM} into a new state
     * (i.e. calls {@code forceTo}), that action is <em>queued</em>. That is, the action will not start until the
     * current transition/state change completes. For example, if one calls {@code fsm.forceTo(S)} (where "S" is some
     * state) inside a callback that was invoked as result of trigger "T", the order of events will roughly be as
     * follows:
     * <ol>
     *     <li>{@code fsm.trigger(T)} invoked</li>
     *     <li>start of trigger "T"</li>
     *     <li>callbacks from trigger "T", one of which calls {@code fsm.forceTo(S)} (which, in turn, queues that action
     *     and returns immediately)</li>
     *     <li>end of trigger "T"</li>
     *     <li>start of forced state change to "S"</li>
     *     <li>callbacks from forced state change to "S"</li>
     *     <li>end of forced state change to "S"</li>
     *     <li>{@code fsm.trigger(T)} returns</li>
     * </ol>
     * </p>
     * <p>
     * Any exceptions thrown inside a queued transition/state change will be propagated to the invocation of
     * {@code trigger}/{@code forceTo} that is not inside any callback.
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
     * Callbacks occur in this order:
     * <ol>
     *     <li>the {@code FSM}'s before all callbacks in the order they were added (unable to abort state change);</li>
     *     <li>state on exit callbacks:
     *         <ol type="i">
     *             <li>the current states's on exit callbacks in the order they were added;</li>
     *             <li>the current states's nearest ancestor's on exit callbacks in the order they were added;</li>
     *             <li>...</li>
     *             <li>the current states's furthest ancestor's on exit callbacks in the order they were added;</li>
     *         </ol>
     *     </li>
     *     <li><strong>STATE CHANGE</strong></li>
     *     <li>state on enter callbacks:
     *         <ol type="i">
     *             <li>the new (destination) states's furthest ancestor's on enter callbacks in the order they were
     *             added;</li>
     *             <li>...</li>
     *             <li>the new (destination) states's nearest ancestor's on enter callbacks in the order they were
     *             added;</li>
     *             <li>the new (destination) states's on enter callbacks in the order they were added;</li>
     *         </ol>
     *     </li>
     *     <li>the {@code FSM}'s after all callbacks in the order they were added.</li>
     * </ol>
     * </p>
     * <p>
     * If any callback activates a trigger (i.e. calls {@code trigger}) or forces the {@code FSM} into a new state
     * (i.e. calls {@code forceTo}), that action is <em>queued</em>. That is, the action will not start until the
     * current transition/state change completes. For example, if one calls {@code fsm.forceTo(S)} (where "S" is some
     * state) inside a callback that was invoked as result of trigger "T", the order of events will roughly be as
     * follows:
     * <ol>
     *     <li>{@code fsm.trigger(T)} invoked</li>
     *     <li>start of trigger "T"</li>
     *     <li>callbacks from trigger "T", one of which calls {@code fsm.forceTo(S)} (which, in turn, queues that action
     *     and returns immediately)</li>
     *     <li>end of trigger "T"</li>
     *     <li>start of forced state change to "S"</li>
     *     <li>callbacks from forced state change to "S"</li>
     *     <li>end of forced state change to "S"</li>
     *     <li>{@code fsm.trigger(T)} returns</li>
     * </ol>
     * </p>
     * <p>
     * Any exceptions thrown inside a queued transition/state change will be propagated to the invocation of
     * {@code trigger}/{@code forceTo} that is not inside any callback.
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
        StateBundle<S, T> sb = stateMap.s2bundle(
                Objects.requireNonNull(state, "cannot force transition to null state")
        );
        if (sb == null) {
            throw new IllegalArgumentException("state does not belong to this FSM");
        }
        if (!sb.state.isAccessible()) {
            throw new IllegalArgumentException("cannot enter inaccessible state");
        }

        // If a transition/state change is currently in progress, queue the forceTo request.
        // Otherwise, do it immediately.
        if (stateChanging) {
            queue.addLast(() -> forceToUnqueued(sb, args));
        } else {
            forceToUnqueued(sb, args);
        }
    }

    private void forceToUnqueued(StateBundle<S, T> sb, Map<String, Object> args) {
        // "Lock" the FSM.
        assert !stateChanging;
        stateChanging = true;

        if (sb == currSB) {
            // no-op if the FSM is already in the given state.
            return;
        }

        // Prepare event object.
        if (args == null) {
            args = new HashMap<>();
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

        // "Unlock" the FSM.
        stateChanging = false;

        emptyQueue();
    }

    /**
     * <p>
     * Activate the given trigger.
     * </p>
     * <p>
     * If the given trigger is invalid in the current state (i.e. the given trigger is not associated with any
     * transition which has the current state as a source state), then an exception is thrown. If this {@code FSM} is
     * configured to ignore invalid triggers or if the current state is configured to ignore invalid triggers, then this
     * method returns false instead of throwing an exception to indicate failure.
     * </p>
     * <p>
     * If more than one valid transition from the current state with the given trigger exists, the one that was
     * registered first during the build procedure is chosen.
     * </p>
     * <p>
     * If a transition from the current state with the given trigger exists, the transition can be aborted if any of the
     * before callbacks on the transition or any of the before all callbacks on this {@code FSM} return false; if a
     * transition is aborted in this way, this method returns false.
     * </p>
     * <p>
     * Assuming a transition successfully executes to completion, callbacks occur in this order:
     * <ol>
     *     <li>the {@code FSM}'s before all callbacks in the order they were added;</li>
     *     <li>the transitions's before callbacks in the order they were added;</li>
     *     <li>state on exit callbacks:
     *         <ol type="i">
     *             <li>the current states's on exit callbacks in the order they were added;</li>
     *             <li>the current states's nearest ancestor's on exit callbacks in the order they were added;</li>
     *             <li>...</li>
     *             <li>the current states's furthest ancestor's on exit callbacks in the order they were added;</li>
     *         </ol>
     *     </li>
     *     <li><strong>STATE CHANGE</strong></li>
     *     <li>state on enter callbacks:
     *         <ol type="i">
     *             <li>the new (destination) states's furthest ancestor's on enter callbacks in the order they were
     *             added;</li>
     *             <li>...</li>
     *             <li>the new (destination) states's nearest ancestor's on enter callbacks in the order they were
     *             added;</li>
     *             <li>the new (destination) states's on enter callbacks in the order they were added;</li>
     *         </ol>
     *     </li>
     *     <li>the transitions's after callbacks in the order they were added;</li>
     *     <li>the {@code FSM}'s after all callbacks in the order they were added.</li>
     * </ol>
     * </p>
     * <p>
     * If any callback activates a trigger (i.e. calls {@code trigger}) or forces the {@code FSM} into a new state
     * (i.e. calls {@code forceTo}), that action is <em>queued</em>. That is, the action will not start until the
     * current transition/state change completes. For example, if one calls {@code fsm.forceTo(S)} (where "S" is some
     * state) inside a callback that was invoked as result of trigger "T", the order of events will roughly be as
     * follows:
     * <ol>
     *     <li>{@code fsm.trigger(T)} invoked</li>
     *     <li>start of trigger "T"</li>
     *     <li>callbacks from trigger "T", one of which calls {@code fsm.forceTo(S)} (which, in turn, queues that action
     *     and returns immediately)</li>
     *     <li>end of trigger "T"</li>
     *     <li>start of forced state change to "S"</li>
     *     <li>callbacks from forced state change to "S"</li>
     *     <li>end of forced state change to "S"</li>
     *     <li>{@code fsm.trigger(T)} returns</li>
     * </ol>
     * </p>
     * <p>
     * A corollary to the previous point is that the result of invocations of {@code trigger} inside callbacks cannot be
     * determined when {@code trigger} is invoked; as such, <strong>{@code trigger} always returns true when called from
     * inside a callback</strong>. Moreover, any exceptions thrown inside a queued transition/state change will be
     * propagated to the invocation of {@code trigger}/{@code forceTo} that is not inside any callback.
     * </p>
     *
     * @param trigger the trigger.
     * @return whether or not a transition executed to completion, or true if the trigger was queued.
     * @throws NullPointerException if the given trigger is {@code null}.
     * @throws IllegalStateException if the given trigger is invalid and invalid triggers are not ignored in the current
     * state.
     */
    public boolean trigger(T trigger) {
        return trigger(trigger, Map.of());
    }

    /**
     * <p>
     * Activate the given trigger.
     * </p>
     * <p>
     * If the given trigger is invalid in the current state (i.e. the given trigger is not associated with any
     * transition which has the current state as a source state), then an exception is thrown. If this {@code FSM} is
     * configured to ignore invalid triggers or if the current state is configured to ignore invalid triggers, then this
     * method returns false instead of throwing an exception to indicate failure.
     * </p>
     * <p>
     * If more than one valid transition from the current state with the given trigger exists, the one that was
     * registered first during the build procedure is chosen.
     * </p>
     * <p>
     * If a transition from the current state with the given trigger exists, the transition can be aborted if any of the
     * before callbacks on the transition or any of the before all callbacks on this {@code FSM} return false; if a
     * transition is aborted in this way, this method returns false.
     * </p>
     * <p>
     * Assuming a transition successfully executes to completion, callbacks occur in this order:
     * <ol>
     *     <li>the {@code FSM}'s before all callbacks in the order they were added;</li>
     *     <li>the transitions's before callbacks in the order they were added;</li>
     *     <li>state on exit callbacks:
     *         <ol type="i">
     *             <li>the current states's on exit callbacks in the order they were added;</li>
     *             <li>the current states's nearest ancestor's on exit callbacks in the order they were added;</li>
     *             <li>...</li>
     *             <li>the current states's furthest ancestor's on exit callbacks in the order they were added;</li>
     *         </ol>
     *     </li>
     *     <li><strong>STATE CHANGE</strong></li>
     *     <li>state on enter callbacks:
     *         <ol type="i">
     *             <li>the new (destination) states's furthest ancestor's on enter callbacks in the order they were
     *             added;</li>
     *             <li>...</li>
     *             <li>the new (destination) states's nearest ancestor's on enter callbacks in the order they were
     *             added;</li>
     *             <li>the new (destination) states's on enter callbacks in the order they were added;</li>
     *         </ol>
     *     </li>
     *     <li>the transitions's after callbacks in the order they were added;</li>
     *     <li>the {@code FSM}'s after all callbacks in the order they were added.</li>
     * </ol>
     * </p>
     * <p>
     * If any callback activates a trigger (i.e. calls {@code trigger}) or forces the {@code FSM} into a new state
     * (i.e. calls {@code forceTo}), that action is <em>queued</em>. That is, the action will not start until the
     * current transition/state change completes. For example, if one calls {@code fsm.forceTo(S)} (where "S" is some
     * state) inside a callback that was invoked as result of trigger "T", the order of events will roughly be as
     * follows:
     * <ol>
     *     <li>{@code fsm.trigger(T)} invoked</li>
     *     <li>start of trigger "T"</li>
     *     <li>callbacks from trigger "T", one of which calls {@code fsm.forceTo(S)} (which, in turn, queues that action
     *     and returns immediately)</li>
     *     <li>end of trigger "T"</li>
     *     <li>start of forced state change to "S"</li>
     *     <li>callbacks from forced state change to "S"</li>
     *     <li>end of forced state change to "S"</li>
     *     <li>{@code fsm.trigger(T)} returns</li>
     * </ol>
     * </p>
     * <p>
     * A corollary to the previous point is that the result of invocations of {@code trigger} inside callbacks cannot be
     * determined when {@code trigger} is invoked; as such, <strong>{@code trigger} always returns true when called from
     * inside a callback</strong>. Moreover, any exceptions thrown inside a queued transition/state change will be
     * propagated to the invocation of {@code trigger}/{@code forceTo} that is not inside any callback.
     * </p>
     *
     * @param trigger the trigger.
     * @param args arguments to put in the {@link Event Event} object so that they might be accessed from callbacks.
     * @return whether or not a transition executed to completion, or true if the trigger was queued.
     * @throws NullPointerException if the given trigger is {@code null}.
     * @throws IllegalStateException if the given trigger is invalid and invalid triggers are not ignored in the current
     * state.
     */
    public boolean trigger(T trigger, Map<String, Object> args) {
        Objects.requireNonNull(trigger, "trigger must be non-null");
        if (stateChanging) {
            queue.addLast(() -> triggerUnqueued(trigger, args));
            return true;
        }
        return triggerUnqueued(trigger, args);
    }

    private boolean triggerUnqueued(T trigger, Map<String, Object> args) {
        // "Lock" the FSM.
        assert !stateChanging;
        stateChanging = true;

        // Resolve the transition.
        Transition<S, T> transition = currSB.resolveTransition(trigger);
        if (transition == null) {
            // I.e. if the trigger is invalid...
            if (!ignoreInvalidTriggers && !currSB.state.getIgnoreInvalidTriggers()) {
                throw new IllegalStateException(
                        String.format("cannot use trigger '%s' in state '%s'", trigger, State.fullName(currSB.state))
                );
            }
            return false;
        }

        // Resolve the destination state.
        StateBundle<S, T> dst = transition.resolveDst(currSB);

        // Prepare event object.
        if (args == null) {
            args = new HashMap<>();
        }
        Event<S, T> event = new Event<>(this, currSB.state, dst.state, trigger, args);

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

        // "Unlock" the FSM.
        stateChanging = false;

        emptyQueue();

        return true;
    }

    private void emptyQueue() {
        // This works recursively since the Runnable will ultimately call emptyQueue again.
        Runnable r = queue.pollFirst();
        if (r != null) {
            r.run();
        }
    }

    private boolean beforeAll(Event<S, T> event) {
        return beforeAll == null || beforeAll.apply(event);
    }

    private void afterAll(Event<S, T> event) {
        if (afterAll != null) {
            afterAll.accept(event);
        }
    }

    private static <S, T> void chainOnExitCallbacks(State<S, T> state, Event<S, T> event) {
        if (state != null) {
            // Call the ancestors' callbacks second, as per specification.
            state.onExit(event);
            chainOnExitCallbacks(state.getParent(), event);
        }
    }

    private static <S, T> void chainOnEnterCallbacks(State<S, T> state, Event<S, T> event) {
        if (state != null) {
            // Call the ancestors' callbacks first, as per specification.
            chainOnEnterCallbacks(state.getParent(), event);
            state.onEnter(event);
        }
    }
}
