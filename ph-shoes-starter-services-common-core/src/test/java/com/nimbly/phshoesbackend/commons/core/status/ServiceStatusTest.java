package com.nimbly.phshoesbackend.commons.core.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ServiceStatusTest {

    @Test
    void builderFrom_copiesMetadataAndDependencies() {
        ServiceStatus original = ServiceStatus.builder()
                .serviceId("svc")
                .displayName("Service")
                .state(ServiceState.UP)
                .checkedAt(Instant.EPOCH)
                .uptimeSeconds(12)
                .metadataEntry("region", "apac")
                .dependency("db", ServiceDependencyStatus.builder()
                        .name("db")
                        .state(ServiceState.UP)
                        .description("ok")
                        .build())
                .build();

        ServiceStatus copy = ServiceStatus.builderFrom(original).build();

        assertEquals("svc", copy.getServiceId());
        assertEquals("apac", copy.getMetadata().get("region"));
        assertEquals("db", copy.getDependencies().get("db").getName());
    }

    @Test
    void getMetadata_returnsEmptyMapWhenNull() {
        ServiceStatus status = ServiceStatus.builder().build();

        assertTrue(status.getMetadata().isEmpty());
        assertTrue(status.getDependencies().isEmpty());
    }
}
