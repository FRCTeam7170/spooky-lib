package frc.team7170.lib.fsm2;

import java.util.Map;

/**
 * {@code Event} represents the immutable context for a transition/state change in a
 * {@link FSM FSM}.
 *
 * @author Robert Russell
 */
public final class Event {

    /**
     * The {@link FSM FSM} in which the transition/state change occurred.
     */
    public final FSM machine;

    /**
     * The src (source) {@linkplain State state} (i.e. the state being transitioned from). The src and dst (destination)
     * states are equal for internal transitions and reflexive transitions.
     */
    public final State src;

    /**
     * The dst (destination) {@linkplain State state} (i.e. the state being transitioned to). The dst and src (source)
     * states are equal for internal transitions and reflexive transitions.
     */
    public final State dst;

    /**
     * The {@linkplain Transition transition} that is occurring. If the state change was forced (i.e. via one of the
     * {@code forceTo} methods on a {@link FSM FSM}), then this is {@code null}.
     */
    public final Transition transition;

    /**
     * The trigger that caused the state change. If the state change was forced (i.e. via one of the {@code forceTo}
     * methods on a {@link FSM FSM}), then this is {@code null}.
     */
    public final String trigger;

    /**
     * The arguments passed to the call of {@code trigger} or {@code forceTo} (on a
     * {@link FSM FSM}) that caused the state change.
     * This map is guaranteed to be non-null and, unless {@code trigger}/{@code forceTo} was invoked with an immutable
     * map, mutable.
     * Moreover, since the same {@code Event} object is passed to every callback in the callback chain for a state
     * change, changes to this map in one callback will be visible to other callbacks.
     */
    public final Map<String, Object> args;

    Event(FSM machine,
          State src,
          State dst,
          Transition transition,
          String trigger,
          Map<String, Object> args) {
        this.machine = machine;
        this.src = src;
        this.dst = dst;
        this.transition = transition;
        this.trigger = trigger;
        this.args = args;
    }
}
