package com.cla.demo.jobs;

import com.cla.demo.feeds.TaskFeeds;
import com.cla.demo.scheduling.PhaseSchedulers;
import com.cla.demo.statemachine.Events;
import com.cla.demo.statemachine.Phases;
import com.cla.demo.tasks.IngestTask;
import com.cla.demo.tasks.persitence.TasksDb;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import reactor.core.publisher.Mono;

public class IngestJob implements Action<Phases, Events> {

    @Override
    public void execute(StateContext<Phases, Events> context) {
        StateMachine<Phases, Events> stateMachine = context.getStateMachine();
        TaskFeeds.subscribeOnIngestTaskFeed(
                stateMachine.getId(),
                this::executeTaskMap, // -> task operation
                throwable -> stateMachine.sendEvent(Events.PAUSE), // pause on error
                () -> stateMachine.sendEvent(Events.DONE_INGEST) // Job completion operation
        );
    }

    private void executeTaskMap(IngestTask ingestTask) {
        Mono.just(ingestTask)
                .subscribeOn(PhaseSchedulers.INGEST_SCHEDULER)
                .doOnSuccess(it -> TasksDb.done(it))
                .subscribe(it -> it.execute());
    }
}
