package frc.team7170.lib.fsm;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * {@code Transition} is used internally to represent a state transition on a {@link FSM FSM}.
 *
 * @param <S> the state type.
 * @param <T> the trigger type.
 *
 * @author Robert Russell
 */
final class Transition<S, T> {

    /**
     * A builder for transitions in a {@link FSM FSM}.
     *
     * @apiNote {@code Builder} is genericized in terms of the {@link FSM.Builder FSM.Builder} type so that
     * {@link #build() build} knows what type to return.
     *
     * @param <S> the state type
     * @param <T> the trigger type.
     * @param <I> the type of the {@link FSM.Builder FSM.Builder}.
     *
     * @author Robert Russell
     */
    public static final class Builder<S, T, I extends FSM.Builder<S, T, I>> {

        private final T trigger;
        private final List<StateBundle<S, T>> srcs;
        private final StateBundle<S, T> dst;
        private final I parent;
        private final boolean internal;
        private EventFunction<S, T> before;
        private Consumer<Event<S, T>> after;

        private Builder(T trigger, List<StateBundle<S, T>> srcs, StateBundle<S, T> dst, I parent, boolean internal) {
            // All parameters should have already been validated.
            this.trigger = trigger;
            this.srcs = srcs;
            this.dst = dst;
            this.parent = parent;
            this.internal = internal;
        }

        /**
         * <p>
         * Register a callback to be run before this transition executes. The associated transition context (i.e.
         * {@link Event Event} object) is ignored.
         * </p>
         * <p>
         * The callback should return quickly, lest the rest of the transition procedure will be delayed.
         * </p>
         * <p>
         * Multiple before callbacks can be added and they will be invoked in the order they were added. If any before
         * callback returns false, the remaining before callbacks will not be executed.
         * </p>
         *
         * @param callback a callback to be run before this transition executes.
         * @return this builder.
         * @throws NullPointerException if the given callback is {@code null}.
         */
        public Builder<S, T, I> before(Runnable callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            return before(event -> {
                callback.run();
                return true;
            });
        }

        /**
         * <p>
         * Register a callback accepting an {@link Event Event} object to be run before this transition executes.
         * </p>
         * <p>
         * The callback should return quickly, lest the rest of the transition procedure will be delayed.
         * </p>
         * <p>
         * Multiple before callbacks can be added and they will be invoked in the order they were added. If any before
         * callback returns false, the remaining before callbacks will not be executed.
         * </p>
         *
         * @param callback a callback to be run before this transition executes.
         * @return this builder.
         * @throws NullPointerException if the given callback is {@code null}.
         */
        public Builder<S, T, I> before(Consumer<Event<S, T>> callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            return before(event -> {
                callback.accept(event);
                return true;
            });
        }

        /**
         * <p>
         * Register a callback accepting an {@link Event Event} object to be run before this transition executes. The
         * callback returns a boolean indicating whether the transition should proceed (true if it should proceed, false
         * if not), effectively allowing one to create conditional transitions.
         * </p>
         * <p>
         * The callback should return quickly, lest the rest of the transition procedure will be delayed.
         * </p>
         * <p>
         * Multiple before callbacks can be added and they will be invoked in the order they were added. If any before
         * callback returns false, the remaining before callbacks will not be executed.
         * </p>
         *
         * @param callback a callback to be run before this transition executes.
         * @return this builder.
         * @throws NullPointerException if the given callback is {@code null}.
         */
        public Builder<S, T, I> before(EventFunction<S, T> callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            if (before == null) {
                before = callback;
            } else {
                before = before.seqCompose(callback);
            }
            return this;
        }

        /**
         * <p>
         * Register a callback to be run after this transition executes. The associated transition context (i.e.
         * {@link Event Event} object) is ignored.
         * </p>
         * <p>
         * The callback should return quickly, lest the rest of the transition procedure will be delayed.
         * </p>
         * <p>
         * Multiple after callbacks can be added and they will be invoked in the order they were added.
         * </p>
         *
         * @param callback a callback to be run after this transition executes.
         * @return this builder.
         * @throws NullPointerException if the given callback is {@code null}.
         */
        public Builder<S, T, I> after(Runnable callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            return after(event -> callback.run());
        }

        /**
         * <p>
         * Register a callback accepting an {@link Event Event} object to be run after this transition executes.
         * </p>
         * <p>
         * The callback should return quickly, lest the rest of the transition procedure will be delayed.
         * </p>
         * <p>
         * Multiple after callbacks can be added and they will be invoked in the order they were added.
         * </p>
         *
         * @param callback a callback to be run after this transition executes.
         * @return this builder.
         * @throws NullPointerException if the given callback is {@code null}.
         */
        public Builder<S, T, I> after(Consumer<Event<S, T>> callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            if (after == null) {
                after = callback;
            } else {
                after = after.andThen(callback);
            }
            return this;
        }

        /**
         * Build the {@code Transition} object and register it with all the source states.
         *
         * @return the parent {@linkplain FSM.Builder FSM builder}.
         */
        public I build() {
            Transition<S, T> transition = new Transition<>(dst, before, after, internal);
            for (StateBundle<S, T> src : srcs) {
                src.addTransition(trigger, transition);
            }
            return parent;
        }

        /**
         * @return a new {@code Transition.Builder} for a normal (i.e. not internal or reflexive) transition.
         */
        static <S, T, I extends FSM.Builder<S, T, I>> Transition.Builder<S, T, I> normal(
                T trigger, List<StateBundle<S, T>> srcs, StateBundle<S, T> dst, I parent
        ) {
            return new Builder<>(trigger, srcs, dst, parent, false);
        }

        /**
         * @return a new {@code Transition.Builder} for an internal transition.
         */
        static <S, T, I extends FSM.Builder<S, T, I>> Transition.Builder<S, T, I> internal(
                T trigger, List<StateBundle<S, T>> srcs, I parent
        ) {
            return new Builder<>(trigger, srcs, null, parent, true);
        }

        /**
         * @return a new {@code Transition.Builder} for a reflexive transition.
         */
        static <S, T, I extends FSM.Builder<S, T, I>> Transition.Builder<S, T, I> reflexive(
                T trigger, List<StateBundle<S, T>> srcs, I parent
        ) {
            return new Builder<>(trigger, srcs, null, parent, false);
        }
    }

    private final StateBundle<S, T> dst;
    private final EventFunction<S, T> before;
    private final Consumer<Event<S, T>> after;
    final boolean internal;

    private Transition(
            StateBundle<S, T> dst,
            EventFunction<S, T> before,
            Consumer<Event<S, T>> after,
            boolean internal
    ) {
        this.dst = dst;
        this.before = before;
        this.after = after;
        this.internal = internal;
    }

    /**
     * Resolve the destination state for this transition given the source state and assuming the given source state is a
     * valid source state for this transition.
     *
     * @param src the source state (bundle).
     * @return the resolved destination state.
     */
    StateBundle<S, T> resolveDst(StateBundle<S, T> src) {
        return dst != null ? dst : src;
    }

    boolean before(Event<S, T> event) {
        return before == null || before.apply(event);
    }

    void after(Event<S, T> event) {
        if (after != null) {
            after.accept(event);
        }
    }
}
