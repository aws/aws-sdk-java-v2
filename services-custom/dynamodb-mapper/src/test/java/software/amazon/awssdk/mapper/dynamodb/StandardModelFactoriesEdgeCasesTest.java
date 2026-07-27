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
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.mapper.dynamodb.pojos.AutoKeyAndVal;
import software.amazon.awssdk.mapper.dynamodb.pojos.TestClass;

public class StandardModelFactoriesEdgeCasesTest {

    private static final DynamoDBMapperConfig V1_CONFIG = new DynamoDBMapperConfig.Builder()
        .withTypeConverterFactory(DynamoDBMapperConfig.DEFAULT.getTypeConverterFactory())
        .withConversionSchema(ConversionSchemas.V1)
        .build();

    private static final DynamoDBMapperConfig V2_CONFIG = new DynamoDBMapperConfig.Builder()
        .withTypeConverterFactory(DynamoDBMapperConfig.DEFAULT.getTypeConverterFactory())
        .withConversionSchema(ConversionSchemas.V2)
        .build();

    private static final DynamoDBMapperModelFactory factory = StandardModelFactories.of(S3Link.Factory.of(null));
    private static final DynamoDBMapperModelFactory.TableFactory v1Models = factory.getTableFactory(V1_CONFIG);
    private static final DynamoDBMapperModelFactory.TableFactory v2Models = factory.getTableFactory(V2_CONFIG);

    private AttributeValue convert(String getter, Object value) {
        try {
            Method gm = TestClass.class.getMethod(getter);
            StandardAnnotationMaps.FieldMap<Object> map = StandardAnnotationMaps.of(gm, null);
            return v1Models.getTable(TestClass.class).field(map.attributeName()).convert(value);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object unconvert(String getter, String setter, AttributeValue value) {
        try {
            Method gm = TestClass.class.getMethod(getter);
            Method sm = TestClass.class.getMethod(setter, gm.getReturnType());
            StandardAnnotationMaps.FieldMap<Object> map = StandardAnnotationMaps.of(gm, null);
            return v2Models.getTable(TestClass.class).field(map.attributeName()).unconvert(value);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> DynamoDBMapperTableModel<T> table(T object) {
        return v2Models.getTable((Class<T>) object.getClass());
    }

    @Test
    public void convert_unicodeString_preserved() {
        assertEquals("こんにちは", convert("getString", "こんにちは").s());
        assertEquals("emoji-😀", convert("getString", "emoji-😀").s());
    }

    @Test
    public void unconvert_unicodeString_preserved() {
        assertEquals("こんにちは",
                unconvert("getString", "setString", AttributeValue.builder().s("こんにちは").build()));
    }

    @Test
    public void convert_emptyString_isDropped() {
        Assert.assertNull(convert("getString", ""));
    }

    @Test
    public void convert_numericBoundaries_exactStrings() {
        assertEquals(String.valueOf(Integer.MIN_VALUE), convert("getInt", Integer.MIN_VALUE).n());
        assertEquals(String.valueOf(Integer.MAX_VALUE), convert("getInt", Integer.MAX_VALUE).n());
        assertEquals(String.valueOf(Long.MIN_VALUE), convert("getLong", Long.MIN_VALUE).n());
        assertEquals(String.valueOf(Long.MAX_VALUE), convert("getLong", Long.MAX_VALUE).n());
    }

    @Test
    public void convert_bigNumbers_precisionPreserved() {
        BigInteger big = new BigInteger("99999999999999999999");
        assertEquals("99999999999999999999", convert("getBigInt", big).n());

        BigDecimal dec = new BigDecimal("12345.6789");
        assertEquals("12345.6789", convert("getBigDecimal", dec).n());
    }

    @Test
    public void convert_bigDecimal_scalePreserved() {
        assertEquals("43.0", convert("getBigDecimal", new BigDecimal("43.0")).n());
    }

    @Test
    public void unconvert_integralType_fromDecimalString_throws() {
        try {
            unconvert("getInt", "setInt", AttributeValue.builder().n("1.0").build());
            fail("Expected NumberFormatException for decimal string on integral type");
        } catch (NumberFormatException e) {
            // expected
        }
    }

    @Test
    public void convert_nonFiniteDouble() {
        assertEquals("NaN", convert("getDouble", Double.NaN).n());
        assertEquals("Infinity", convert("getDouble", Double.POSITIVE_INFINITY).n());
        assertEquals("-Infinity", convert("getDouble", Double.NEGATIVE_INFINITY).n());
    }

    public enum Color { RED, GREEN, BLUE }

    public static class EnumPojo extends AutoKeyAndVal<Color> {
        @DynamoDBTypeConvertedEnum
        public Color getVal() { return super.getVal(); }
        public void setVal(Color val) { super.setVal(val); }
    }

    @Test
    public void convert_enum_asString() {
        DynamoDBMapperTableModel<EnumPojo> model = table(new EnumPojo());
        assertEquals("GREEN", model.field("val").convert(Color.GREEN).s());
    }

    @Test
    public void unconvert_enum_fromString() {
        DynamoDBMapperTableModel<EnumPojo> model = table(new EnumPojo());
        assertEquals(Color.BLUE, model.field("val").unconvert(AttributeValue.builder().s("BLUE").build()));
    }

    @Test
    public void unconvert_enum_unknownValue_throws() {
        DynamoDBMapperTableModel<EnumPojo> model = table(new EnumPojo());
        try {
            model.field("val").unconvert(AttributeValue.builder().s("PURPLE").build());
            fail("Expected exception for unknown enum constant");
        } catch (RuntimeException e) {
            // expected
        }
    }

    public static class JsonPayload {
        private String name;
        private int count;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof JsonPayload)) {
                return false;
            }
            JsonPayload that = (JsonPayload) o;
            return count == that.count
                   && (name == null ? that.name == null : name.equals(that.name));
        }
        @Override
        public int hashCode() {
            return (name == null ? 0 : name.hashCode()) * 31 + count;
        }
    }

    public static class JsonPojo extends AutoKeyAndVal<JsonPayload> {
        @DynamoDBTypeConvertedJson
        public JsonPayload getVal() { return super.getVal(); }
        public void setVal(JsonPayload val) { super.setVal(val); }
    }

    @Test
    public void convert_json_asString() {
        JsonPayload payload = new JsonPayload();
        payload.setName("widget");
        payload.setCount(7);

        DynamoDBMapperTableModel<JsonPojo> model = table(new JsonPojo());
        AttributeValue av = model.field("val").convert(payload);

        Assert.assertNotNull(av.s());
        Assert.assertTrue(av.s().contains("widget"));
        Assert.assertTrue(av.s().contains("7"));
    }

    @Test
    public void json_roundTrip() {
        JsonPayload payload = new JsonPayload();
        payload.setName("widget");
        payload.setCount(7);

        DynamoDBMapperTableModel<JsonPojo> model = table(new JsonPojo());
        AttributeValue av = model.field("val").convert(payload);
        Object back = model.field("val").unconvert(av);

        assertEquals(payload, back);
    }
}
