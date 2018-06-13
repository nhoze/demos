package com.cla.demo.statemachine;

import com.cla.demo.jobs.*;
import com.cla.demo.utils.PrintUtils;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.config.configurers.StateConfigurer;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.transition.Transition;

import java.time.Duration;
import java.util.concurrent.Executors;

import static com.cla.demo.utils.PrintUtils.threadPrinting;

public final class StateMachines {


    private static final TaskExecutor SM_TASK_EXECUTOR = new ConcurrentTaskExecutor(
            Executors.newFixedThreadPool(15, new ThreadFactoryBuilder().setNameFormat("sm-thread-%d").build()));

    public static StateMachine<Phases, Events> buildOnlyMapStateMachine(String machineId) throws Exception {
        StateMachineBuilder.Builder<Phases, Events> smBuilder = StateMachineBuilder.builder();


        smBuilder.configureConfiguration()
                .withConfiguration()
                .machineId(machineId)
                .taskExecutor(SM_TASK_EXECUTOR)
                .listener(new TransitionPrinter(machineId));

        smBuilder.configureStates()
                .withStates()
                .initial(Phases.SCANNING)
                .state(Phases.PAUSED)
                .stateExit(Phases.SCANNING, SafeExitJob.ofAllPhases())
                .end(Phases.FINISHED)
                .and()
                .withStates()
                .parent(Phases.SCANNING)
                .initial(Phases.MAP)
                .state(Phases.MAP, new MapJob())
                .state(Phases.MAP_FINALIZE, new MapFinalizeJob())
                .history(Phases.HISTORY, StateConfigurer.History.DEEP);

        smBuilder.configureTransitions()
                .withExternal()
                .source(Phases.MAP).target(Phases.MAP_FINALIZE)
                .event(Events.DONE_MAP)
                .and()
                .withExternal()
                .source(Phases.MAP_FINALIZE).target(Phases.FINISHED)
                .event(Events.DONE_MAP_FINALIZE)
                .and()
                .withExternal()
                .source(Phases.SCANNING).target(Phases.PAUSED)
                .event(Events.PAUSE)
                .and()
                .withExternal()
                .source(Phases.PAUSED).target(Phases.HISTORY)
                .event(Events.RESUME)
                .and()
                .withExternal()
                .source(Phases.SCANNING).target(Phases.FINISHED)
                .event(Events.STOP);

        return smBuilder.build();
    }

    public static StateMachine<Phases, Events> buildMapAndIngest(String machineId) throws Exception {
        StateMachineBuilder.Builder<Phases, Events> smBuilder = StateMachineBuilder.builder();

        smBuilder.configureConfiguration()
                .withConfiguration()
                .machineId(machineId)
                .taskExecutor(SM_TASK_EXECUTOR)
                .listener(new TransitionPrinter(machineId));

        smBuilder.configureStates()
                .withStates()
                .initial(Phases.SCANNING)
                .state(Phases.PAUSED)
                .stateExit(Phases.SCANNING, SafeExitJob.ofAllPhases())
                .end(Phases.FINISHED)
                .and()
                .withStates()
                .parent(Phases.SCANNING)
                .initial(Phases.MAP)
                .state(Phases.MAP, new MapJob())
                .state(Phases.MAP_FINALIZE, new MapFinalizeJob())
                .state(Phases.INGEST, new IngestJob())
                .state(Phases.INGEST_FINALIZE, new IngestFinalizeJob())
                .history(Phases.HISTORY, StateConfigurer.History.SHALLOW);

        smBuilder.configureTransitions()
                .withExternal()
                .source(Phases.MAP).target(Phases.MAP_FINALIZE)
                .event(Events.DONE_MAP)
                .and()
                .withExternal()
                .source(Phases.MAP_FINALIZE).target(Phases.INGEST)
                .event(Events.DONE_MAP_FINALIZE)
                .and()
                .withExternal()
                .source(Phases.INGEST).target(Phases.INGEST_FINALIZE)
                .event(Events.DONE_INGEST)
                .and()
                .withExternal()
                .source(Phases.INGEST_FINALIZE).target(Phases.FINISHED)
                .event(Events.DONE_INGEST_FINALIZE)
                .and()
                .withExternal()
                .source(Phases.SCANNING).target(Phases.PAUSED)
                .event(Events.PAUSE)
                .and()
                .withExternal()
                .source(Phases.PAUSED).target(Phases.HISTORY)
                .event(Events.RESUME)
                .and()
                .withExternal()
                .source(Phases.SCANNING).target(Phases.FINISHED)
                .event(Events.STOP);

        return smBuilder.build();
    }

    public static StateMachine<Phases, Events> buildMapAndIngestWithInterim(String machineId) throws Exception {
        StateMachineBuilder.Builder<Phases, Events> smBuilder = StateMachineBuilder.builder();

        smBuilder.configureConfiguration()
                .withConfiguration()
                .machineId(machineId)
                .taskExecutor(SM_TASK_EXECUTOR)
                .listener(new TransitionPrinter(machineId));

        smBuilder.configureStates()
                .withStates()
                .initial(Phases.SCANNING)
                .state(Phases.PAUSED)
                .stateExit(Phases.SCANNING, SafeExitJob.ofAllPhases())
                .end(Phases.FINISHED)
                .and()
                .withStates()
                .parent(Phases.SCANNING)
                .initial(Phases.MAP)
                .state(Phases.MAP, new MapJob())
                .state(Phases.MAP_FINALIZE, new MapFinalizeJob())
                .state(Phases.INGEST, new IngestJob())
                .state(Phases.INGEST_INTERIM, new IngestInterimJob())
                .choice(Phases.DECIDE_INGEST_INTERIM)
                .state(Phases.INGEST_FINALIZE, new IngestFinalizeJob())
                .history(Phases.HISTORY, StateConfigurer.History.SHALLOW);

        smBuilder.configureTransitions()
                .withExternal()
                .source(Phases.MAP).target(Phases.MAP_FINALIZE)
                .event(Events.DONE_MAP)
                .and()
                .withExternal()
                .source(Phases.MAP_FINALIZE).target(Phases.INGEST)
                .event(Events.DONE_MAP_FINALIZE)
                .and()
                .withExternal()
                .source(Phases.INGEST).target(Phases.DECIDE_INGEST_INTERIM)
                .timer(Duration.ofSeconds(3).toMillis())
                .action(SafeExitJob.of(Phases.INGEST))
                .and()
                .withChoice()
                .source(Phases.DECIDE_INGEST_INTERIM).first(Phases.INGEST_INTERIM, randomInterimDecision())
                .last(Phases.INGEST)
                .and()
                .withExternal()
                .source(Phases.INGEST_INTERIM).target(Phases.INGEST)
                .event(Events.DONE_INTERIM)
                .and()
                .withExternal()
                .source(Phases.INGEST).target(Phases.INGEST_FINALIZE)
                .event(Events.DONE_INGEST)
                .and()
                .withExternal()
                .source(Phases.INGEST_FINALIZE).target(Phases.FINISHED)
                .event(Events.DONE_INGEST_FINALIZE)
                .and()
                .withExternal()
                .source(Phases.SCANNING).target(Phases.PAUSED)
                .event(Events.PAUSE)
                .and()
                .withExternal()
                .source(Phases.PAUSED).target(Phases.HISTORY)
                .event(Events.RESUME)
                .and()
                .withExternal()
                .source(Phases.SCANNING).target(Phases.FINISHED)
                .event(Events.STOP);

        return smBuilder.build();
    }

    private static Guard<Phases, Events> randomInterimDecision() {
        return context -> {
            final boolean decision = (int) (Math.random() * 10) % 2 == 0;
            PrintUtils.threadPrinting(decision == true ?
                    "Coin flip says head, do ingest interim!!!" :
                    "Coin flip Says tail, No ingest interim, weepee :)");
            return decision;
        };
    }

    private static class TransitionPrinter extends StateMachineListenerAdapter<Phases, Events> {

        private final String machineId;

        private TransitionPrinter(String machineId) {
            this.machineId = machineId;
        }

        @Override
        public void transition(Transition<Phases, Events> transition) {
            final Phases source = transition.getSource() == null ? null : transition.getSource().getId();
            final Phases target = transition.getTarget() == null ? null : transition.getTarget().getId();
            threadPrinting(String.format("SM (%s), %s -> %s", machineId, source, target));
        }
    }
}
