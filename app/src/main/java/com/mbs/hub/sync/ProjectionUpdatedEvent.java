package com.mbs.hub.sync;

/**
 * Published by {@code JiraEventConsumer} after a successful UPSERT so the
 * materialized-view refresher knows to recompute.
 */
public record ProjectionUpdatedEvent(String issueKey, String projectKey) {}
