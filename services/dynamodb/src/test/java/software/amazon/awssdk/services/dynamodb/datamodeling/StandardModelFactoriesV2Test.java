/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.pojos.SubClass;
import software.amazon.awssdk.services.dynamodb.pojos.TestClass;
import software.amazon.awssdk.services.dynamodb.pojos.UnannotatedSubClass;

public class StandardModelFactoriesV2Test {

    protected static final DynamoDbMapperConfig CONFIG = new DynamoDbMapperConfig.Builder()
            .withTypeConverterFactory(DynamoDbMapperConfig.DEFAULT.getTypeConverterFactory())
            .withConversionSchema(ConversionSchemas.V2)
            .build();

    private static final DynamoDbMapperModelFactory factory = StandardModelFactories.of(S3Link.Factory.of(null));
    private static final DynamoDbMapperModelFactory.TableFactory models = factory.getTableFactory(CONFIG);

    protected <T> AttributeValue convert(Class<T> clazz, Method getter, Object value) {
        final StandardAnnotationMaps.FieldMap<Object> map = StandardAnnotationMaps.of(getter, null);
        return models.getTable(clazz).field(map.attributeName()).convert(value);
    }

    @Test
    public void testBoolean() {
        // These are all native booleans by default in the v2 schema
        assertEquals(true, convert("getBoolean", true).bool());
        assertEquals(false, convert("getBoolean", false).bool());
        assertEquals(true, convert("getBoxedBoolean", true).bool());
        assertEquals(false, convert("getBoxedBoolean", false).bool());
        assertEquals(true, convert("getNativeBoolean", true).bool());
        assertEquals(false, convert("getNativeBoolean", false).bool());
    }

    @Test
    public void testString() {
        assertEquals("abc", convert("getString", "abc").s());

        assertEquals(RandomUuidMarshaller.randomUUID,
                     convert("getCustomString", "abc").s());
    }

    @Test
    public void testUuid() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid.toString(), convert("getUuid", uuid).s());
    }

    @Test
    public void testDate() {
        assertEquals("1970-01-01T00:00:00Z",
                     convert("getDate", new Date(0)).s());

        Calendar c = GregorianCalendar.getInstance();
        c.setTimeInMillis(0);

        assertEquals("1970-01-01T00:00:00Z",
                     convert("getCalendar", c).s());
    }

    @Test
    public void testNumbers() {
        assertEquals("0", convert("getByte", (byte) 0).n());
        assertEquals("1", convert("getByte", (byte) 1).n());
        assertEquals("0", convert("getBoxedByte", (byte) 0).n());
        assertEquals("1", convert("getBoxedByte", (byte) 1).n());

        assertEquals("0", convert("getShort", (short) 0).n());
        assertEquals("1", convert("getShort", (short) 1).n());
        assertEquals("0", convert("getBoxedShort", (short) 0).n());
        assertEquals("1", convert("getBoxedShort", (short) 1).n());

        assertEquals("0", convert("getInt", 0).n());
        assertEquals("1", convert("getInt", 1).n());
        assertEquals("0", convert("getBoxedInt", 0).n());
        assertEquals("1", convert("getBoxedInt", 1).n());

        assertEquals("0", convert("getLong", 0l).n());
        assertEquals("1", convert("getLong", 1l).n());
        assertEquals("0", convert("getBoxedLong", 0l).n());
        assertEquals("1", convert("getBoxedLong", 1l).n());

        assertEquals("0", convert("getBigInt", BigInteger.ZERO).n());
        assertEquals("1", convert("getBigInt", BigInteger.ONE).n());

        assertEquals("0.0", convert("getFloat", 0f).n());
        assertEquals("1.0", convert("getFloat", 1f).n());
        assertEquals("0.0", convert("getBoxedFloat", 0f).n());
        assertEquals("1.0", convert("getBoxedFloat", 1f).n());

        assertEquals("0.0", convert("getDouble", 0d).n());
        assertEquals("1.0", convert("getDouble", 1d).n());
        assertEquals("0.0", convert("getBoxedDouble", 0d).n());
        assertEquals("1.0", convert("getBoxedDouble", 1d).n());

        assertEquals("0", convert("getBigDecimal", BigDecimal.ZERO).n());
        assertEquals("1", convert("getBigDecimal", BigDecimal.ONE).n());
    }

    @Test
    public void testBinary() {
        ByteBuffer value = ByteBuffer.wrap("value".getBytes());
        assertEquals(value.slice(), convert("getByteArray", "value".getBytes()).b());
        assertEquals(value.slice(), convert("getByteBuffer", value.slice()).b());
    }

    @Test
    public void testBooleanSet() {
        // Set<Boolean> (which is silly but technically valid) gets mapped to
        // a List of Booleans now via the ObjectSetToListMarshaller.
        AttributeValue value =
                convert("getBooleanSet", Collections.singleton(true));

        Assert.assertEquals(1, value.l().size());
        Assert.assertEquals(true, value.l().get(0).bool());
    }

    @Test
    public void testStringSet() {
        assertEquals(Collections.singletonList("a"),
                     convert("getStringSet", Collections.singleton("a")).ss());
        assertEquals(Collections.singletonList("b"),
                     convert("getStringSet", Collections.singleton("b")).ss());

        assertEquals(Arrays.asList("a", "b", "c"),
                     convert("getStringSet", new TreeSet<String>() {{
                         add("a");
                         add("b");
                         add("c");
                     }}).ss());
    }

    @Test
    public void testUuidSet() {
        final UUID one = UUID.randomUUID();
        final UUID two = UUID.randomUUID();
        final UUID three = UUID.randomUUID();

        assertEquals(Collections.singletonList(one.toString()),
                     convert("getUuidSet", Collections.singleton(one)).ss());

        assertEquals(Collections.singletonList(two.toString()),
                     convert("getUuidSet", Collections.singleton(two)).ss());

        assertEquals(
                Arrays.asList(
                        one.toString(),
                        two.toString(),
                        three.toString()),
                convert("getUuidSet", new LinkedHashSet<UUID>() {{
                    add(one);
                    add(two);
                    add(three);
                }}).ss());
    }

    @Test
    public void testDateSet() {
        assertEquals(Collections.singletonList("1970-01-01T00:00:00Z"),
                     convert("getDateSet", Collections.singleton(new Date(0)))
                             .ss());

        Calendar c = GregorianCalendar.getInstance();
        c.setTimeInMillis(0);

        assertEquals(Collections.singletonList("1970-01-01T00:00:00Z"),
                     convert("getCalendarSet", Collections.singleton(c))
                             .ss());
    }

    @Test
    public void testNumberSet() {
        assertEquals(Collections.singletonList("0"),
                     convert("getByteSet", Collections.singleton((byte) 0)).ns());
        assertEquals(Collections.singletonList("0"),
                     convert("getShortSet", Collections.singleton((short) 0)).ns());
        assertEquals(Collections.singletonList("0"),
                     convert("getIntSet", Collections.singleton(0)).ns());
        assertEquals(Collections.singletonList("0"),
                     convert("getLongSet", Collections.singleton(0l)).ns());
        assertEquals(Collections.singletonList("0"),
                     convert("getBigIntegerSet", Collections.singleton(BigInteger.ZERO))
                             .ns());
        assertEquals(Collections.singletonList("0.0"),
                     convert("getFloatSet", Collections.singleton(0f)).ns());
        assertEquals(Collections.singletonList("0.0"),
                     convert("getDoubleSet", Collections.singleton(0d)).ns());
        assertEquals(Collections.singletonList("0"),
                     convert("getBigDecimalSet", Collections.singleton(BigDecimal.ZERO))
                             .ns());

        assertEquals(Arrays.asList("0", "1", "2"),
                     convert("getLongSet", new TreeSet<Number>() {{
                         add(0);
                         add(1);
                         add(2);
                     }}).ns());
    }

    @Test
    public void testBinarySet() {
        final ByteBuffer test = ByteBuffer.wrap("test".getBytes());
        final ByteBuffer test2 = ByteBuffer.wrap("test2".getBytes());

        assertEquals(Collections.singletonList(test.slice()),
                     convert("getByteArraySet", Collections.singleton("test".getBytes()))
                             .bs());

        assertEquals(Collections.singletonList(test.slice()),
                     convert("getByteBufferSet", Collections.singleton(test.slice()))
                             .bs());

        assertEquals(Arrays.asList(test.slice(), test2.slice()),
                     convert("getByteBufferSet", new TreeSet<ByteBuffer>() {{
                         add(test.slice());
                         add(test2.slice());
                     }}).bs());
    }

    @Test
    public void testObjectSet() {
        AttributeValue value =
                convert("getObjectSet", Collections.singleton(new SubClass()));

        assertEquals(1, value.l().size());
        assertEquals(new HashMap<String, AttributeValue>() {{
                         put("name", AttributeValue.builder().s("name").build());
                         put("value", AttributeValue.builder().n("123").build());
                     }},
                     value.l().get(0).m());

        assertEquals(Arrays.asList(AttributeValue.builder().nul(true).build()),
                     convert("getObjectSet", Collections.<SubClass>singleton(null)).l());
    }

    @Test
    public void testList() {
        assertEquals(Arrays.asList(
                AttributeValue.builder().s("a").build(),
                AttributeValue.builder().s("b").build(),
                AttributeValue.builder().s("c").build()),
                     convert("getList", Arrays.asList("a", "b", "c")).l());

        assertEquals(Arrays.asList(AttributeValue.builder().nul(true).build()),
                     convert("getList", Collections.<String>singletonList(null)).l());
    }

    @Test
    public void testObjectList() {
        AttributeValue value = convert(
                "getObjectList",
                Collections.singletonList(new SubClass()));

        assertEquals(1, value.l().size());
        assertEquals(new HashMap<String, AttributeValue>() {{
                         put("name", AttributeValue.builder().s("name").build());
                         put("value", AttributeValue.builder().n("123").build());
                     }},
                     value.l().get(0).m());
    }

    @Test
    public void testSetList() {
        assertEquals(
                Arrays.asList(AttributeValue.builder().ss("a").build()),
                convert("getSetList", Arrays.asList(
                        Collections.<String>singleton("a"))).l());

        List<Set<String>> list = new ArrayList<Set<String>>();
        list.add(null);

        assertEquals(
                Arrays.asList(AttributeValue.builder().nul(true).build()),
                convert("getSetList", list).l());
    }

    @Test
    public void testMap() {
        assertEquals(new HashMap<String, AttributeValue>() {{
                         put("a", AttributeValue.builder().s("b").build());
                         put("c", AttributeValue.builder().s("d").build());
                         put("e", AttributeValue.builder().s("f").build());
                     }},
                     convert("getMap", new HashMap<String, String>() {{
                         put("a", "b");
                         put("c", "d");
                         put("e", "f");
                     }}).m());

        assertEquals(Collections.singletonMap("a", AttributeValue.builder().nul(true).build()),
                     convert("getMap", Collections.<String, String>singletonMap("a", null)).m());
    }

    @Test
    public void testSetMap() {
        assertEquals(new HashMap<String, AttributeValue>() {{
                         put("a", AttributeValue.builder().ss("a", "b").build());
                     }},
                     convert("getSetMap", new HashMap<String, Set<String>>() {{
                         put("a", new TreeSet<String>(Arrays.asList("a", "b")));
                     }}).m());

        assertEquals(new HashMap<String, AttributeValue>() {{
                         put("a", AttributeValue.builder().ss("a").build());
                         put("b", AttributeValue.builder().nul(true).build());
                     }},
                     convert("getSetMap", new HashMap<String, Set<String>>() {{
                         put("a", new TreeSet<String>(Arrays.asList("a")));
                         put("b", null);
                     }}).m());
    }

    @Test
    public void testObject() {
        assertEquals(new HashMap<String, AttributeValue>() {{
                         put("name", AttributeValue.builder().s("name").build());
                         put("value", AttributeValue.builder().n("123").build());
                     }},
                     convert("getObject", new SubClass()).m());
    }

    @Test
    public void testUnannotatedObject() throws Exception {
        try {
            convert(UnannotatedSubClass.class, UnannotatedSubClass.class.getMethod("getChild"),
                    new UnannotatedSubClass());

            Assert.fail("Expected DynamoDBMappingException");
        } catch (DynamoDbMappingException e) {
            // Ignored or expected.
        }
    }

    @Test
    public void testS3Link() {
        S3ClientCache cache = new S3ClientCache((AwsCredentialsProvider) null);
        S3Link link = new S3Link(cache, "bucket", "key");

        assertEquals("{\"s3\":{"
                     + "\"bucket\":\"bucket\","
                     + "\"key\":\"key\","
                     + "\"region\":null}}",
                     convert("getS3Link", link).s());
    }

    private AttributeValue convert(String getter, Object value) {
        try {

            return convert(TestClass.class, TestClass.class.getMethod(getter), value);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
