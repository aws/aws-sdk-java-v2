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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.pojos.SubClass;
import software.amazon.awssdk.services.dynamodb.pojos.TestClass;
import software.amazon.awssdk.services.dynamodb.pojos.UnannotatedSubClass;

public class StandardModelFactoriesV2UnconvertTest {

    protected static final DynamoDbMapperConfig CONFIG = new DynamoDbMapperConfig.Builder()
            .withTypeConverterFactory(DynamoDbMapperConfig.DEFAULT.getTypeConverterFactory())
            .withConversionSchema(ConversionSchemas.V2)
            .build();

    private static final DynamoDbMapperModelFactory factory = StandardModelFactories.of(new S3Link.Factory(new S3ClientCache((AwsCredentialsProvider) null)));
    private static final DynamoDbMapperModelFactory.TableFactory models = factory.getTableFactory(CONFIG);

    protected <T> Object unconvert(Class<T> clazz, Method getter, Method setter, AttributeValue value) {
        final StandardAnnotationMaps.FieldMap<Object> map = StandardAnnotationMaps.of(getter, null);
        return models.getTable(clazz).field(map.attributeName()).unconvert(value);
    }

    @Test
    public void testBoolean() {
        assertEquals(false, unconvert("getBoolean", "setBoolean",
                                      AttributeValue.builder().n("0").build()));

        assertEquals(true, unconvert("getBoolean", "setBoolean",
                                     AttributeValue.builder().n("1").build()));

        assertEquals(false, unconvert("getBoolean", "setBoolean",
                                      AttributeValue.builder().bool(false).build()));

        assertEquals(true, unconvert("getBoolean", "setBoolean",
                                     AttributeValue.builder().bool(true).build()));
        
        assertEquals(false, unconvert("getBoxedBoolean", "setBoxedBoolean",
                                      AttributeValue.builder().n("0").build()));

        assertEquals(true, unconvert("getBoxedBoolean", "setBoxedBoolean",
                                     AttributeValue.builder().n("1").build()));

        assertEquals(false, unconvert("getBoxedBoolean", "setBoxedBoolean",
                                      AttributeValue.builder().bool(false).build()));

        assertEquals(true, unconvert("getBoxedBoolean", "setBoxedBoolean",
                                     AttributeValue.builder().bool(true).build()));
    }

    @Test
    public void testString() {
        assertEquals("test", unconvert("getString", "setString",
                                       AttributeValue.builder().s("test").build()));

        Assert.assertNull(unconvert("getCustomString", "setCustomString",
                                    AttributeValue.builder().s("ignoreme").build()));
    }

    @Test
    public void testUuid() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, unconvert("getUuid", "setUuid",
                                     AttributeValue.builder().s(uuid.toString()).build()));
    }

    @Test
    public void testDate() {
        assertEquals(new Date(0), unconvert("getDate", "setDate",
                                            AttributeValue.builder().s("1970-01-01T00:00:00.000Z").build()));

        Calendar c = GregorianCalendar.getInstance();
        c.setTimeInMillis(0);

        assertEquals(c, unconvert("getCalendar", "setCalendar",
                                  AttributeValue.builder().s("1970-01-01T00:00:00.000Z").build()));
    }

    @Test
    public void testNumbers() {
        assertEquals((byte) 1, unconvert("getByte", "setByte",
                                         AttributeValue.builder().n("1").build()));
        assertEquals((byte) 1, unconvert("getBoxedByte", "setBoxedByte",
                                         AttributeValue.builder().n("1").build()));

        assertEquals((short) 1, unconvert("getShort", "setShort",
                                          AttributeValue.builder().n("1").build()));
        assertEquals((short) 1, unconvert("getBoxedShort", "setBoxedShort",
                                          AttributeValue.builder().n("1").build()));

        assertEquals(1, unconvert("getInt", "setInt",
                                  AttributeValue.builder().n("1").build()));
        assertEquals(1, unconvert("getBoxedInt", "setBoxedInt",
                                  AttributeValue.builder().n("1").build()));

        assertEquals(1l, unconvert("getLong", "setLong",
                                   AttributeValue.builder().n("1").build()));
        assertEquals(1l, unconvert("getBoxedLong", "setBoxedLong",
                                   AttributeValue.builder().n("1").build()));

        assertEquals(BigInteger.ONE, unconvert("getBigInt", "setBigInt",
                                               AttributeValue.builder().n("1").build()));

        assertEquals(1.5f, unconvert("getFloat", "setFloat",
                                     AttributeValue.builder().n("1.5").build()));
        assertEquals(1.5f, unconvert("getBoxedFloat", "setBoxedFloat",
                                     AttributeValue.builder().n("1.5").build()));

        assertEquals(1.5d, unconvert("getDouble", "setDouble",
                                     AttributeValue.builder().n("1.5").build()));
        assertEquals(1.5d, unconvert("getBoxedDouble", "setBoxedDouble",
                                     AttributeValue.builder().n("1.5").build()));

        assertEquals(BigDecimal.ONE, unconvert("getBigDecimal", "setBigDecimal",
                                               AttributeValue.builder().n("1").build()));
    }

    @Test
    public void testBinary() {
        ByteBuffer test = ByteBuffer.wrap("test".getBytes());
        Assert.assertTrue(Arrays.equals("test".getBytes(), (byte[]) unconvert(
                "getByteArray", "setByteArray",
                AttributeValue.builder().b(test.slice()).build())));

        assertEquals(test.slice(), unconvert("getByteBuffer", "setByteBuffer",
                                             AttributeValue.builder().b(test.slice()).build()));
    }

    @Test
    public void testBooleanSet() {
        assertEquals(new HashSet<Boolean>() {{
                         add(true);
                     }},
                     unconvert("getBooleanSet", "setBooleanSet",
                               AttributeValue.builder().ns("1").build()));

        assertEquals(new HashSet<Boolean>() {{
                         add(false);
                     }},
                     unconvert("getBooleanSet", "setBooleanSet",
                               AttributeValue.builder().ns("0").build()));

        assertEquals(new HashSet<Boolean>() {{
                         add(true);
                         add(false);
                     }},
                     unconvert("getBooleanSet", "setBooleanSet",
                               AttributeValue.builder().ns("0", "1").build()));

        assertEquals(new HashSet<Boolean>() {{
                         add(true);
                     }},
                     unconvert("getBooleanSet", "setBooleanSet",
                               AttributeValue.builder().l(
                                       AttributeValue.builder().bool(true).build()).build()));

        assertEquals(new HashSet<Boolean>() {{
                         add(false);
                     }},
                     unconvert("getBooleanSet", "setBooleanSet",
                               AttributeValue.builder().l(
                                       AttributeValue.builder().bool(false).build()).build()));

        assertEquals(new HashSet<Boolean>() {{
                         add(false);
                         add(true);
                     }},
                     unconvert("getBooleanSet", "setBooleanSet",
                               AttributeValue.builder().l(
                                       AttributeValue.builder().bool(false).build(),
                                       AttributeValue.builder().bool(true).build()).build()));

        assertEquals(new HashSet<Boolean>() {{
                         add(null);
                     }},
                     unconvert("getBooleanSet", "setBooleanSet",
                               AttributeValue.builder().l(
                                       AttributeValue.builder().nul(true).build()).build()));
    }

    @Test
    public void testStringSet() {
        Assert.assertNull(unconvert("getStringSet", "setStringSet",
                                    AttributeValue.builder().nul(true).build()));

        assertEquals(new HashSet<String>() {{
                         add("a");
                         add("b");
                     }},
                     unconvert("getStringSet", "setStringSet",
                               AttributeValue.builder().ss("a", "b").build()));
    }

    @Test
    public void testUuidSet() {
        Assert.assertNull(unconvert("getUuidSet", "setUuidSet",
                                    AttributeValue.builder().nul(true).build()));

        final UUID one = UUID.randomUUID();
        final UUID two = UUID.randomUUID();

        assertEquals(new HashSet<UUID>() {{
                         add(one);
                         add(two);
                     }},
                     unconvert("getUuidSet", "setUuidSet",
                               AttributeValue.builder().ss(
                                       one.toString(),
                                       two.toString()).build()));
    }

    @Test
    public void testDateSet() {
        assertEquals(Collections.singleton(new Date(0)),
                     unconvert("getDateSet", "setDateSet", AttributeValue.builder()
                             .ss("1970-01-01T00:00:00.000Z").build()));

        Calendar c = GregorianCalendar.getInstance();
        c.setTimeInMillis(0);

        assertEquals(Collections.singleton(c),
                     unconvert("getCalendarSet", "setCalendarSet",
                               AttributeValue.builder()
                                       .ss("1970-01-01T00:00:00.000Z").build()));
    }

    @Test
    public void testNumberSet() {
        Assert.assertNull(unconvert("getByteSet", "setByteSet",
                                    AttributeValue.builder().nul(true).build()));
        Assert.assertNull(unconvert("getShortSet", "setShortSet",
                                    AttributeValue.builder().nul(true).build()));
        Assert.assertNull(unconvert("getIntSet", "setIntSet",
                                    AttributeValue.builder().nul(true).build()));
        Assert.assertNull(unconvert("getLongSet", "setLongSet",
                                    AttributeValue.builder().nul(true).build()));
        Assert.assertNull(unconvert("getBigIntegerSet", "setBigIntegerSet",
                                    AttributeValue.builder().nul(true).build()));
        Assert.assertNull(unconvert("getFloatSet", "setFloatSet",
                                    AttributeValue.builder().nul(true).build()));
        Assert.assertNull(unconvert("getDoubleSet", "setDoubleSet",
                                    AttributeValue.builder().nul(true).build()));
        Assert.assertNull(unconvert("getBigDecimalSet", "setBigDecimalSet",
                                    AttributeValue.builder().nul(true).build()));


        assertEquals(new HashSet<Byte>() {{
                         add((byte) 1);
                     }},
                     unconvert("getByteSet", "setByteSet",
                               AttributeValue.builder().ns("1").build()));

        assertEquals(new HashSet<Short>() {{
                         add((short) 1);
                     }},
                     unconvert("getShortSet", "setShortSet",
                               AttributeValue.builder().ns("1").build()));

        assertEquals(new HashSet<Integer>() {{
                         add(1);
                     }},
                     unconvert("getIntSet", "setIntSet",
                               AttributeValue.builder().ns("1").build()));

        assertEquals(new HashSet<Long>() {{
                         add(1l);
                     }},
                     unconvert("getLongSet", "setLongSet",
                               AttributeValue.builder().ns("1").build()));

        assertEquals(new HashSet<BigInteger>() {{
                         add(BigInteger.ONE);
                     }},
                     unconvert("getBigIntegerSet", "setBigIntegerSet",
                               AttributeValue.builder().ns("1").build()));

        assertEquals(new HashSet<Float>() {{
                         add(1.5f);
                     }},
                     unconvert("getFloatSet", "setFloatSet",
                               AttributeValue.builder().ns("1.5").build()));

        assertEquals(new HashSet<Double>() {{
                         add(1.5d);
                     }},
                     unconvert("getDoubleSet", "setDoubleSet",
                               AttributeValue.builder().ns("1.5").build()));

        assertEquals(new HashSet<BigDecimal>() {{
                         add(BigDecimal.ONE);
                     }},
                     unconvert("getBigDecimalSet", "setBigDecimalSet",
                               AttributeValue.builder().ns("1").build()));
    }

    @Test
    public void testBinarySet() {
        Assert.assertNull(unconvert("getByteArraySet", "setByteArraySet",
                                    AttributeValue.builder().nul(true).build()));
        Assert.assertNull(unconvert("getByteBufferSet", "setByteBufferSet",
                                    AttributeValue.builder().nul(true).build()));

        ByteBuffer test = ByteBuffer.wrap("test".getBytes());

        Set<byte[]> result = (Set<byte[]>) unconvert(
                "getByteArraySet", "setByteArraySet",
                AttributeValue.builder().bs(test.slice()).build());

        assertEquals(1, result.size());
        Assert.assertTrue(Arrays.equals(
                "test".getBytes(),
                result.iterator().next()));

        Assert.assertEquals(Collections.singleton(test.slice()),
                            unconvert("getByteBufferSet", "setByteBufferSet",
                                      AttributeValue.builder().bs(test.slice()).build()));
    }

    @Test
    public void testObjectSet() {
        Object result = unconvert("getObjectSet", "setObjectSet",
                                  AttributeValue.builder().l(AttributeValue.builder().m(
                                          new HashMap<String, AttributeValue>() {{
                                              put("name", AttributeValue.builder().s("name").build());
                                              put("value", AttributeValue.builder().n("123").build());
                                              put("null", AttributeValue.builder().nul(true).build());
                                          }}
                                                                                       )
                                          .build())
                                          .build());

        assertEquals(Collections.singleton(new SubClass()), result);

        result = unconvert("getObjectSet", "setObjectSet",
                           AttributeValue.builder().l(
                               AttributeValue.builder()
                               .nul(true)
                               .build())
                           .build());

        assertEquals(Collections.<SubClass>singleton(null), result);
    }

    @Test
    public void testList() {
        Assert.assertNull(unconvert("getList", "setList",
                                    AttributeValue.builder().nul(true).build()));

        assertEquals(Arrays.asList("a", "b", "c"),
                     unconvert("getList", "setList", AttributeValue.builder().l(
                             AttributeValue.builder().s("a").build(),
                             AttributeValue.builder().s("b").build(),
                             AttributeValue.builder().s("c").build())
                         .build()));

        assertEquals(Arrays.asList("a", null),
                     unconvert("getList", "setList", AttributeValue.builder().l(
                             AttributeValue.builder().s("a").build(),
                             AttributeValue.builder().nul(true).build()).build()));
    }

    @Test
    public void testObjectList() {
        Assert.assertNull(unconvert("getObjectList", "setObjectList",
                                    AttributeValue.builder().nul(true).build()));

        assertEquals(Arrays.asList(new SubClass(), null),
                     unconvert("getObjectList", "setObjectList",
                               AttributeValue.builder().l(
                                       AttributeValue.builder().m(new HashMap<String, AttributeValue>() {{
                                           put("name", AttributeValue.builder().s("name").build());
                                           put("value", AttributeValue.builder().n("123").build());
                                           put("null", AttributeValue.builder().nul(true).build());
                                       }}).build(),
                                       AttributeValue.builder().nul(true).build()).build()));
    }

    @Test
    public void testSetList() {
        Assert.assertNull(unconvert("getSetList", "setSetList",
                                    AttributeValue.builder().nul(true).build()));

        assertEquals(Arrays.asList(new Set[] {null}),
                     unconvert("getSetList", "setSetList", AttributeValue.builder().l(
                             AttributeValue.builder().nul(true).build()).build()));

        assertEquals(Arrays.asList(Collections.singleton("a")),
                     unconvert("getSetList", "setSetList", AttributeValue.builder().l(
                             AttributeValue.builder().ss("a").build()).build()));
    }

    @Test
    public void testMap() {
        Assert.assertNull(unconvert("getMap", "setMap",
                                    AttributeValue.builder().nul(true).build()));

        assertEquals(new HashMap<String, String>() {{
                         put("a", "b");
                         put("c", "d");
                     }},
                     unconvert("getMap", "setMap", AttributeValue.builder().m(
                             new HashMap<String, AttributeValue>() {{
                                 put("a", AttributeValue.builder().s("b").build());
                                 put("c", AttributeValue.builder().s("d").build());
                             }}).build()));

        assertEquals(new HashMap<String, String>() {{
                         put("a", null);
                     }},
                     unconvert("getMap", "setMap", AttributeValue.builder().m(
                             new HashMap<String, AttributeValue>() {{
                                 put("a", AttributeValue.builder().nul(true).build());
                             }}).build()));
    }

    @Test
    public void testSetMap() {
        Assert.assertNull(unconvert("getSetMap", "setSetMap",
                                    AttributeValue.builder().nul(true).build()));

        assertEquals(new HashMap<String, Set<String>>() {{
                         put("a", null);
                         put("b", new TreeSet<String>(Arrays.asList("a", "b")));
                     }},
                     unconvert("getSetMap", "setSetMap", AttributeValue.builder().m(
                             new HashMap<String, AttributeValue>() {{
                                 put("a", AttributeValue.builder().nul(true).build());
                                 put("b", AttributeValue.builder().ss("a", "b").build());
                             }}).build()));
    }

    @Test
    public void testObject() {
        Assert.assertNull(unconvert("getObject", "setObject",
                                    AttributeValue.builder().nul(true).build()));

        assertEquals(new SubClass(), unconvert("getObject", "setObject",
                                               AttributeValue.builder().m(new HashMap<String, AttributeValue>() {{
                                                   put("name", AttributeValue.builder().s("name").build());
                                                   put("value", AttributeValue.builder().n("123").build());
                                               }}).build()));

        assertEquals(new SubClass(), unconvert("getObject", "setObject",
                                               AttributeValue.builder().m(new HashMap<String, AttributeValue>() {{
                                                   put("name", AttributeValue.builder().s("name").build());
                                                   put("value", AttributeValue.builder().n("123").build());
                                                   put("null", AttributeValue.builder().nul(true).build());
                                               }}).build()));
    }

    @Test
    public void testUnannotatedObject() throws Exception {
        Method getter = UnannotatedSubClass.class.getMethod("getChild");
        Method setter = UnannotatedSubClass.class
                .getMethod("setChild", UnannotatedSubClass.class);

        try {
            unconvert(UnannotatedSubClass.class, getter, setter, AttributeValue.builder().s("").build());
            Assert.fail("Expected DynamoDBMappingException");
        } catch (DynamoDbMappingException e) {
            // Ignored or expected.
        }
    }

    @Test
    public void testS3Link() {
        S3Link link = (S3Link) unconvert("getS3Link", "setS3Link",
                                         AttributeValue.builder().s("{\"s3\":{"
                                                            + "\"bucket\":\"bucket\","
                                                            + "\"key\":\"key\","
                                                            + "\"region\":null}}").build());

        assertEquals("bucket", link.bucketName());
        assertEquals("key", link.getKey());
        assertEquals(Region.US_EAST_1, link.s3Region());
    }

    public Object unconvert(String getter, String setter, AttributeValue value) {
        try {

            Method gm = TestClass.class.getMethod(getter);
            Method sm = TestClass.class.getMethod(setter, gm.getReturnType());
            return unconvert(TestClass.class, gm, sm, value);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("BOOM", e);
        }
    }

}
