package frc.team7170.lib.fsm2;

import java.util.function.Consumer;

// TODO: rename to StringState

/**
 * A basic implementation of {@link State State} used internally for when a {@link FSM FSM} is instantiated using
 * strings to represent states.
 *
 * @param <T> the trigger type.
 *
 * @author Robert Russell
 */
class BaseState<T> implements State<String, T> {

    private final String name;
    private State<String, T> parent;
    boolean accessible = false;
    boolean ignoreInvalidTriggers = false;
    Consumer<Event<String, T>> onEnter = null;
    Consumer<Event<String, T>> onExit = null;

    BaseState(String name, State<String, T> parent) {
        this.name = name;
        this.parent = parent;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public State<String, T> getParent() {
        return parent;
    }

    @Override
    public boolean isAccessible() {
        return accessible;
    }

    @Override
    public boolean getIgnoreInvalidTriggers() {
        return ignoreInvalidTriggers;
    }

    @Override
    public void onEnter(Event<String, T> event) {
        if (onEnter != null) {
            onEnter.accept(event);
        }
    }

    @Override
    public void onExit(Event<String, T> event) {
        if (onExit != null) {
            onExit.accept(event);
        }
    }
}
