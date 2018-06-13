package com.cla.demo.tasks;

import com.cla.demo.statemachine.Phases;

import java.util.Map;

import static com.cla.demo.utils.PrintUtils.threadPrinting;

public class MappingTask extends AbstractTask {

    public MappingTask(String machineId, TaskContext taskContext) {
      super(machineId,taskContext);
    }


    @Override
    public Phases getPhase() {
        return Phases.MAP;
    }

    @Override
    public void execute() {
        String path = taskContext.get("path", String.class);
        threadPrinting(String.format("SM (%s),  Adding Mapping result: %s", machineId, path));
    }
}
