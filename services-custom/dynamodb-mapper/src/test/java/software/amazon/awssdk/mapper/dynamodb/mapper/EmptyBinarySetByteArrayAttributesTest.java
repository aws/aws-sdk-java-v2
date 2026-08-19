/*
 * Copyright 2020 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package software.amazon.awssdk.mapper.dynamodb.mapper;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.awssdk.mapper.dynamodb.pojos.BinaryAttributeByteArrayClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests empty binary set attributes represented as byte[]
 */
@RunWith(MockitoJUnitRunner.class)
public class EmptyBinarySetByteArrayAttributesTest {

    private static final String BINARY_SET_ATTRIBUTE = "binarySetAttribute";
    private static final String KEY_NAME = "key";
    private static final String KEY_VALUE = "test-id";
    private static final byte[] EMPTY_BINARY = new byte[]{};
    private static final Set<byte[]> EMPTY_BINARY_SET;
    private static final AttributeValue EMPTY_BINARY_SET_AV = AttributeValue.builder().bs(SdkBytes.fromByteArray(EMPTY_BINARY)).build();

    private static final Map<String, AttributeValue> ITEM_MAP;
    private static final Map<String, AttributeValue> KEY_MAP;
    private static final BinaryAttributeByteArrayClass TEST_OBJECT;

    static {
        EMPTY_BINARY_SET = new HashSet<>();
        EMPTY_BINARY_SET.add(EMPTY_BINARY);

        KEY_MAP = new HashMap<>();
        KEY_MAP.put(KEY_NAME, AttributeValue.builder().s(KEY_VALUE).build());

        ITEM_MAP = new HashMap<>();
        ITEM_MAP.put(KEY_NAME, AttributeValue.builder().s(KEY_VALUE).build());
        ITEM_MAP.put(BINARY_SET_ATTRIBUTE, EMPTY_BINARY_SET_AV);

        TEST_OBJECT = new BinaryAttributeByteArrayClass();
        TEST_OBJECT.setKey(KEY_VALUE);
        TEST_OBJECT.setBinarySetAttribute(EMPTY_BINARY_SET);
    }

    @Mock
    private DynamoDbClient mockDynamo;

    @Captor
    private ArgumentCaptor<GetItemRequest> getItemRequestCaptor;

    @Captor
    private ArgumentCaptor<PutItemRequest> putItemRequestCaptor;

    @Captor
    private ArgumentCaptor<UpdateItemRequest> updateItemRequestArgumentCaptor;

    @Test
    public void testLoad() {
        when(mockDynamo.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().item(ITEM_MAP).build());
        DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(mockDynamo);

        BinaryAttributeByteArrayClass x = dynamoDBMapper.load(BinaryAttributeByteArrayClass.class,
                                                              ITEM_MAP.get(KEY_NAME).s());
        assertEquals(ITEM_MAP.get(KEY_NAME).s(), x.getKey());
        // v2 SdkBytes.asByteArray() returns a defensive copy, so Set equality (which compares byte[]
        // elements by identity) would fail; compare the single element by content instead.
        Set<byte[]> loadedSet = x.getBinarySetAttribute();
        assertEquals(EMPTY_BINARY_SET.size(), loadedSet.size());
        assertArrayEquals(EMPTY_BINARY_SET.iterator().next(), loadedSet.iterator().next());

        verify(mockDynamo).getItem(getItemRequestCaptor.capture());
        GetItemRequest getItemRequest = getItemRequestCaptor.getValue();
        assertEquals(KEY_MAP, getItemRequest.key());
    }

    @Test
    public void testSaveUsingPut() {
        DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(mockDynamo);

        dynamoDBMapper.save(TEST_OBJECT, DynamoDBMapperConfig.builder()
                                                             .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.PUT)
                                                             .build());

        verify(mockDynamo).putItem(putItemRequestCaptor.capture());
        PutItemRequest putItemRequest = putItemRequestCaptor.getValue();
        assertEquals(ITEM_MAP, putItemRequest.item());
    }

    @Test
    public void testSaveUsingUpdate() {
        when(mockDynamo.updateItem(any(UpdateItemRequest.class)))
            .thenReturn(UpdateItemResponse.builder().attributes(ITEM_MAP).build());
        DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(mockDynamo);

        dynamoDBMapper.save(TEST_OBJECT, DynamoDBMapperConfig.builder()
                                                             .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.UPDATE)
                                                             .build());

        verify(mockDynamo).updateItem(updateItemRequestArgumentCaptor.capture());
        UpdateItemRequest updateItemRequest = updateItemRequestArgumentCaptor.getValue();
        assertEquals(KEY_MAP, updateItemRequest.key());
        Map<String, AttributeValueUpdate> updates = updateItemRequest.attributeUpdates();
        AttributeValueUpdate attributeValueUpdate = updates.get(BINARY_SET_ATTRIBUTE);
        assertEquals(EMPTY_BINARY_SET_AV, attributeValueUpdate.value());
        assertEquals("PUT", attributeValueUpdate.actionAsString());
    }
}
