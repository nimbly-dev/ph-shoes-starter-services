package com.nimbly.phshoesbackend.commons.core.repository.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.nimbly.phshoesbackend.commons.core.config.SuppressionProperties;
import com.nimbly.phshoesbackend.commons.core.model.SuppressionEntry;
import com.nimbly.phshoesbackend.commons.core.model.SuppressionReason;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

class DynamoSuppressionRepositoryTest {

    @Test
    void put_skipsWhenEmailHashMissing() {
        DynamoDbClient client = Mockito.mock(DynamoDbClient.class);
        DynamoSuppressionRepository repository = new DynamoSuppressionRepository(client, new SuppressionProperties());

        SuppressionEntry entry = new SuppressionEntry();
        entry.setEmailHash(" ");

        repository.put(entry);

        verify(client, never()).putItem(any(PutItemRequest.class));
    }

    @Test
    void put_writesExpectedAttributes() {
        DynamoDbClient client = Mockito.mock(DynamoDbClient.class);
        SuppressionProperties properties = new SuppressionProperties();
        DynamoSuppressionRepository repository = new DynamoSuppressionRepository(client, properties);

        SuppressionEntry entry = new SuppressionEntry();
        entry.setEmailHash("hash123");
        entry.setReason(SuppressionReason.BOUNCE_HARD);
        entry.setSource("test");
        entry.setNotes("note");
        entry.setCreatedAt(Instant.EPOCH);
        entry.setUpdatedAt(Instant.ofEpochSecond(10));
        entry.setExpiresAt(25L);

        repository.put(entry);

        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(client).putItem(captor.capture());
        PutItemRequest request = captor.getValue();
        assertTrue(request.item().containsKey(properties.getEmailHashAttribute()));
        assertTrue(request.item().containsKey(properties.getReasonAttribute()));
        assertTrue(request.item().containsKey(properties.getSourceAttribute()));
        assertTrue(request.item().containsKey(properties.getNotesAttribute()));
        assertTrue(request.item().containsKey(properties.getCreatedAtAttribute()));
        assertTrue(request.item().containsKey(properties.getUpdatedAtAttribute()));
        assertTrue(request.item().containsKey(properties.getExpiresAtAttribute()));
    }

    @Test
    void isSuppressed_returnsFalseWhenItemMissing() {
        DynamoDbClient client = Mockito.mock(DynamoDbClient.class);
        when(client.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());
        DynamoSuppressionRepository repository = new DynamoSuppressionRepository(client, new SuppressionProperties());

        assertFalse(repository.isSuppressed("hash123"));
    }

    @Test
    void isSuppressed_returnsTrueWhenNoTtl() {
        DynamoDbClient client = Mockito.mock(DynamoDbClient.class);
        SuppressionProperties properties = new SuppressionProperties();
        Map<String, AttributeValue> item = Map.of(
                properties.getEmailHashAttribute(), AttributeValue.fromS("hash123"));
        when(client.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().item(item).build());
        DynamoSuppressionRepository repository = new DynamoSuppressionRepository(client, properties);

        assertTrue(repository.isSuppressed("hash123"));
    }

    @Test
    void isSuppressed_returnsFalseWhenExpired() {
        DynamoDbClient client = Mockito.mock(DynamoDbClient.class);
        SuppressionProperties properties = new SuppressionProperties();
        long expired = Instant.now().minusSeconds(60).getEpochSecond();
        Map<String, AttributeValue> item = Map.of(
                properties.getEmailHashAttribute(), AttributeValue.fromS("hash123"),
                properties.getExpiresAtAttribute(), AttributeValue.fromN(Long.toString(expired)));
        when(client.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().item(item).build());
        DynamoSuppressionRepository repository = new DynamoSuppressionRepository(client, properties);

        assertFalse(repository.isSuppressed("hash123"));
    }

    @Test
    void remove_skipsWhenBlank() {
        DynamoDbClient client = Mockito.mock(DynamoDbClient.class);
        DynamoSuppressionRepository repository = new DynamoSuppressionRepository(client, new SuppressionProperties());

        repository.remove(" ");

        verify(client, never()).deleteItem(any(DeleteItemRequest.class));
    }
}
