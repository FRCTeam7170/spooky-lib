package frc.team7170.lib.fsm2;

import java.util.function.Consumer;

class BaseState implements State {

    private final String name;
    private State parent;
    boolean accessible = false;
    boolean ignoreMistrigger = false;
    Consumer<Event> onEnter = null;
    Consumer<Event> onExit = null;

    BaseState(String name, State parent) {
        this.name = name;
        this.parent = parent;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public State getParent() {
        return parent;
    }

    @Override
    public boolean isAccessible() {
        return accessible;
    }

    @Override
    public boolean getIgnoreMistrigger() {
        return ignoreMistrigger;
    }

    @Override
    public void onEnter(Event event) {
        if (onEnter != null) {
            onEnter.accept(event);
        }
    }

    @Override
    public void onExit(Event event) {
        if (onExit != null) {
            onExit.accept(event);
        }
    }
}
