package com.mbs.hub.config;

import com.mbs.hub.jira.JiraProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Lets Spring pick up hub.jira.* into JiraProperties record. */
@Configuration
@EnableConfigurationProperties(JiraProperties.class)
public class JiraPropertiesConfig {}
