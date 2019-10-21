package frc.team7170.lib.fsm2;

// TODO: warn about changing return of methods (undef behaviour)
public interface State {
    String name();

    default State getParent() {
        return null;
    }

    default boolean isAccessible() {
        return true;
    }

    default boolean getIgnoreMistrigger() {
        return false;
    }

    default void onEnter(Event event) {}

    default void onExit(Event event) {}
}
