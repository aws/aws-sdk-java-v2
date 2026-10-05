/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.mapper.dynamodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.ConsistentReads;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.SaveBehavior;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.TableNameOverride;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

/**
 * Verifies that {@link DynamoDBMapper#builder()} wires the client, mapper config, and attribute transformer the same
 * way the equivalent constructors do, by observing the requests the mapper sends.
 */
public class DynamoDBMapperBuilderTest {

    private static final String HASH_KEY = "1234";

    private DynamoDbClient ddb;

    @Before
    public void setup() {
        ddb = mock(DynamoDbClient.class);
        when(ddb.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().item(storedItem()).build());
        when(ddb.updateItem(any(UpdateItemRequest.class))).thenReturn(UpdateItemResponse.builder().build());
        when(ddb.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());
    }

    @Test
    public void build_withOnlyClient_usesDefaultConfig() {
        DynamoDBMapper mapper = DynamoDBMapper.builder().dynamoDbClient(ddb).build();

        StringItem loaded = mapper.load(StringItem.class, HASH_KEY);

        GetItemRequest request = capturedGetItem();
        assertEquals("M_String", request.tableName());
        assertFalse(request.consistentRead());
        assertEquals("stored", loaded.getValue());
    }

    @Test
    public void build_withMapperConfig_appliesConfigToOperations() {
        DynamoDBMapperConfig config = DynamoDBMapperConfig.builder()
                                                          .withTableNameOverride(TableNameOverride.withTableNamePrefix("dev_"))
                                                          .withConsistentReads(ConsistentReads.CONSISTENT)
                                                          .build();
        DynamoDBMapper mapper = DynamoDBMapper.builder().dynamoDbClient(ddb).mapperConfig(config).build();

        mapper.load(StringItem.class, HASH_KEY);

        GetItemRequest request = capturedGetItem();
        assertEquals("dev_M_String", request.tableName());
        assertTrue(request.consistentRead());
    }

    @Test
    public void build_withPartialMapperConfig_fallsBackToDefaultsForUnsetValues() {
        DynamoDBMapper mapper = DynamoDBMapper.builder()
                                              .dynamoDbClient(ddb)
                                              .mapperConfig(DynamoDBMapperConfig.builder()
                                                                                .withConsistentReads(ConsistentReads.CONSISTENT)
                                                                                .build())
                                              .build();

        mapper.save(item("new"));

        // SaveBehavior was not set, so the DEFAULT (UPDATE) applies and the write is an UpdateItem.
        verify(ddb).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    public void build_withMapperConfigConsumer_appliesConfigToOperations() {
        DynamoDBMapper mapper = DynamoDBMapper.builder()
                                              .dynamoDbClient(ddb)
                                              .mapperConfig(c -> c.withTableNameOverride(
                                                                      TableNameOverride.withTableNameReplacement("Replaced"))
                                                                  .withConsistentReads(ConsistentReads.CONSISTENT))
                                              .build();

        mapper.load(StringItem.class, HASH_KEY);

        GetItemRequest request = capturedGetItem();
        assertEquals("Replaced", request.tableName());
        assertTrue(request.consistentRead());
    }

    @Test
    public void build_withAttributeTransformer_untransformsOnRead() {
        DynamoDBMapper mapper = DynamoDBMapper.builder()
                                              .dynamoDbClient(ddb)
                                              .attributeTransformer(new ValueRewritingTransformer())
                                              .build();

        StringItem loaded = mapper.load(StringItem.class, HASH_KEY);

        assertEquals("untransformed", loaded.getValue());
    }

    @Test
    public void build_withAttributeTransformer_transformsOnWrite() {
        DynamoDBMapper mapper = DynamoDBMapper.builder()
                                              .dynamoDbClient(ddb)
                                              .mapperConfig(SaveBehavior.CLOBBER.config())
                                              .attributeTransformer(new ValueRewritingTransformer())
                                              .build();

        mapper.save(item("new"));

        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(ddb).putItem(captor.capture());
        assertEquals("transformed", captor.getValue().item().get("value").s());
    }

    @Test
    public void build_withLaterSetterCall_lastValueWins() {
        DynamoDBMapper mapper = DynamoDBMapper.builder()
                                              .dynamoDbClient(ddb)
                                              .mapperConfig(ConsistentReads.CONSISTENT.config())
                                              .mapperConfig(ConsistentReads.EVENTUAL.config())
                                              .build();

        mapper.load(StringItem.class, HASH_KEY);

        assertFalse(capturedGetItem().consistentRead());
    }

    @Test
    public void build_withoutClient_throwsNullPointerException() {
        try {
            DynamoDBMapper.builder().mapperConfig(DynamoDBMapperConfig.DEFAULT).build();
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("dynamoDbClient"));
        }
    }

    private GetItemRequest capturedGetItem() {
        ArgumentCaptor<GetItemRequest> captor = ArgumentCaptor.forClass(GetItemRequest.class);
        verify(ddb).getItem(captor.capture());
        return captor.getValue();
    }

    private static StringItem item(String value) {
        StringItem item = new StringItem();
        item.setId(HASH_KEY);
        item.setValue(value);
        return item;
    }

    private static Map<String, AttributeValue> storedItem() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(HASH_KEY).build());
        item.put("value", AttributeValue.builder().s("stored").build());
        return item;
    }

    @DynamoDBTable(tableName = "M_String")
    public static class StringItem {
        private String id;
        private String value;

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDBAttribute
        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /** Overwrites the "value" attribute so each direction of the transformer is observable. */
    private static final class ValueRewritingTransformer implements AttributeTransformer {
        @Override
        public Map<String, AttributeValue> transform(Parameters<?> parameters) {
            return withValue(parameters.getAttributeValues(), "transformed");
        }

        @Override
        public Map<String, AttributeValue> untransform(Parameters<?> parameters) {
            return withValue(parameters.getAttributeValues(), "untransformed");
        }

        private static Map<String, AttributeValue> withValue(Map<String, AttributeValue> attributes, String value) {
            Map<String, AttributeValue> copy = new HashMap<>(attributes);
            copy.put("value", AttributeValue.builder().s(value).build());
            return copy;
        }
    }
}
