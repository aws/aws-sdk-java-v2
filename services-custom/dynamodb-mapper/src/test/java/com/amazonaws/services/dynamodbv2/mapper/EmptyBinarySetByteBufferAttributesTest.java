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
package com.amazonaws.services.dynamodbv2.mapper;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.amazonaws.services.dynamodbv2.pojos.BinaryAttributeByteBufferClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests empty binary set attributes represented as ByteBuffer
 */
@RunWith(MockitoJUnitRunner.class)
public class EmptyBinarySetByteBufferAttributesTest {

    private static final String BINARY_SET_ATTRIBUTE = "binarySetAttribute";
    private static final String KEY_NAME = "key";
    private static final String KEY_VALUE = "test-id";
    private static final ByteBuffer EMPTY_BINARY = ByteBuffer.wrap(new byte[]{});
    private static final Set<ByteBuffer> EMPTY_BINARY_SET;
    private static final AttributeValue EMPTY_BINARY_SET_AV = new AttributeValue().withBS(EMPTY_BINARY);

    private static final Map<String, AttributeValue> ITEM_MAP;
    private static final Map<String, AttributeValue> KEY_MAP;
    private static final BinaryAttributeByteBufferClass TEST_OBJECT;

    static {
        EMPTY_BINARY_SET = new HashSet<>();
        EMPTY_BINARY_SET.add(EMPTY_BINARY);

        KEY_MAP = new HashMap<>();
        KEY_MAP.put(KEY_NAME, new AttributeValue().withS(KEY_VALUE));

        ITEM_MAP = new HashMap<>();
        ITEM_MAP.put(KEY_NAME, new AttributeValue().withS(KEY_VALUE));
        ITEM_MAP.put(BINARY_SET_ATTRIBUTE, EMPTY_BINARY_SET_AV);

        TEST_OBJECT = new BinaryAttributeByteBufferClass();
        TEST_OBJECT.setKey(KEY_VALUE);
        TEST_OBJECT.setBinarySetAttribute(EMPTY_BINARY_SET);
    }

    @Mock
    private AmazonDynamoDB mockDynamo;

    @Captor
    private ArgumentCaptor<GetItemRequest> getItemRequestCaptor;

    @Captor
    private ArgumentCaptor<PutItemRequest> putItemRequestCaptor;

    @Captor
    private ArgumentCaptor<UpdateItemRequest> updateItemRequestArgumentCaptor;

    @Test
    public void testLoad() {
        when(mockDynamo.getItem(any(GetItemRequest.class))).thenReturn(new GetItemResult().withItem(ITEM_MAP));
        DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(mockDynamo);

        BinaryAttributeByteBufferClass x = dynamoDBMapper.load(BinaryAttributeByteBufferClass.class,
                                                               ITEM_MAP.get(KEY_NAME).getS());
        assertEquals(ITEM_MAP.get(KEY_NAME).getS(), x.getKey());
        assertEquals(EMPTY_BINARY_SET, x.getBinarySetAttribute());

        verify(mockDynamo).getItem(getItemRequestCaptor.capture());
        GetItemRequest getItemRequest = getItemRequestCaptor.getValue();
        assertEquals(KEY_MAP, getItemRequest.getKey());
    }

    @Test
    public void testSaveUsingPut() {
        DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(mockDynamo);

        dynamoDBMapper.save(TEST_OBJECT, DynamoDBMapperConfig.builder()
                                                             .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.PUT)
                                                             .build());

        verify(mockDynamo).putItem(putItemRequestCaptor.capture());
        PutItemRequest putItemRequest = putItemRequestCaptor.getValue();
        assertEquals(ITEM_MAP, putItemRequest.getItem());
    }

    @Test
    public void testSaveUsingUpdate() {
        when(mockDynamo.updateItem(any(UpdateItemRequest.class)))
            .thenReturn(new UpdateItemResult().withAttributes(ITEM_MAP));
        DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(mockDynamo);

        dynamoDBMapper.save(TEST_OBJECT, DynamoDBMapperConfig.builder()
                                                             .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.UPDATE)
                                                             .build());

        verify(mockDynamo).updateItem(updateItemRequestArgumentCaptor.capture());
        UpdateItemRequest updateItemRequest = updateItemRequestArgumentCaptor.getValue();
        assertEquals(KEY_MAP, updateItemRequest.getKey());
        Map<String, AttributeValueUpdate> updates = updateItemRequest.getAttributeUpdates();
        AttributeValueUpdate attributeValueUpdate = updates.get(BINARY_SET_ATTRIBUTE);
        assertEquals(EMPTY_BINARY_SET_AV, attributeValueUpdate.getValue());
        assertEquals("PUT", attributeValueUpdate.getAction());
    }
}
