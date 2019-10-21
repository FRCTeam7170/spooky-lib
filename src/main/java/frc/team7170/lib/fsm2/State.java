package frc.team7170.lib.fsm2;

// TODO: warn about changing return of methods (undef behaviour)
public interface State {
    String name();

    default State parent() {
        return null;
    }

    default boolean accessible() {
        return true;
    }

    default boolean ignoreMistrigger() {
        return false;
    }

    default void onEnter(Event event) {}

    default void onExit(Event event) {}
}
