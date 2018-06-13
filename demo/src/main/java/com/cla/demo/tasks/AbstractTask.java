package com.cla.demo.tasks;

public abstract class AbstractTask implements Task {

    protected final String machineId;
    protected final TaskContext taskContext;

    public AbstractTask(String machineId, TaskContext taskContext) {
        this.machineId = machineId;
        this.taskContext = taskContext;
    }

    @Override
    public String getMachineId() {
        return machineId;
    }

    @Override
    public TaskContext getTaskContext() {
        return taskContext;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "machineId='" + machineId + '\'' +
                ", taskContext=" + taskContext +
                '}';
    }
}
