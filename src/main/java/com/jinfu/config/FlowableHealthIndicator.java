package com.jinfu.config;

import lombok.RequiredArgsConstructor;
import org.flowable.engine.RepositoryService;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Flowable process engine health check, exposed via /actuator/health.
 * Bean name "flowable" becomes the health component key.
 */
@Component("flowable")
@RequiredArgsConstructor
public class FlowableHealthIndicator implements HealthIndicator {

    private final RepositoryService repositoryService;

    @Override
    public Health health() {
        try {
            long processDefinitions = repositoryService.createProcessDefinitionQuery().count();
            return Health.up()
                    .withDetail("engine", "running")
                    .withDetail("processDefinitions", processDefinitions)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
