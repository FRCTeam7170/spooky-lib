package frc.team7170.lib.fsm2;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

// TODO: make capital after param tag consistent
// TODO: comment in FSM class where Transition does null checks for us.
/**
 * <p>
 * A transition for a {@link FSM FSM}.
 * </p>
 * <p>
 * Typically, the user should not have to interact with this class; it is mainly used internally. However,
 * {@code Transition} instances are accessible via {@link Event Event} objects in a user callback should the user wish
 * to query certain properties of an executing transition.
 * </p>
 *
 * @author Robert Russell
 */
public final class Transition {

    /**
     * A builder for {@code Transition}s.
     *
     * @apiNote This class is generic so that {@link Builder#build() build} can return the appropriate "parent builder".
     *
     * @param <T> the type of {@linkplain FSM.Builder FSM builder} (either
     * {@link FSM.BuilderFromStrings BuilderFromStrings} or
     * {@link FSM.BuilderFromEnum BuilderFromEnum}).
     *
     * @author Robert Russell
     */
    public static final class Builder<T extends FSM.Builder> {

        private final String trigger;
        private final List<? extends State> srcs;
        private final State dst;
        private final T parent;
        private final Type type;
        private Function<Event, Boolean> before;
        private Consumer<Event> after;

        private Builder(String trigger, List<? extends State> srcs, State dst, T parent, Type type) {
            // These null checks are redundant in some cases.
            this.trigger = Objects.requireNonNull(trigger, "trigger string must be non-null");
            this.srcs = Objects.requireNonNull(srcs, "srcs must be non-null");
            this.dst = dst;
            this.parent = parent;
            this.type = type;

            if (srcs.size() == 0) {
                throw new IllegalArgumentException("transitions must have at least one src state");
            }
            for (State s : srcs) {
                // Make sure there are no null srcs.
                if (s == null) {
                    throw new NullPointerException("transitions cannot have any null src states");
                }
                // Make sure all srcs are accessible. (Technically there is no harm in having an inaccessible src but a
                // transition from an inaccessible src is impossible so it is a user error.)
                if (!s.isAccessible()) {
                    throw new IllegalArgumentException("transition src states must be accessible");
                }
            }
            if (dst != null && !dst.isAccessible()) {
                throw new IllegalArgumentException("transition dst states must be accessible");
            }
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
        public void before(Runnable callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            before(event -> {
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
        public void before(Consumer<Event> callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            before(event -> {
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
        public void before(Function<Event, Boolean> callback) {
            if (before != null) {
                throw new IllegalStateException("cannot register more than one before callback");
            }
            before = Objects.requireNonNull(callback, "callback must be non-null");
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
        public void after(Runnable callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            after(event -> callback.run());
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
        public void after(Consumer<Event> callback) {
            if (after != null) {
                throw new IllegalStateException("cannot register more than one after callback");
            }
            after = Objects.requireNonNull(callback, "callback must be non-null");
        }

        /**
         * Build the {@code Transition} object and add it to the parent
         * {@linkplain FSM.Builder FSM builder}.
         *
         * @return the parent FSM builder.
         */
        public T build() {
            parent.addTransition(new Transition(this));
            return parent;
        }

        /**
         * @return a new {@code Builder} for a normal (i.e. not internal or reflexive) transition.
         */
        static <T extends FSM.Builder> Builder<T> normal(
                String trigger, List<? extends State> srcs, State dst, T parent
        ) {
            return new Builder<>(trigger, srcs, dst, parent, Type.NORMAL);
        }

        /**
         * @return a new {@code Builder} for an internal transition.
         */
        static <T extends FSM.Builder> Builder<T> internal(
                String trigger, List<? extends State> srcs, T parent
        ) {
            return new Builder<>(trigger, srcs, null, parent, Type.INTERNAL);
        }

        /**
         * @return a new {@code Builder} for a reflexive transition.
         */
        static <T extends FSM.Builder> Builder<T> reflexive(
                String trigger, List<? extends State> srcs, T parent
        ) {
            return new Builder<>(trigger, srcs, null, parent, Type.REFLEXIVE);
        }
    }

    /**
     * Type is an enum used internally to represent the three different transition types: normal, internal, and
     * reflexive. While {@code Type} is not public, the user can check a transition's type via
     * {@link #isInternal() isInternal} and {@link #isReflexive() isReflexive}.
     */
    private enum Type {
        NORMAL, INTERNAL, REFLEXIVE
    }

    private final String trigger;
    private final List<? extends State> srcs;
    private final State dst;
    private final Type type;
    private final Function<Event, Boolean> before;
    private final Consumer<Event> after;

    private Transition(Builder<?> builder) {
        trigger = builder.trigger;
        srcs = builder.srcs;
        dst = builder.dst;
        type = builder.type;
        before = builder.before;
        after = builder.after;
    }

    /**
     * @return the trigger string for this transition.
     */
    public String getTrigger() {
        return trigger;
    }

    /**
     * @return a {@linkplain List list} of the source {@linkplain State states} for this transition (i.e. those states
     * this transition can transition from). The returned list always contains at least one element.
     */
    public List<State> getSrcs() {
        // Make a defensive copy.
        return List.copyOf(srcs);
    }

    /**
     * @return the destination {@linkplain State state} for this transition (i.e. the state this transition transitions
     * to), or {@code null} if this transition is {@linkplain #isInternal() internal} or
     * {@linkplain #isReflexive() reflexive}.
     */
    public State getDst() {
        return dst;
    }

    /**
     * @return whether or not this transition is internal (i.e. no state change occurs when it executes).
     */
    public boolean isInternal() {
        return type == Type.INTERNAL;
    }

    /**
     * @return whether or not this transition is reflexive (i.e. its source and destination states are the same).
     */
    public boolean isReflexive() {
        return type == Type.REFLEXIVE;
    }

    /**
     * Given a trigger string and the current state of the FSM associated with this transition, return whether or not
     * this transition can execute (i.e. if the given trigger matches this transition's trigger and if the given state
     * is one of this transition's sources).
     */
    boolean canExecute(String trigger, State currState) {
        return trigger.equals(this.trigger) && srcs.contains(currState);
    }

    boolean before(Event event) {
        return before != null ? before.apply(event) : true;
    }

    void after(Event event) {
        if (after != null) {
            after.accept(event);
        }
    }
}
