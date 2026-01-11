package com.nimbly.phshoesbackend.commons.core.autoconfig;

import java.util.List;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import com.nimbly.phshoesbackend.commons.core.config.props.DynamoMigrationProperties;
import com.nimbly.phshoesbackend.commons.core.migrations.UpgradeContext;
import com.nimbly.phshoesbackend.commons.core.migrations.UpgradeStep;
import com.nimbly.phshoesbackend.commons.core.migrations.runner.DynamoUpgradeRunner;
import com.nimbly.phshoesbackend.commons.core.migrations.utility.TableCreator;
import com.nimbly.phshoesbackend.commons.core.migrations.version.DynamoMigrationVersionStore;
import com.nimbly.phshoesbackend.commons.core.migrations.version.MigrationVersionStore;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@AutoConfiguration
@ConditionalOnClass(DynamoDbClient.class)
@ConditionalOnBean(DynamoDbClient.class)
@ConditionalOnProperty(prefix = "phshoes.dynamo.migrations", name = "enabled", havingValue = "true")
public class DynamoMigrationsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public UpgradeContext upgradeContext(DynamoMigrationProperties properties) {
        return new UpgradeContext(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public TableCreator tableCreator(DynamoDbClient dynamoDbClient) {
        return new TableCreator(dynamoDbClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public MigrationVersionStore migrationVersionStore(DynamoDbClient dynamoDbClient,
                                                       DynamoMigrationProperties properties) {
        return new DynamoMigrationVersionStore(dynamoDbClient, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    ApplicationRunner dynamoUpgradeRunner(List<UpgradeStep> steps,
                                          UpgradeContext context,
                                          MigrationVersionStore versionStore) {
        return new DynamoUpgradeRunner(steps, context, versionStore);
    }
}
