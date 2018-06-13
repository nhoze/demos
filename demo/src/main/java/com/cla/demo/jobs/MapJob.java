package com.cla.demo.jobs;

import com.cla.demo.scheduling.PhaseSchedulers;
import com.cla.demo.tasks.persitence.TasksDb;
import com.cla.demo.feeds.TaskFeeds;
import com.cla.demo.feeds.TaskFeeds.FeedKey;
import com.cla.demo.statemachine.Events;
import com.cla.demo.statemachine.Phases;
import com.cla.demo.tasks.MappingTask;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import reactor.core.publisher.Mono;

public class MapJob implements Action<Phases, Events> {

    @Override
    public void execute(StateContext<Phases, Events> context) {
        StateMachine<Phases, Events> stateMachine = context.getStateMachine();
        TaskFeeds.subscribeOnMapTaskFeed(
                stateMachine.getId(),
                this::executeTaskMap, // -> task operation
                throwable -> stateMachine.sendEvent(Events.PAUSE), // pause on error operation
                () -> stateMachine.sendEvent(Events.DONE_MAP) // Notify done job on completion
        );
    }

    private void executeTaskMap(MappingTask mappingTask) {
        Mono.just(mappingTask)
                .subscribeOn(PhaseSchedulers.MAPPING_SCHEDULER)
                .doOnSuccess(mt -> TasksDb.done(mt))
                .subscribe(mt -> mt.execute());
    }
}
