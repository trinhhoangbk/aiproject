package com.mbs.hub.config;

/** Topic names, kept together so they're grep-friendly. */
public final class KafkaTopics {
    private KafkaTopics() {}
    public static final String JIRA_INBOUND_EVENTS = "jira.inbound.events";
    public static final String HUB_DOMAIN_EVENTS   = "hub.domain.events";
    public static final String HUB_AUDIT           = "hub.audit";
    public static final String HUB_SYNC_COMMANDS   = "hub.sync.commands";
    public static final String HUB_DLQ             = "hub.dlq";
}
