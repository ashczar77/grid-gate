package com.gridgate.cascade;

/**
 * Result of evaluating a completed provider attempt within the cascade.
 */
public enum CascadeStep {
    FULFILLED,
    CONTINUE,
    EXHAUSTED,
    HALTED_AMBIGUOUS
}
