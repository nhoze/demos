package com.cla.demo.jobs;

import com.cla.demo.statemachine.Events;
import com.cla.demo.statemachine.Phases;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

public class MapFinalizeJob implements Action<Phases, Events> {

    @Override
    public void execute(StateContext<Phases, Events> context) {
        System.out.print("Executing map finalize...");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        System.out.print(" DONE!!!");
        System.out.println();
        context.getStateMachine().sendEvent(Events.DONE_MAP_FINALIZE);
    }
}
