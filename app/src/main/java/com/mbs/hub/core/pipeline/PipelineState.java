package com.mbs.hub.core.pipeline;

/**
 * Pipeline project lifecycle — 04 Domain §F (F-05 accepted by BUSINESS APPROVED).
 */
public enum PipelineState {
    DRAFT, READY_TO_ALLOCATE, ALLOCATED,
    CHANGES_REQUIRED, REJECTED, CLOSED, REASSIGNED
}
