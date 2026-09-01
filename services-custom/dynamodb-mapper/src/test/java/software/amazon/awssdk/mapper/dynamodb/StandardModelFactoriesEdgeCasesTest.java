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
import software.amazon.awssdk.mapper.dynamodb.internal.DynamoDBMapperModelFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.mapper.dynamodb.pojos.AutoKeyAndVal;
import software.amazon.awssdk.mapper.dynamodb.pojos.TestClass;

/**
 * Regression coverage for v1/v2 converter parity on edge inputs, beyond the single
 * representative value each type gets in the other {@code StandardModelFactories*} tests.
 */
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
    public void convert_withUnicodeString_preservesCharacters() {
        assertEquals("こんにちは", convert("getString", "こんにちは").s());
        assertEquals("emoji-😀", convert("getString", "emoji-😀").s());
    }

    @Test
    public void unconvert_withUnicodeString_preservesCharacters() {
        assertEquals("こんにちは",
                unconvert("getString", "setString", AttributeValue.builder().s("こんにちは").build()));
    }

    @Test
    public void convert_withEmptyString_returnsNull() {
        Assert.assertNull(convert("getString", ""));
    }

    @Test
    public void convert_withNumericBoundaries_producesExactStrings() {
        assertEquals(String.valueOf(Integer.MIN_VALUE), convert("getInt", Integer.MIN_VALUE).n());
        assertEquals(String.valueOf(Integer.MAX_VALUE), convert("getInt", Integer.MAX_VALUE).n());
        assertEquals(String.valueOf(Long.MIN_VALUE), convert("getLong", Long.MIN_VALUE).n());
        assertEquals(String.valueOf(Long.MAX_VALUE), convert("getLong", Long.MAX_VALUE).n());
    }

    @Test
    public void convert_withBigNumbers_preservesPrecision() {
        BigInteger big = new BigInteger("99999999999999999999");
        assertEquals("99999999999999999999", convert("getBigInt", big).n());

        BigDecimal dec = new BigDecimal("12345.6789");
        assertEquals("12345.6789", convert("getBigDecimal", dec).n());
    }

    @Test
    public void convert_withBigDecimal_preservesScale() {
        assertEquals("43.0", convert("getBigDecimal", new BigDecimal("43.0")).n());
    }

    @Test
    public void unconvert_withDecimalStringOnIntegralType_throwsNumberFormatException() {
        try {
            unconvert("getInt", "setInt", AttributeValue.builder().n("1.0").build());
            fail("Expected NumberFormatException for decimal string on integral type");
        } catch (NumberFormatException e) {
            // expected
        }
    }

    @Test
    public void convert_withNonFiniteDouble_producesExactStrings() {
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
    public void convert_withEnum_producesString() {
        DynamoDBMapperTableModel<EnumPojo> model = table(new EnumPojo());
        assertEquals("GREEN", model.field("val").convert(Color.GREEN).s());
    }

    @Test
    public void unconvert_withEnumString_producesEnum() {
        DynamoDBMapperTableModel<EnumPojo> model = table(new EnumPojo());
        assertEquals(Color.BLUE, model.field("val").unconvert(AttributeValue.builder().s("BLUE").build()));
    }

    @Test
    public void unconvert_withUnknownEnumValue_throwsException() {
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
    public void convert_withJsonPayload_producesString() {
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
    public void convertAndUnconvert_withJsonPayload_roundTrips() {
        JsonPayload payload = new JsonPayload();
        payload.setName("widget");
        payload.setCount(7);

        DynamoDBMapperTableModel<JsonPojo> model = table(new JsonPojo());
        AttributeValue av = model.field("val").convert(payload);
        Object back = model.field("val").unconvert(av);

        assertEquals(payload, back);
    }

    @Test
    public void unconvert_withByteBuffer_returnsWritableBuffer() {
        // v1 parity: v1 getB() returned a writable buffer. v2 SdkBytes.asByteBuffer() is
        // read-only, so the unmarshaller returns a writable copy for callers that mutate it.
        DynamoDBMapperTableModel<TestClass> model = v2Models.getTable(TestClass.class);
        AttributeValue stored = AttributeValue.builder().b(SdkBytes.fromUtf8String("data")).build();

        ByteBuffer result = (ByteBuffer) model.field("byteBuffer").unconvert(stored);

        Assert.assertFalse("returned buffer is writable (v1 parity)", result.isReadOnly());
        result.put(0, (byte) 'X');
        assertEquals((byte) 'X', result.get(0));
    }
}
