package com.cla.demo.tasks;

import com.cla.demo.statemachine.Phases;

public interface Task {

    String getMachineId();

    Phases getPhase();

    TaskContext getTaskContext();

    void execute();
}
