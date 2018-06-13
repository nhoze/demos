package com.cla.demo.tasks;

import com.cla.demo.statemachine.Phases;

import static com.cla.demo.utils.PrintUtils.threadPrinting;

public class IngestTask extends AbstractTask {


    public IngestTask(String machineId, TaskContext taskContext) {
        super(machineId, taskContext);
    }

    @Override
    public Phases getPhase() {
        return Phases.INGEST;
    }

    @Override
    public void execute() {
        String path = taskContext.get("path", String.class);
        threadPrinting(String.format("SM (%s),  Adding ingest result: %s", machineId, path));
    }
}
