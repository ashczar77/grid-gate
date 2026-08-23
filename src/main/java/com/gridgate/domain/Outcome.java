package com.gridgate.domain;

/**
 * How a single provider call attempt ended.
 */
public enum Outcome {
    SUCCESS,
    REJECTED,
    UNREACHABLE,
    REFUSED,
    VOICEMAIL,
    AMBIGUOUS
}
