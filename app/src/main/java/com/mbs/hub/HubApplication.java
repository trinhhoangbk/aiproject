package com.mbs.hub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Multi-Project Workload & Resource Balancing Hub.
 *
 * <p>Baselines in effect — see {@code 04-PLANNING/PLAN_APPROVAL_RECORD.md}.</p>
 */
@SpringBootApplication
@EnableScheduling
public class HubApplication {

    public static void main(String[] args) {
        SpringApplication.run(HubApplication.class, args);
    }
}
