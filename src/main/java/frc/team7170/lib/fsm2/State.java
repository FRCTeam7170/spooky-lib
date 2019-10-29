package frc.team7170.lib.fsm2;

import java.util.Objects;

/**
 * A state for a {@link FSM FSM}.
 *
 * @apiNote This interface is provided to be implemented by enums to create an enum of all the states for a {@code FSM}.
 * Considering the restrictions on instantiation of {@code FSMs}, implementing this interface on anything other than an
 * enum is pointless.
 *
 * @author Robert Russell
 */
public interface State {

    /**
     * Get the name of this state. The returned name is relative, meaning it does not contain its ancestors' names
     * delimited by {@value FSM#SUB_STATE_SEP}.
     *
     * @apiNote This method is called {@code name} as opposed to {@code getName} so it is automatically satisfied by
     * enum implementors. Since the {@link Enum#name() name} method for enums is final and the sub state
     * separator/delimiter ({@value FSM#SUB_STATE_SEP}) is not a valid identifier character in Java, this guarantees
     * that users can not create enum-based states with invalid names (i.e. containing {@value FSM#SUB_STATE_SEP}).
     * Moreover, since {@link FSM FSMs} can only be instantiated with strings or an enum representing states, this
     * effectively guarantees users cannot make states with invalid names at all.
     *
     * @return the name of this state.
     */
    String name();

    /**
     * Get the parent {@code State} of this state, or {@code null} if it has no parent.
     *
     * @implSpec Dynamically changing the return value of this method will result in undefined behaviour.
     *
     * @return the parent {@code State} of this state, or {@code null} if it has no parent.
     */
    default State getParent() {
        return null;
    }

    /**
     * Get whether or not this state is "accessible" (can be directly entered by a {@link FSM FSM}).
     *
     * @apiNote This property is mainly provided to allow one to create state hierarchies in which certain states may
     * only be entered through their (distant) children (e.g. a state "A" might be inaccessible, but a {@code FSM} can
     * still be considered in state "A" if it is in an accessible child/grandchild/etc. of "A"). Typically, this
     * property might be leveraged to assure a {@code FSM} can only assume leaf states (i.e. states with no children).
     *
     * @implSpec Dynamically changing the return value of this method will result in undefined behaviour.
     *
     * @return whether or not this state is "accessible".
     */
    default boolean isAccessible() {
        return true;
    }

    /**
     * Get whether or not to ignore invalid triggers while this state is the current state in a {@link FSM FSM}.
     *
     * @implSpec Dynamically changing the return value of this method will result in undefined behaviour.
     *
     * @return whether or not to ignore invalid triggers while this state is the current state in a {@code FSM}.
     */
    default boolean getIgnoreInvalidTriggers() {
        return false;
    }

    /**
     * Callback that is executed after this state is entered.
     *
     * @param event the state change context.
     */
    default void onEnter(Event event) {}

    /**
     * Callback that is executed before this state is exited.
     *
     * @param event the state change context.
     */
    default void onExit(Event event) {}

    /**
     * Get the full name of the the given state (i.e. the name containing all the ancestor names delimited by
     * {@value FSM#SUB_STATE_SEP}).
     *
     * @param state the state to get the full name of.
     * @return the full name of the the given state (i.e. the name containing all the ancestor names delimited by
     * {@value FSM#SUB_STATE_SEP}).
     * @throws NullPointerException if the given state is {@code null}.
     */
    static String fullName(State state) {
        StringBuilder sb = new StringBuilder();
        sb.append(Objects.requireNonNull(state, "state must be non-null").name());
        state = state.getParent();
        while (state != null) {
            sb.append(FSM.SUB_STATE_SEP);
            sb.append(state.name());
            state = state.getParent();
        }
        return sb.toString();
    }

    /**
     * Get whether or not the given state "parent" is in the lineage/ancestry of the given state "child". A state is
     * considered in its own lineage.
     *
     * @param child the (potential) child state.
     * @param parent the (potential) parent state.
     * @return whether or not the given state "parent" is in the lineage/ancestry of the given state "child".
     * @throws NullPointerException if either of the given states are {@code null}.
     */
    static boolean inLineage(State child, State parent) {
        Objects.requireNonNull(child, "child state must be non-null");
        Objects.requireNonNull(parent, "parent state must be non-null");
        for (; child != null; child = child.getParent()) {
            if (child == parent) {
                return true;
            }
        }
        return false;
    }
}
