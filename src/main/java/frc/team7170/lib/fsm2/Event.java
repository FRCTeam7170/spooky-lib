package frc.team7170.lib.fsm2;

import java.util.Map;

public class Event {

    public final FiniteStateMachine machine;
    public final State src;
    public final State dst;
    public final Transition transition;
    public final String trigger;
    public final Map<String, Object> args;

    Event(FiniteStateMachine machine,
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
