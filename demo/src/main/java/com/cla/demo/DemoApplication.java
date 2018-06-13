package com.cla.demo;

import ch.qos.logback.classic.Level;
import com.cla.demo.statemachine.Events;
import com.cla.demo.statemachine.Phases;
import com.cla.demo.statemachine.StateMachines;
import com.cla.demo.tasks.MappingTask;
import com.cla.demo.tasks.TaskContext;
import com.cla.demo.tasks.persitence.TasksDb;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.statemachine.StateMachine;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) throws Exception {
        setSMLoggingLevel(Level.ERROR);

        //onlyMapScenario();

        //mapAndIngestScenario();

        //mapAndIngestWithInterimScenario();
    }

    private static void mapAndIngestWithInterimScenario() throws Exception {
        String machineId = "1";
        buildTaskDb(machineId, "D:\\Data\\normal");

        final StateMachine<Phases, Events> mapAndIngestWithInterimSM = StateMachines.buildMapAndIngestWithInterim(machineId);
        mapAndIngestWithInterimSM.start();
    }

    private static void mapAndIngestScenario() throws Exception {
        String machineId = "1";
        buildTaskDb(machineId, "D:\\Data\\normal");

        final StateMachine<Phases, Events> mapAndIngestSM = StateMachines.buildMapAndIngest(machineId);
        mapAndIngestSM.start();


        Thread.sleep(14500);

        System.out.println(">>>>>>>>>>>>>>>>>>>> Pausing for 3 sec");
        mapAndIngestSM.sendEvent(Events.PAUSE);
        Thread.sleep(1000);

        System.out.println(">>>>>>>>>>>>>>>>>>>> Resuming");
        mapAndIngestSM.sendEvent(Events.RESUME);
    }

    private static void onlyMapScenario() throws Exception {
        String machineId = "1";
        buildTaskDb(machineId, "D:\\Data\\normal");

        final StateMachine<Phases, Events> onlyMapSM = StateMachines.buildOnlyMapStateMachine(machineId);
        onlyMapSM.start();


        Thread.sleep(5000);

        System.out.println(">>>>>>>>>>>>>>>>>>>> Pausing for 3 sec");
        onlyMapSM.sendEvent(Events.PAUSE);
        Thread.sleep(3000);

        System.out.println(">>>>>>>>>>>>>>>>>>>> Resuming");
        onlyMapSM.sendEvent(Events.RESUME);
    }

    private static void setSMLoggingLevel(Level level) {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.springframework")).setLevel(level);
    }

    private static void buildTaskDb(String machineId, String path) throws IOException {
        Flux.fromStream(Files.walk(Paths.get(path))
                .filter(Files::isRegularFile))
                .map(p -> newMappingTask(machineId, p.toString()))
                .subscribe(TasksDb::addTask);

    }

    private static MappingTask newMappingTask(String machineId, String path) {
        return new MappingTask(machineId, new TaskContext().add("path", path));
    }

}