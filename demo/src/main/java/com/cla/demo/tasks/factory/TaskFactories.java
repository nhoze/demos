package com.cla.demo.tasks.factory;

import com.cla.demo.statemachine.Phases;
import com.cla.demo.tasks.IngestTask;
import com.cla.demo.tasks.MappingTask;
import com.cla.demo.tasks.Task;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

public final class TaskFactories {

    private static Map<Phases, TaskFactory> factories = ImmutableMap.of(
            Phases.MAP, (machineId, taskContext) -> new MappingTask(machineId, taskContext),
            Phases.INGEST, (machineId, taskContext) -> new IngestTask(machineId, taskContext));

    public static <T extends Task> TaskFactory<T> factory(Phases phase) {
        return factories.get(phase);
    }
}
