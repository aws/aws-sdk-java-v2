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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * {@code @DynamoDBMarshalling}, {@code AbstractEnumMarshaller}, {@code JsonMarshaller}) to the converter layer:
 * items written by the legacy marshallers load unchanged through the replacement annotations, and a generic converter
 * receives the target type that the legacy {@code DynamoDBMarshaller.unmarshall(Class, String)} was passed.
 */
public class LegacyMarshallerMigrationTest extends LocalDynamoDBTestBase {

    private static final String TABLE_NAME =
        LegacyMarshallerMigrationTest.class.getSimpleName() + "-" + System.currentTimeMillis();

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
    public void typeConvertedEnum_readsAndWritesLegacyEnumMarshallerFormat() {
        String key = putRaw("status", AttributeValue.builder().s("X").build());

        Item loaded = mapper.load(Item.class, key);
        assertEquals(Status.X, loaded.getStatus());

        loaded.setStatus(Status.Z);
        mapper.save(loaded);
        assertEquals("Z", getRaw(key).get("status").s());
    }

    @Test
    public void genericConverterWithClassConstructor_receivesTargetType() {
        String key = putRaw("genericStatus", AttributeValue.builder().s("Y").build());

        Item loaded = mapper.load(Item.class, key);
        assertEquals(Status.Y, loaded.getGenericStatus());

        loaded.setGenericStatus(Status.X);
        mapper.save(loaded);
        assertEquals("X", getRaw(key).get("genericStatus").s());
        assertEquals(Status.X, mapper.load(Item.class, key).getGenericStatus());
    }

    @Test
    public void typeConvertedJson_readsLegacyJsonMarshallerFormat() {
        // Default Jackson output, which is what the legacy JsonMarshaller wrote.
        String key = putRaw("part", AttributeValue.builder().s("{\"id\":\"a\",\"quantity\":3}").build());
        Map<String, AttributeValue> update = new HashMap<String, AttributeValue>(getRaw(key));
        update.put("parts", AttributeValue.builder().s("[{\"id\":\"b\",\"quantity\":1},{\"id\":\"c\",\"quantity\":2}]")
                                          .build());
        ddb.putItem(b -> b.tableName(TABLE_NAME).item(update));

        Item loaded = mapper.load(Item.class, key);

        assertEquals("a", loaded.getPart().getId());
        assertEquals(Integer.valueOf(3), loaded.getPart().getQuantity());
        assertEquals(2, loaded.getParts().size());
        assertEquals(PartList.class, loaded.getParts().getClass());
        assertEquals("c", loaded.getParts().get(1).getId());
        assertEquals(Integer.valueOf(2), loaded.getParts().get(1).getQuantity());
    }

    @Test
    public void typeConvertedJson_roundTrips() {
        Item item = new Item();
        item.setKey(UUID.randomUUID().toString());
        item.setPart(new Part("a", 3));
        List<Part> parts = new ArrayList<Part>();
        parts.add(new Part("b", 1));
        item.setParts(parts);

        mapper.save(item);
        Item loaded = mapper.load(Item.class, item.getKey());

        assertEquals("a", loaded.getPart().getId());
        assertEquals(1, loaded.getParts().size());
        assertEquals("b", loaded.getParts().get(0).getId());
    }

    private String putRaw(String attribute, AttributeValue value) {
        String key = UUID.randomUUID().toString();
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("key", AttributeValue.builder().s(key).build());
        item.put(attribute, value);
        ddb.putItem(b -> b.tableName(TABLE_NAME).item(item));
        return key;
    }

    private Map<String, AttributeValue> getRaw(String key) {
        Map<String, AttributeValue> itemKey = new HashMap<String, AttributeValue>();
        itemKey.put("key", AttributeValue.builder().s(key).build());
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

    /**
     * Concrete list type that keeps the element type through erasure, as a legacy {@code JsonMarshaller} subclass
     * would have with {@code super(Type.class)}.
     */
    public static final class PartList extends ArrayList<Part> {
    }

    public static class Part {
        private String id;
        private Integer quantity;

        public Part() {
        }

        public Part(String id, Integer quantity) {
            this.id = id;
            this.quantity = quantity;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    @DynamoDBTable(tableName = "overridden")
    public static class Item {
        private String key;
        private Status status;
        private Status genericStatus;
        private Part part;
        private List<Part> parts;

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

        @DynamoDBTypeConvertedJson
        public Part getPart() {
            return part;
        }

        public void setPart(Part part) {
            this.part = part;
        }

        @DynamoDBTypeConvertedJson(targetType = PartList.class)
        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }
}
