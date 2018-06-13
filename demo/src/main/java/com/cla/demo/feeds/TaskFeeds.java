package com.cla.demo.feeds;

import com.cla.demo.tasks.persitence.TasksDb;
import com.cla.demo.statemachine.Phases;
import com.cla.demo.tasks.Task;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TaskFeeds {

    private static Map<FeedKey, FeedControl> feedsRegistry = new HashMap<>();

    private static Scheduler producerScheduler = Schedulers.newParallel("producer", 15);

    public static <T extends Task> void subscribeOnIngestTaskFeed(String machineId,
                                                                  Consumer<T> tasksConsumer,
                                                                  Consumer<? super Throwable> errorHandler,
                                                                  Runnable completionHandler) {
        subscribeOnFeed(FeedKey.of(machineId, Phases.INGEST), Phases.MAP, tasksConsumer, errorHandler, completionHandler);
    }

    public static <T extends Task> void subscribeOnMapTaskFeed(String machineId,
                                                               Consumer<T> tasksConsumer,
                                                               Consumer<? super Throwable> errorHandler,
                                                               Runnable completionHandler) {
        subscribeOnFeed(FeedKey.of(machineId, Phases.MAP), null, tasksConsumer, errorHandler, completionHandler);
    }


    private static <T extends Task> void subscribeOnFeed(FeedKey feedKey,
                                                         Phases derivedFrom,
                                                         Consumer<T> tasksConsumer,
                                                         Consumer<? super Throwable> errorHandler,
                                                         Runnable completionHandler) {
        FeedControl result = feedsRegistry.get(feedKey);
        if (result == null) {
            TasksDb.restorePhase(feedKey.machineId, feedKey.phase, derivedFrom);
            Flux<T> tasksFlux = tasksProducer(derivedFrom, feedKey);
            Disposable subscriber = tasksFlux
                    .subscribe(tasksConsumer, errorHandler, completionHandler);
            feedsRegistry.put(feedKey, new FeedControl(tasksFlux, subscriber));
        }
    }

    private static <T extends Task> Flux<T> tasksProducer(Phases derivedFrom, FeedKey feedKey) {
        long tasksCount = TasksDb.countTasksPhase(feedKey.machineId, derivedFrom, feedKey.phase);
        return Flux
                .interval(Duration.ofSeconds(1), producerScheduler)
                .flatMap(time -> Flux.fromIterable(TasksDb.<T>enqueueTasks(feedKey.machineId,
                        derivedFrom, feedKey.phase, 10)))
                .doOnError(throwable -> throwable.printStackTrace())
                .take(tasksCount);
    }

    public static void unSubscribeFromFeed(FeedKey feedKey) {
        FeedControl feedControl = feedsRegistry.remove(feedKey);
        if (feedControl != null) {
            feedControl.subscriber.dispose();
        }
    }

    private static class FeedControl {

        private final Flux<? extends Task> tasksFlux;
        private final Disposable subscriber;

        public FeedControl(Flux<? extends Task> tasksFlux, Disposable subscriber) {
            this.tasksFlux = tasksFlux;
            this.subscriber = subscriber;
        }
    }

    public static class FeedKey {

        private final String machineId;
        private final Phases phase;

        public static FeedKey of(String machineId, Phases phase) {
            return new FeedKey(machineId, phase);
        }

        private FeedKey(String machineId, Phases phase) {
            this.machineId = machineId;
            this.phase = phase;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FeedKey feedKey = (FeedKey) o;

            if (machineId != null ? !machineId.equals(feedKey.machineId) : feedKey.machineId != null) return false;
            return phase == feedKey.phase;
        }

        @Override
        public int hashCode() {
            int result = machineId != null ? machineId.hashCode() : 0;
            result = 31 * result + (phase != null ? phase.hashCode() : 0);
            return result;
        }
    }
}
