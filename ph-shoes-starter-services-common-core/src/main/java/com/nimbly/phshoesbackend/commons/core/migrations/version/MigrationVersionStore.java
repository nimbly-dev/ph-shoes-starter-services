package com.nimbly.phshoesbackend.commons.core.migrations.version;

import java.util.Optional;

public interface MigrationVersionStore {
    Optional<String> getCurrentVersion(String serviceName);

    void saveCurrentVersion(String serviceName, String version);
}
