package frc.team7170.lib.fsm;

import java.util.Map;

// TODO: add fields for S-typed version of src and dst?

/**
 * {@code Event} represents the immutable context for a transition/state change in a {@link FSM FSM}.
 *
 * @param <S> the state type.
 * @param <T> the trigger type.
 *
 * @author Robert Russell
 */
public final class Event<S, T> {

    /**
     * The {@link FSM FSM} in which the transition/state change occurred.
     */
    public final FSM<S, T> machine;

    /**
     * The src (source) {@linkplain State state} (i.e. the state being transitioned from). The src and dst (destination)
     * states are equal for internal transitions and reflexive transitions.
     */
    public final State<S, T> src;

    /**
     * The dst (destination) {@linkplain State state} (i.e. the state being transitioned to). The dst and src (source)
     * states are equal for internal transitions and reflexive transitions.
     */
    public final State<S, T> dst;

    /**
     * The trigger that caused the state change. If the state change was forced (i.e. via one of the {@code forceTo}
     * methods on a {@link FSM FSM}), then this is {@code null}.
     */
    public final T trigger;

    /**
     * <p>
     * The arguments passed to the call of {@code trigger} or {@code forceTo} (on a {@link FSM FSM}) that caused the
     * state change.
     * </p>
     * <p>
     * This map is guaranteed to be non-null and, unless {@code trigger}/{@code forceTo} was invoked with an immutable
     * map, mutable.
     * </p>
     * <p>
     * Since the same {@code Event} object is passed to every callback in the callback chain for a state change, changes
     * to this map in one callback will be visible to other callbacks.
     * </p>
     */
    public final Map<String, Object> args;

    Event(FSM<S, T> machine,
          State<S, T> src,
          State<S, T> dst,
          T trigger,
          Map<String, Object> args) {
        this.machine = machine;
        this.src = src;
        this.dst = dst;
        this.trigger = trigger;
        this.args = args;
    }
}
