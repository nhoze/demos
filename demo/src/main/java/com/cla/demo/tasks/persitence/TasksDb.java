package com.cla.demo.tasks.persitence;

import com.cla.demo.statemachine.Phases;
import com.cla.demo.tasks.Task;
import com.cla.demo.tasks.factory.TaskFactories;
import com.cla.demo.utils.PrintUtils;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cla.demo.utils.PrintUtils.threadPrinting;

public class TasksDb {

    private static final Table<String, Phases, List<Task>> newTasksTable = HashBasedTable.create();

    private static final Table<String, Phases, List<Task>> enqueuedTasksTable = HashBasedTable.create();

    private static final Table<String, Phases, List<Task>> doneTasksTable = HashBasedTable.create();

    public static synchronized void done(Task task) {
        safeGetTaskBucket(enqueuedTasksTable, task.getMachineId(), task.getPhase()).remove(task);
        safeGetTaskBucket(doneTasksTable, task.getMachineId(), task.getPhase()).add(task);
    }

    private static synchronized <T extends Task> List<T> safeGetTaskBucket(Table<String, Phases, List<T>> tasksTable, String machineId, Phases phase) {
        List<T> doneTasks = tasksTable.get(machineId, phase);
        if (doneTasks == null) {
            doneTasks = new LinkedList<>();
            tasksTable.put(machineId, phase, doneTasks);
        }
        return doneTasks;
    }

    public static synchronized void addTask(Task task) {
        safeGetTaskBucket(newTasksTable, task.getMachineId(), task.getPhase()).add(task);
    }

    public static synchronized <T extends Task> List<T> enqueueTasks(String machineId, Phases derivedFrom, Phases phase, int bulkSize) {
        List<Task> tasks = derivedFrom == null ?
                safeGetTaskBucket(newTasksTable, machineId, phase) :
                safeGetTaskBucket(doneTasksTable, machineId, derivedFrom);
        final int actualBulkSize = bulkSize < tasks.size() ? bulkSize : tasks.size();
        final Map<Task, Task> derivedToNextTask = tasks.subList(0, actualBulkSize).stream()
                .collect(Collectors.toMap(k -> k, v -> derivedFrom == null ?
                        v : TaskFactories.<T>factory(phase).newTask(machineId, v.getTaskContext())));
        tasks.removeAll(derivedToNextTask.keySet());
        threadPrinting(String.format("SM (%s), loaded %d tasks, %d left",
                machineId, derivedToNextTask.keySet().size(), tasks.size()));
        final LinkedList result = new LinkedList(derivedToNextTask.values());
        safeGetTaskBucket(enqueuedTasksTable, machineId, phase).addAll(result);
        return result;
    }

    public static long countTasksPhase(String machineId, Phases derivedFrom, Phases phase) {
        List<Task> tasks = derivedFrom == null ?
                safeGetTaskBucket(newTasksTable, machineId, phase) : safeGetTaskBucket(doneTasksTable, machineId, derivedFrom);
        return tasks.size();
    }

    public static void restorePhase(String machineId, Phases phase, Phases derivedFrom) {
        final List<Task> tasks = safeGetTaskBucket(enqueuedTasksTable, machineId, phase);
        if (!tasks.isEmpty()) {
            PrintUtils.threadPrinting(String.format("Restoring %d enqueued elements", tasks.size()));
            List<Task> prevBucket = derivedFrom == null ?
                    safeGetTaskBucket(newTasksTable, machineId, phase) :
                    safeGetTaskBucket(doneTasksTable, machineId, derivedFrom);
            Phases phaseToRestore = derivedFrom == null ? phase : derivedFrom;
            prevBucket.addAll(tasks.stream()
                    .map(t -> TaskFactories.factory(phaseToRestore).newTask(machineId, t.getTaskContext()))
                    .collect(Collectors.toList()));
            tasks.clear();
        }
    }
}
