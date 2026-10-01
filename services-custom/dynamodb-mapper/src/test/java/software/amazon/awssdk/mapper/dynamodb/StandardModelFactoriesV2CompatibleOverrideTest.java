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

import java.lang.reflect.Method;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperFieldModel.DynamoDBAttributeType;
import software.amazon.awssdk.mapper.dynamodb.internal.DynamoDBMapperModelFactory;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Reruns the V2_COMPATIBLE suite through a custom {@link ConversionSchema} built with
 * {@link ConversionSchemas#v2CompatibleBuilder(String)}. A custom schema converts fields that have no type converter
 * through its {@link ItemConverter} rather than the standard rules, so this covers annotation handling on that path
 * (for example {@code @DynamoDBTyped(BOOL)}, which V2_COMPATIBLE does not otherwise honor by type).
 */
public class StandardModelFactoriesV2CompatibleOverrideTest extends StandardModelFactoriesV2CompatibleTest {

    private final DynamoDBMapperConfig config = new DynamoDBMapperConfig.Builder()
        .withTypeConverterFactory(DynamoDBMapperConfig.DEFAULT.getTypeConverterFactory())
        .withConversionSchema(ConversionSchemas.v2CompatibleBuilder("V2CompatibleOverride").build())
        .build();

    private final DynamoDBMapperModelFactory factory = StandardModelFactories.of(S3Link.Factory.of(null));
    private final DynamoDBMapperModelFactory.TableFactory models = factory.getTableFactory(config);

    @Override
    protected <T> AttributeValue convert(Class<T> clazz, Method getter, Object value) {
        final StandardAnnotationMaps.FieldMap<Object> map = StandardAnnotationMaps.of(getter, null);
        return models.getTable(clazz).field(map.attributeName()).convert(value);
    }

    @Test
    public void typedBool_onBoxedBoolean_writesNativeBool() {
        DynamoDBMapperTableModel<TypedBoolItem> table = models.getTable(TypedBoolItem.class);
        assertEquals(true, table.field("boxedBool").convert(Boolean.TRUE).bool());
    }

    /**
     * {@code @DynamoDBTyped(BOOL)} forces native BOOL only for boolean properties. Other types keep the marshaller
     * chosen by Java type, as before {@code @DynamoDBTyped(BOOL)} was honored on this path.
     */
    @Test
    public void typedBool_onNonBooleanProperty_keepsTypeBasedMarshaller() {
        DynamoDBMapperTableModel<TypedBoolItem> table = models.getTable(TypedBoolItem.class);
        assertEquals("abc", table.field("notABool").convert("abc").s());
    }

    /**
     * A flattened property's getter belongs to the flattened type while {@code DeclaringReflect} reads and writes it
     * through the owning object; the item converter must still resolve that getter and its setter.
     */
    @Test
    public void flattenedProperty_roundTripsThroughItemConverter() {
        DynamoDBMapperTableModel<FlattenedItem> table = models.getTable(FlattenedItem.class);
        FlattenedItem item = new FlattenedItem();
        item.setId("id");
        item.setFlags(new Flags());
        item.getFlags().setTyped(true);
        item.getFlags().setPlain(true);
        item.getFlags().setLabel("label");

        Map<String, AttributeValue> converted = table.convert(item);

        assertEquals(true, converted.get("typedFlag").bool());
        assertEquals("1", converted.get("plainFlag").n());
        assertEquals("label", converted.get("labelAttr").s());

        FlattenedItem unconverted = table.unconvert(converted);

        assertEquals(Boolean.TRUE, unconverted.getFlags().getTyped());
        assertEquals(Boolean.TRUE, unconverted.getFlags().getPlain());
        assertEquals("label", unconverted.getFlags().getLabel());
    }

    /**
     * Members of a {@link DynamoDBDocument} class are converted by the item converter itself (not the standard rules),
     * so {@code @DynamoDBTyped(BOOL)} must be honored there too.
     */
    @Test
    public void nestedDocumentMember_typedBool_roundTripsThroughItemConverter() {
        DynamoDBMapperTableModel<DocumentItem> table = models.getTable(DocumentItem.class);
        Flags flags = new Flags();
        flags.setTyped(true);
        flags.setPlain(true);
        flags.setLabel("label");

        AttributeValue converted = table.<Flags>field("flags").convert(flags);

        assertEquals(true, converted.m().get("typed").bool());
        assertEquals("1", converted.m().get("plain").n());
        assertEquals("label", converted.m().get("label").s());

        Flags unconverted = table.<Flags>field("flags").unconvert(converted);

        assertEquals(Boolean.TRUE, unconverted.getTyped());
        assertEquals(Boolean.TRUE, unconverted.getPlain());
        assertEquals("label", unconverted.getLabel());
    }

    @DynamoDBDocument
    public static class Flags {
        private Boolean typed;
        private Boolean plain;
        private String label;

        @DynamoDBTyped(DynamoDBAttributeType.BOOL)
        public Boolean getTyped() {
            return typed;
        }

        public void setTyped(Boolean typed) {
            this.typed = typed;
        }

        public Boolean getPlain() {
            return plain;
        }

        public void setPlain(Boolean plain) {
            this.plain = plain;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    @DynamoDBTable(tableName = "nonexisting-test-tablename")
    public static class FlattenedItem {
        private String id;
        private Flags flags;

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDBFlattened(attributes = {
            @DynamoDBAttribute(mappedBy = "typed", attributeName = "typedFlag"),
            @DynamoDBAttribute(mappedBy = "plain", attributeName = "plainFlag"),
            @DynamoDBAttribute(mappedBy = "label", attributeName = "labelAttr")})
        public Flags getFlags() {
            return flags;
        }

        public void setFlags(Flags flags) {
            this.flags = flags;
        }
    }

    @DynamoDBTable(tableName = "nonexisting-test-tablename")
    public static class DocumentItem {
        private String id;
        private Flags flags;

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Flags getFlags() {
            return flags;
        }

        public void setFlags(Flags flags) {
            this.flags = flags;
        }
    }

    @DynamoDBTable(tableName = "nonexisting-test-tablename")
    public static class TypedBoolItem {
        private String id;
        private Boolean boxedBool;
        private String notABool;

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDBTyped(DynamoDBAttributeType.BOOL)
        public Boolean getBoxedBool() {
            return boxedBool;
        }

        public void setBoxedBool(Boolean boxedBool) {
            this.boxedBool = boxedBool;
        }

        @DynamoDBTyped(DynamoDBAttributeType.BOOL)
        public String getNotABool() {
            return notABool;
        }

        public void setNotABool(String notABool) {
            this.notABool = notABool;
        }
    }

}
