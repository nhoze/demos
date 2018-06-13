package com.cla.demo.tasks.factory;

import com.cla.demo.statemachine.Phases;
import com.cla.demo.tasks.Task;
import com.cla.demo.tasks.TaskContext;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

public interface TaskFactory<T extends Task> {

    T newTask(String machineId, TaskContext taskContext);


}
