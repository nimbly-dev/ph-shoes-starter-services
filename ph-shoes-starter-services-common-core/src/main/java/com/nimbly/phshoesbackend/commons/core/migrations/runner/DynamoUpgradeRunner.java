package com.nimbly.phshoesbackend.commons.core.migrations.runner;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import com.nimbly.phshoesbackend.commons.core.migrations.UpgradeContext;
import com.nimbly.phshoesbackend.commons.core.migrations.UpgradeStep;
import com.nimbly.phshoesbackend.commons.core.migrations.version.MigrationVersion;
import com.nimbly.phshoesbackend.commons.core.migrations.version.MigrationVersionStore;

public class DynamoUpgradeRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DynamoUpgradeRunner.class);
    private static final String DEFAULT_VERSION = "0.0.0";

    private final List<UpgradeStep> steps;
    private final UpgradeContext context;
    private final MigrationVersionStore versionStore;

    public DynamoUpgradeRunner(List<UpgradeStep> steps,
                               UpgradeContext context,
                               MigrationVersionStore versionStore) {
        this.steps = steps;
        this.context = context;
        this.versionStore = versionStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (CollectionUtils.isEmpty(steps)) {
            log.info("No DynamoDB upgrade steps registered.");
            return;
        }

        Map<String, String> currentVersions = new HashMap<>();
        steps.stream()
                .sorted(Comparator.comparingInt(UpgradeStep::order))
                .forEach(step -> {
                    String service = step.service();
                    if (!StringUtils.hasText(service)) {
                        throw new IllegalStateException("Migration step is missing service name");
                    }
                    String currentVersion = currentVersions.computeIfAbsent(service, this::loadCurrentVersion);
                    MigrationVersion stepTarget = MigrationVersion.parse(step.toVersion());
                    MigrationVersion existing = MigrationVersion.parse(currentVersion);
                    if (existing.compareTo(stepTarget) >= 0) {
                        log.info("Skipping Dynamo upgrade {} -> {} ({}) for service {} (current version {})",
                                step.fromVersion(), step.toVersion(), step.description(), service, currentVersion);
                        return;
                    }
                    log.info("Executing Dynamo upgrade {} -> {} ({}) for service {}",
                            step.fromVersion(), step.toVersion(), step.description(), service);
                    step.apply(context);
                    versionStore.saveCurrentVersion(service, step.toVersion());
                    currentVersions.put(service, step.toVersion());
                });
    }

    private String loadCurrentVersion(String service) {
        return versionStore.getCurrentVersion(service).orElse(DEFAULT_VERSION);
    }
}
