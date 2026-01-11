package com.nimbly.phshoesbackend.commons.core.migrations.version;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.util.StringUtils;
import com.nimbly.phshoesbackend.commons.core.config.props.DynamoMigrationProperties;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

public class DynamoMigrationVersionStore implements MigrationVersionStore {
    private static final String BASE_TABLE_NAME = "migration_versions";
    private static final String SERVICE_ATTRIBUTE = "service";
    private static final String VERSION_ATTRIBUTE = "currentVersion";
    private static final String UPDATED_AT_ATTRIBUTE = "updatedAt";

    private final DynamoDbClient dynamoDbClient;
    private final DynamoMigrationProperties properties;

    public DynamoMigrationVersionStore(DynamoDbClient dynamoDbClient, DynamoMigrationProperties properties) {
        this.dynamoDbClient = Objects.requireNonNull(dynamoDbClient, "dynamoDbClient");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public Optional<String> getCurrentVersion(String serviceName) {
        String tableName = resolveTableName();
        try {
            var response = dynamoDbClient.getItem(GetItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of(SERVICE_ATTRIBUTE, AttributeValue.fromS(serviceName)))
                    .build());
            if (!response.hasItem()) {
                return Optional.empty();
            }
            AttributeValue versionValue = response.item().get(VERSION_ATTRIBUTE);
            if (versionValue == null || !StringUtils.hasText(versionValue.s())) {
                throw new IllegalStateException("Missing migration version for service " + serviceName);
            }
            return Optional.of(versionValue.s());
        } catch (ResourceNotFoundException ex) {
            throw new IllegalStateException("Migration versions table not found: " + tableName, ex);
        }
    }

    @Override
    public void saveCurrentVersion(String serviceName, String version) {
        String tableName = resolveTableName();
        try {
            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(tableName)
                    .item(Map.of(
                            SERVICE_ATTRIBUTE, AttributeValue.fromS(serviceName),
                            VERSION_ATTRIBUTE, AttributeValue.fromS(version),
                            UPDATED_AT_ATTRIBUTE, AttributeValue.fromS(Instant.now().toString())))
                    .build());
        } catch (ResourceNotFoundException ex) {
            throw new IllegalStateException("Migration versions table not found: " + tableName, ex);
        }
    }

    private String resolveTableName() {
        String prefix = properties.getTablePrefix();
        if (!StringUtils.hasText(prefix)) {
            return BASE_TABLE_NAME;
        }
        return prefix + BASE_TABLE_NAME;
    }
}
