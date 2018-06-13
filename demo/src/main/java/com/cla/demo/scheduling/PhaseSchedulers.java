package com.cla.demo.scheduling;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class PhaseSchedulers {

    public static final Scheduler MAPPING_SCHEDULER = Schedulers.newParallel("Mapping",15);
    public static final Scheduler INGEST_SCHEDULER = Schedulers.newParallel("Ingest",15);
}
