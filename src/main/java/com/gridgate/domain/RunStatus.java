package com.gridgate.domain;

/**
 * Lifecycle state of a cascade run.
 */
public enum RunStatus {
    PENDING,
    PLAN_READY,
    RUNNING,
    FULFILLED,
    EXHAUSTED,
    HALTED_AMBIGUOUS,
    CANCELLED
}
