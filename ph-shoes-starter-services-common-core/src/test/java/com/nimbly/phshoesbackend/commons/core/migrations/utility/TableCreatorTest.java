package com.nimbly.phshoesbackend.commons.core.migrations.utility;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTimeToLiveRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTimeToLiveResponse;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndexDescription;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;
import software.amazon.awssdk.services.dynamodb.model.TimeToLiveDescription;
import software.amazon.awssdk.services.dynamodb.model.TimeToLiveStatus;
import software.amazon.awssdk.services.dynamodb.model.UpdateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveRequest;

class TableCreatorTest {

    @Test
    void createTableIfNotExists_createsTable() {
        DynamoDbClient client = Mockito.mock(DynamoDbClient.class);
        when(client.describeTable(any(DescribeTableRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().message("missing").build());
        TableCreator creator = new TableCreator(client);

        creator.createTableIfNotExists(
                "table",
                List.of(AttributeDefinition.builder().attributeName("pk").attributeType(ScalarAttributeType.S).build()),
                List.of(KeySchemaElement.builder().attributeName("pk").keyType(software.amazon.awssdk.services.dynamodb.model.KeyType.HASH).build()),
                BillingMode.PAY_PER_REQUEST,
                1,
                1);

        verify(client).createTable(any(CreateTableRequest.class));
    }

    @Test
    void createTableIfNotExists_skipsWhenPresent() {
        DynamoDbClient client = Mockito.mock(DynamoDbClient.class);
        when(client.describeTable(any(DescribeTableRequest.class))).thenReturn(
                DescribeTableResponse.builder()
                        .table(TableDescription.builder().tableStatus(TableStatus.ACTIVE).build())
                        .build());
        TableCreator creator = new TableCreator(client);

        creator.createTableIfNotExists(
                "table",
                List.of(AttributeDefinition.builder().attributeName("pk").attributeType(ScalarAttributeType.S).build()),
                List.of(KeySchemaElement.builder().attributeName("pk").keyType(software.amazon.awssdk.services.dynamodb.model.KeyType.HASH).build()),
                BillingMode.PAY_PER_REQUEST,
                1,
                1);

        verify(client, never()).createTable(any(CreateTableRequest.class));
    }

    @Test
    void createGsiIfNotExists_skipsWhenIndexExists() {
        DynamoDbClient client = Mockito.mock(DynamoDbClient.class);
        when(client.describeTable(any(DescribeTableRequest.class))).thenReturn(
                DescribeTableResponse.builder()
                        .table(TableDescription.builder()
                                .globalSecondaryIndexes(List.of(GlobalSecondaryIndexDescription.builder()
                                        .indexName("gsi")
                                        .build()))
                                .build())
                        .build());
        TableCreator creator = new TableCreator(client);

        creator.createGsiIfNotExists("table", "gsi", "attr", ScalarAttributeType.S,
                BillingMode.PAY_PER_REQUEST, 1, 1);

        verify(client, never()).updateTable(any(UpdateTableRequest.class));
    }

    @Test
    void createGsiIfNotExists_updatesWhenMissing() {
        DynamoDbClient client = Mockito.mock(DynamoDbClient.class);
        when(client.describeTable(any(DescribeTableRequest.class))).thenReturn(
                DescribeTableResponse.builder()
                        .table(TableDescription.builder()
                                .globalSecondaryIndexes(List.of())
                                .build())
                        .build());
        TableCreator creator = new TableCreator(client);

        creator.createGsiIfNotExists("table", "gsi", "attr", ScalarAttributeType.S,
                BillingMode.PAY_PER_REQUEST, 1, 1);

        verify(client).updateTable(any(UpdateTableRequest.class));
    }

    @Test
    void enableTtlIfDisabled_skipsWhenMissingTable() {
        DynamoDbClient client = Mockito.mock(DynamoDbClient.class);
        when(client.describeTimeToLive(any(DescribeTimeToLiveRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().message("missing").build());
        TableCreator creator = new TableCreator(client);

        creator.enableTtlIfDisabled("table", "ttl");

        verify(client, never()).updateTimeToLive(any(UpdateTimeToLiveRequest.class));
    }

    @Test
    void enableTtlIfDisabled_updatesWhenDisabled() {
        DynamoDbClient client = Mockito.mock(DynamoDbClient.class);
        when(client.describeTimeToLive(any(DescribeTimeToLiveRequest.class))).thenReturn(
                DescribeTimeToLiveResponse.builder()
                        .timeToLiveDescription(TimeToLiveDescription.builder()
                                .timeToLiveStatus(TimeToLiveStatus.DISABLED)
                                .build())
                        .build());
        TableCreator creator = new TableCreator(client);

        creator.enableTtlIfDisabled("table", "ttl");

        verify(client).updateTimeToLive(any(UpdateTimeToLiveRequest.class));
    }
}
