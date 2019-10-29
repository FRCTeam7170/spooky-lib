package frc.team7170.lib.fsm2;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

// TODO: comment in FSM class where Transition does null checks for us.
// TODO: type parameter documentation all messed up

final class Transition<T> {

    /**
     * A builder for transitions in a {@link FSM FSM}.
     *
     * @author Robert Russell
     */
    public static final class Builder<S, T, I extends FSM.Builder<S, T, I>> {

        private final T trigger;
        private final List<StateBundle<T>> srcs;
        private final StateBundle<T> dst;
        private final I parent;
        private final boolean internal;
        private Function<Event, Boolean> before;
        private Consumer<Event> after;

        private Builder(T trigger, List<StateBundle<T>> srcs, StateBundle<T> dst, I parent, boolean internal) {
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
         * Only one before callback can be registered; multiple calls to any version of the {@code before} method will
         * result in an {@link IllegalStateException IllegalStateException} to prevent accidentally trying to register
         * multiple before callbacks.
         * </p>
         *
         * @param callback a callback to be run before this transition executes.
         * @throws NullPointerException if the given callback is {@code null}.
         * @throws IllegalStateException if {@code before} has been called previously.
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
         * Only one before callback can be registered; multiple calls to any version of the {@code before} method will
         * result in an {@link IllegalStateException IllegalStateException} to prevent accidentally trying to register
         * multiple before callbacks.
         * </p>
         *
         * @param callback A callback to be run before this transition executes.
         * @throws NullPointerException if the given callback is {@code null}.
         * @throws IllegalStateException if {@code before} has been called previously.
         */
        public Builder<S, T, I> before(Consumer<Event> callback) {
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
         * Only one before callback can be registered; multiple calls to any version of the {@code before} method will
         * result in an {@link IllegalStateException IllegalStateException} to prevent accidentally trying to register
         * multiple before callbacks.
         * </p>
         *
         * @param callback A callback to be run before this transition executes.
         * @throws NullPointerException if the given callback is {@code null}.
         * @throws IllegalStateException if {@code before} has been called previously.
         */
        public Builder<S, T, I> before(Function<Event, Boolean> callback) {
            if (before != null) {
                throw new IllegalStateException("cannot register more than one before callback");
            }
            before = Objects.requireNonNull(callback, "callback must be non-null");
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
         * Only one after callback can be registered; multiple calls to any version of the {@code after} method will
         * result in an {@link IllegalStateException IllegalStateException} to prevent accidentally trying to register
         * multiple after callbacks.
         * </p>
         *
         * @param callback a callback to be run after this transition executes.
         * @throws NullPointerException if the given callback is {@code null}.
         * @throws IllegalStateException if {@code after} has been called previously.
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
         * Only one after callback can be registered; multiple calls to any version of the {@code after} method will
         * result in an {@link IllegalStateException IllegalStateException} to prevent accidentally trying to register
         * multiple after callbacks.
         * </p>
         *
         * @param callback a callback to be run after this transition executes.
         * @throws NullPointerException if the given callback is {@code null}.
         * @throws IllegalStateException if {@code after} has been called previously.
         */
        public Builder<S, T, I> after(Consumer<Event> callback) {
            if (after != null) {
                throw new IllegalStateException("cannot register more than one after callback");
            }
            after = Objects.requireNonNull(callback, "callback must be non-null");
            return this;
        }

        /**
         * Build the {@code Transition} object and add it to the parent
         * {@linkplain FSM.Builder FSM builder}.
         *
         * @return the parent FSM builder.
         */
        public I build() {
            Transition<T> transition = new Transition<>(dst, before, after, internal);
            for (StateBundle<T> src : srcs) {
                src.addTransition(trigger, transition);
            }
            return parent;
        }

        /**
         * @return a new {@code TransitionBuilder} for a normal (i.e. not internal or reflexive) transition.
         */
        static <S, T, I extends FSM.Builder<S, T, I>> Transition.Builder<S, T, I> normal(
                T trigger, List<StateBundle<T>> srcs, StateBundle<T> dst, I parent
        ) {
            return new Builder<>(trigger, srcs, dst, parent, false);
        }

        /**
         * @return a new {@code TransitionBuilder} for an internal transition.
         */
        static <S, T, I extends FSM.Builder<S, T, I>> Transition.Builder<S, T, I> internal(
                T trigger, List<StateBundle<T>> srcs, I parent
        ) {
            return new Builder<>(trigger, srcs, null, parent, true);
        }

        /**
         * @return a new {@code TransitionBuilder} for a reflexive transition.
         */
        static <S, T, I extends FSM.Builder<S, T, I>> Transition.Builder<S, T, I> reflexive(
                T trigger, List<StateBundle<T>> srcs, I parent
        ) {
            return new Builder<>(trigger, srcs, null, parent, false);
        }
    }

    private final StateBundle<T> dst;
    private final Function<Event, Boolean> before;
    private final Consumer<Event> after;
    final boolean internal;

    private Transition(StateBundle<T> dst, Function<Event, Boolean> before, Consumer<Event> after, boolean internal) {
        this.dst = dst;
        this.before = before;
        this.after = after;
        this.internal = internal;
    }

    StateBundle<T> resolveDst(StateBundle<T> src) {
        return dst != null ? dst : src;
    }

    boolean before(Event event) {
        return before == null || before.apply(event);
    }

    void after(Event event) {
        if (after != null) {
            after.accept(event);
        }
    }
}
