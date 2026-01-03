package com.nimbly.phshoesbackend.commons.web.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import com.nimbly.phshoesbackend.commons.core.config.props.ServiceStatusProperties;
import com.nimbly.phshoesbackend.commons.core.status.ServiceDependencyStatus;
import com.nimbly.phshoesbackend.commons.core.status.ServiceState;
import com.nimbly.phshoesbackend.commons.core.status.ServiceStatus;
import com.nimbly.phshoesbackend.commons.core.status.ServiceStatusContributor;

class ServiceStatusControllerTest {

    @Test
    void getStatus_includesPropertiesAndContributors() {
        ServiceStatusProperties properties = new ServiceStatusProperties();
        properties.setServiceId("catalog");
        properties.setDisplayName("Catalog");
        properties.setEnvironment("dev");
        properties.setVersion("1.0.0");
        properties.setDescription("test");
        properties.setMetadata(Map.of("region", "apac"));

        ServiceStatusContributor contributor = builder -> builder
                .metadataEntry("build", "123")
                .dependency("redis", ServiceDependencyStatus.builder()
                        .name("redis")
                        .state(ServiceState.UP)
                        .description("ok")
                        .build());

        ObjectProvider<ServiceStatusContributor> provider = new SimpleProvider(List.of(contributor));
        ServiceStatusController controller = new ServiceStatusController(properties, provider);

        ServiceStatus status = controller.getStatus();

        assertEquals("catalog", status.getServiceId());
        assertEquals("apac", status.getMetadata().get("region"));
        assertEquals("123", status.getMetadata().get("build"));
        assertNotNull(status.getDependencies().get("redis"));
    }

    private static final class SimpleProvider implements ObjectProvider<ServiceStatusContributor> {
        private final List<ServiceStatusContributor> contributors;

        private SimpleProvider(List<ServiceStatusContributor> contributors) {
            this.contributors = contributors;
        }

        @Override
        public ServiceStatusContributor getObject(Object... args) {
            return contributors.get(0);
        }

        @Override
        public ServiceStatusContributor getIfAvailable() {
            return contributors.isEmpty() ? null : contributors.get(0);
        }

        @Override
        public ServiceStatusContributor getIfUnique() {
            return contributors.size() == 1 ? contributors.get(0) : null;
        }

        @Override
        public Stream<ServiceStatusContributor> stream() {
            return contributors.stream();
        }

        @Override
        public Stream<ServiceStatusContributor> orderedStream() {
            return contributors.stream();
        }
    }
}
