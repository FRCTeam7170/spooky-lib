package frc.team7170.lib.fsm2;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Transition {

    public static final class Builder<T extends FiniteStateMachine.Builder> {

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

        public void before(Runnable callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            before(event -> {
                callback.run();
                return true;
            });
        }

        public void before(Consumer<Event> callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            before(event -> {
                callback.accept(event);
                return true;
            });
        }

        public void before(Function<Event, Boolean> callback) {
            before = Objects.requireNonNull(callback, "callback must be non-null");
        }

        public void after(Runnable callback) {
            Objects.requireNonNull(callback, "callback must be non-null");
            after(event -> callback.run());
        }

        public void after(Consumer<Event> callback) {
            after = Objects.requireNonNull(callback, "callback must be non-null");
        }

        public T build() {
            parent.addTransition(new Transition(this));
            return parent;
        }

        static <T extends FiniteStateMachine.Builder> Builder<T> normal(
                String trigger, List<? extends State> srcs, State dst, T parent
        ) {
            return new Builder<>(trigger, srcs, dst, parent, Type.NORMAL);
        }

        static <T extends FiniteStateMachine.Builder> Builder<T> internal(
                String trigger, List<? extends State> srcs, T parent
        ) {
            return new Builder<>(trigger, srcs, null, parent, Type.INTERNAL);
        }

        static <T extends FiniteStateMachine.Builder> Builder<T> reflexive(
                String trigger, List<? extends State> srcs, T parent
        ) {
            return new Builder<>(trigger, srcs, null, parent, Type.REFLEXIVE);
        }
    }

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

    public String getTrigger() {
        return trigger;
    }

    public List<State> getSrcs() {
        return List.copyOf(srcs);
    }

    public State getDst() {
        return dst;
    }

    public boolean isInternal() {
        return type == Type.INTERNAL;
    }

    public boolean isReflexive() {
        return type == Type.REFLEXIVE;
    }

    boolean canExecute(String trigger, State currState) {
        return trigger.equals(this.trigger) && srcs.contains(currState);
    }

    boolean before(Event event) {
        return before != null ? before.apply(event) : true;
    }

    void after(Event event) {
        after.accept(event);
    }
}
