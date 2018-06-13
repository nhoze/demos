package com.cla.demo.statemachine;

public enum Phases {
    SCANNING,
    MAP,
    MAP_FINALIZE,
    INGEST,
    INGEST_FINALIZE,
    PAUSED,
    STOPPED,
    FINISHED,
    HISTORY, INGEST_INTERIM, DECIDE_INGEST_INTERIM;
}
