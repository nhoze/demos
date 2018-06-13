package com.cla.demo.jobs;

import com.cla.demo.feeds.TaskFeeds;
import com.cla.demo.feeds.TaskFeeds.FeedKey;
import com.cla.demo.statemachine.Events;
import com.cla.demo.statemachine.Phases;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import java.util.stream.Stream;

public class SafeExitJob implements Action<Phases, Events> {

    private final Phases[] phases;

    public static SafeExitJob ofAllPhases() {
        return new SafeExitJob(Phases.values());
    }

    public static SafeExitJob of(Phases... phases) {
        return new SafeExitJob(Phases.values());
    }

    public SafeExitJob(Phases[] phases) {
        this.phases = phases;
    }

    @Override
    public void execute(StateContext<Phases, Events> context) {
        final String machineId = context.getStateMachine().getId();
        Stream.of(phases)
                .forEach(p -> TaskFeeds.unSubscribeFromFeed(FeedKey.of(machineId, p)));
    }
}
