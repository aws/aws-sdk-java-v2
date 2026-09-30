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
import com.amazonaws.services.dynamodbv2.datamodeling.JsonMarshaller;
import java.util.ArrayList;
import java.util.Date;
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
 * {@code @DynamoDBMarshalling}, {@code AbstractEnumMarshaller}, {@code JsonMarshaller}) to the converter layer.
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

    // A fixed instant with non-zero milliseconds, to catch any precision loss in the JSON date encoding.
    private static final Date UPDATED = new Date(1_700_000_000_123L);

    private static final AbstractEnumMarshaller<Status> V1_ENUM = new AbstractEnumMarshaller<Status>() { };
    private static final JsonMarshaller<Part> V1_PART_JSON = new JsonMarshaller<Part>(Part.class);
    private static final JsonMarshaller<PartList> V1_PART_LIST_JSON = new JsonMarshaller<PartList>(PartList.class);

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

    @Test
    public void typeConvertedJson_readsV1JsonMarshallerOutput() {
        PartList parts = new PartList();
        parts.add(new Part("b", 1, UPDATED));
        parts.add(new Part("c", 2, null));
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("part", s(V1_PART_JSON.marshall(new Part("a", 3, UPDATED))));
        item.put("parts", s(V1_PART_LIST_JSON.marshall(parts)));
        String key = putRaw(item);

        Item loaded = mapper.load(Item.class, key);

        assertPart(loaded.getPart(), "a", 3, UPDATED);
        assertEquals(PartList.class, loaded.getParts().getClass());
        assertEquals(2, loaded.getParts().size());
        assertPart(loaded.getParts().get(0), "b", 1, UPDATED);
        assertPart(loaded.getParts().get(1), "c", 2, null);
    }

    @Test
    public void typeConvertedJson_writesFormatV1JsonMarshallerReads() {
        Item item = newItem();
        item.setPart(new Part("a", 3, UPDATED));
        List<Part> parts = new ArrayList<Part>();
        parts.add(new Part("b", 1, UPDATED));
        item.setParts(parts);
        mapper.save(item);

        Map<String, AttributeValue> stored = getRaw(item.getKey());

        assertPart(V1_PART_JSON.unmarshall(Part.class, stored.get("part").s()), "a", 3, UPDATED);
        PartList v1Parts = V1_PART_LIST_JSON.unmarshall(PartList.class, stored.get("parts").s());
        assertEquals(1, v1Parts.size());
        assertPart(v1Parts.get(0), "b", 1, UPDATED);
    }

    @Test
    public void typeConvertedJson_roundTrips() {
        Item item = newItem();
        item.setPart(new Part("a", 3, UPDATED));
        List<Part> parts = new ArrayList<Part>();
        parts.add(new Part("b", 1, null));
        item.setParts(parts);

        mapper.save(item);
        Item loaded = mapper.load(Item.class, item.getKey());

        assertPart(loaded.getPart(), "a", 3, UPDATED);
        assertEquals(1, loaded.getParts().size());
        assertPart(loaded.getParts().get(0), "b", 1, null);
    }

    private static void assertPart(Part part, String id, int quantity, Date updated) {
        assertEquals(id, part.getId());
        assertEquals(Integer.valueOf(quantity), part.getQuantity());
        assertEquals(updated, part.getUpdated());
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

    /**
     * Concrete list type that keeps the element type through erasure, as a legacy {@code JsonMarshaller} subclass
     * would have with {@code super(Type.class)}.
     */
    public static final class PartList extends ArrayList<Part> {
    }

    public static class Part {
        private String id;
        private Integer quantity;
        private Date updated;

        public Part() {
        }

        public Part(String id, Integer quantity, Date updated) {
            this.id = id;
            this.quantity = quantity;
            this.updated = updated;
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

        public Date getUpdated() {
            return updated;
        }

        public void setUpdated(Date updated) {
            this.updated = updated;
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
