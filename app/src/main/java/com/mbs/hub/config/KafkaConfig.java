package com.mbs.hub.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka wiring — ADR-ARCH-004. Single broker (dev host, ADR-ARCH-011 LOCKED prototype):
 * one partition per topic. Topics are auto-created on application boot.
 */
@Configuration
public class KafkaConfig {

    @Bean NewTopic jiraInboundEventsTopic() {
        return TopicBuilder.name(KafkaTopics.JIRA_INBOUND_EVENTS).partitions(1).replicas(1).build();
    }
    @Bean NewTopic hubDomainEventsTopic() {
        return TopicBuilder.name(KafkaTopics.HUB_DOMAIN_EVENTS).partitions(1).replicas(1).build();
    }
    @Bean NewTopic hubAuditTopic() {
        return TopicBuilder.name(KafkaTopics.HUB_AUDIT).partitions(1).replicas(1)
                // retention — F-ARCH-NEW-02 Reading A (default 7 days)
                .config("retention.ms", String.valueOf(7L * 24 * 60 * 60 * 1000)).build();
    }
    @Bean NewTopic hubSyncCommandsTopic() {
        return TopicBuilder.name(KafkaTopics.HUB_SYNC_COMMANDS).partitions(1).replicas(1).build();
    }
    @Bean NewTopic hubDlqTopic() {
        return TopicBuilder.name(KafkaTopics.HUB_DLQ).partitions(1).replicas(1).build();
    }
}
