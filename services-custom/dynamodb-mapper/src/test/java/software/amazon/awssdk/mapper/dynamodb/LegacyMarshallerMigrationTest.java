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

import com.amazonaws.services.dynamodbv2.datamodeling.AbstractEnumMarshaller;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * Verifies the migration path from the removed v1 marshaller layer ({@code DynamoDBMarshaller},
 * {@code @DynamoDBMarshalling}, {@code AbstractEnumMarshaller}) to the converter layer.
 * <p>
 * Fixtures are produced by the real v1 marshallers ({@code com.amazonaws.services.dynamodbv2.datamodeling}), so the
 * tests check that items written by v1 load unchanged through the replacement annotations, and that items written
 * through the replacements are still readable by the v1 marshallers (for callers that roll back or run both versions
 * side by side). A generic converter also receives the target type that the legacy
 * {@code DynamoDBMarshaller.unmarshall(Class, String)} was passed.
 */
public class LegacyMarshallerMigrationTest extends LocalDynamoDBTestBase {

    private static final String TABLE_NAME =
        LegacyMarshallerMigrationTest.class.getSimpleName() + "-" + System.currentTimeMillis();

    private static final AbstractEnumMarshaller<Status> V1_ENUM = new AbstractEnumMarshaller<Status>() { };

    private DynamoDbClient ddb;
    private DynamoDBMapper mapper;

    @Before
    public void setup() {
        ddb = client();
        mapper = new DynamoDBMapper(ddb, DynamoDBMapperConfig.builder()
                .withTableNameOverride(new DynamoDBMapperConfig.TableNameOverride(TABLE_NAME))
                .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.CONSISTENT)
                .build());
        ddb.createTable(b -> b.tableName(TABLE_NAME)
                              .keySchema(KeySchemaElement.builder().attributeName("key").keyType(KeyType.HASH).build())
                              .attributeDefinitions(AttributeDefinition.builder()
                                                                       .attributeName("key")
                                                                       .attributeType(ScalarAttributeType.S)
                                                                       .build())
                              .provisionedThroughput(ProvisionedThroughput.builder()
                                                                          .readCapacityUnits(5L)
                                                                          .writeCapacityUnits(5L)
                                                                          .build()));
    }

    @After
    public void tearDown() {
        ddb.deleteTable(b -> b.tableName(TABLE_NAME));
    }

    @Test
    public void typeConvertedEnum_readsV1EnumMarshallerOutput() {
        String key = putRaw("status", s(V1_ENUM.marshall(Status.X)));

        assertEquals(Status.X, mapper.load(Item.class, key).getStatus());
    }

    @Test
    public void typeConvertedEnum_writesFormatV1EnumMarshallerReads() {
        Item item = newItem();
        item.setStatus(Status.Z);
        mapper.save(item);

        String stored = getRaw(item.getKey()).get("status").s();

        assertEquals(V1_ENUM.marshall(Status.Z), stored);
        assertEquals(Status.Z, V1_ENUM.unmarshall(Status.class, stored));
    }

    @Test
    public void genericConverterWithClassConstructor_readsV1EnumMarshallerOutput() {
        String key = putRaw("genericStatus", s(V1_ENUM.marshall(Status.Y)));

        Item loaded = mapper.load(Item.class, key);
        assertEquals(Status.Y, loaded.getGenericStatus());

        loaded.setGenericStatus(Status.X);
        mapper.save(loaded);
        String stored = getRaw(key).get("genericStatus").s();
        assertEquals(Status.X, V1_ENUM.unmarshall(Status.class, stored));
        assertEquals(Status.X, mapper.load(Item.class, key).getGenericStatus());
    }

    private static AttributeValue s(String value) {
        return AttributeValue.builder().s(value).build();
    }

    private static Item newItem() {
        Item item = new Item();
        item.setKey(UUID.randomUUID().toString());
        return item;
    }

    private String putRaw(String attribute, AttributeValue value) {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put(attribute, value);
        return putRaw(item);
    }

    private String putRaw(Map<String, AttributeValue> attributes) {
        String key = UUID.randomUUID().toString();
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>(attributes);
        item.put("key", s(key));
        ddb.putItem(b -> b.tableName(TABLE_NAME).item(item));
        return key;
    }

    private Map<String, AttributeValue> getRaw(String key) {
        Map<String, AttributeValue> itemKey = new HashMap<String, AttributeValue>();
        itemKey.put("key", s(key));
        return ddb.getItem(b -> b.tableName(TABLE_NAME).key(itemKey).consistentRead(true)).item();
    }

    public enum Status { X, Y, Z }

    /**
     * A generic enum converter. The mapper passes the property's target type to the single-{@link Class}
     * constructor, replacing the {@code Class} argument of the legacy {@code DynamoDBMarshaller.unmarshall}.
     */
    public static final class EnumConverter<E extends Enum<E>> implements DynamoDBTypeConverter<String, E> {
        private final Class<E> targetType;

        public EnumConverter(Class<E> targetType) {
            this.targetType = targetType;
        }

        @Override
        public String convert(E object) {
            return object.name();
        }

        @Override
        public E unconvert(String object) {
            return Enum.valueOf(targetType, object);
        }
    }

    @DynamoDBTable(tableName = "overridden")
    public static class Item {
        private String key;
        private Status status;
        private Status genericStatus;

        @DynamoDBHashKey
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @DynamoDBTypeConvertedEnum
        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        @DynamoDBTypeConverted(converter = EnumConverter.class)
        public Status getGenericStatus() {
            return genericStatus;
        }

        public void setGenericStatus(Status genericStatus) {
            this.genericStatus = genericStatus;
        }
    }
}
