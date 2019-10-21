package frc.team7170.lib.fsm2;

// TODO: rename ignoreMistrigger to ignoreInvalidTrigger

/**
 * A state for a {@link FiniteStateMachine FiniteStateMachine}.
 *
 * @apiNote This interface is provided to be implemented by enums to create an enum of all the states for a
 * {@code FiniteStateMachine}. Considering the restrictions on instantiation of {@code FiniteStateMachines},
 * implementing this interface on anything other than an enum is pointless.
 *
 * @author Robert Russell
 */
public interface State {

    /**
     * Get the name of this state. The returned name is relative, meaning it does not contain its ancestors' names
     * delimited by {@value FiniteStateMachine#SUB_STATE_SEP}.
     *
     * @apiNote This method is called {@code name} as opposed to {@code getName} so it is automatically satisfied by
     * enum implementors. Since the {@link Enum#name() name} method for enums is final and the sub state
     * separator/delimiter ({@value FiniteStateMachine#SUB_STATE_SEP}) is not a valid identifier character in Java, this
     * guarantees that users can not create enum-based states with invalid names (i.e. containing
     * {@value FiniteStateMachine#SUB_STATE_SEP}). Moreover, since {@link FiniteStateMachine FiniteStateMachines} can
     * only be instantiated via raw strings or via an enum, this effectively guarantees users cannot make states with
     * invalid names at all.
     *
     * @return The name of this state.
     */
    String name();

    /**
     * Get the parent {@code State} of this state, or {@code null} if it has no parent.
     *
     * @implSpec Dynamically changing the return value of this method will result in undefined behaviour.
     *
     * @return The parent {@code State} of this state, or {@code null} if it has no parent.
     */
    default State getParent() {
        return null;
    }

    /**
     * Get whether or not this state is "accessible" (can be entered by a
     * {@link FiniteStateMachine FiniteStateMachine}).
     *
     * @apiNote This property is mainly provided to allow one to create state hierarchies in which certain states may
     * only be entered through their (distant) children (e.g. a state "A" might be inaccessible, but a
     * {@code FiniteStateMachine} can still be considered in state "A" if it is in an accessible child/grandchild/etc.
     * of "A"). Typically, this property might be leveraged to assure a {@code FiniteStateMachine} can only assume leaf
     * states (i.e. states with no children).
     *
     * @implSpec Dynamically changing the return value of this method will result in undefined behaviour.
     *
     * @return Whether or not this state is "accessible".
     */
    default boolean isAccessible() {
        return true;
    }

    /**
     * Get whether or not to ignore invalid triggers while this state is the current state in a
     * {@link FiniteStateMachine FiniteStateMachine}.
     *
     * @implSpec Dynamically changing the return value of this method will result in undefined behaviour.
     *
     * @return Whether or not to ignore invalid triggers while this state is the current state in a
     * {@code FiniteStateMachine}.
     */
    default boolean getIgnoreMistrigger() {
        return false;
    }

    /**
     * Callback that is executed whenever this state is entered.
     *
     * @param event The state change context.
     */
    default void onEnter(Event event) {}

    /**
     * Callback that is executed whenever this state is exited.
     *
     * @param event The state change context.
     */
    default void onExit(Event event) {}
}
