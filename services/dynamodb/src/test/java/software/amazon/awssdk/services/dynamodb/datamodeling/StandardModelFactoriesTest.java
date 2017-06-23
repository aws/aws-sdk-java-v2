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
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperFieldModel.DynamoDbAttributeType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.pojos.AutoKeyAndVal;
import software.amazon.awssdk.services.dynamodb.pojos.Currency;
import software.amazon.awssdk.services.dynamodb.pojos.DateRange;
import software.amazon.awssdk.services.dynamodb.pojos.KeyAndVal;

/**
 * Unit tests for {@link DynamoDbMapperModelFactory.TableFactory}.
 */
public class StandardModelFactoriesTest {

    private static final DynamoDbMapperModelFactory factory = StandardModelFactories.of(S3Link.Factory.of(null));
    private static final DynamoDbMapperModelFactory.TableFactory models = factory.getTableFactory(DynamoDbMapperConfig.DEFAULT);

    @SuppressWarnings("unchecked")
    private static <T> DynamoDbMapperTableModel<T> getTable(T object) {
        return models.getTable((Class<T>) object.getClass());
    }

    /**
     * Assert that the field key properties are correct.
     */
    private static <T, V> void assertFieldKeyType(KeyType keyType, DynamoDbMapperFieldModel<T, V> field,
                                                  DynamoDbMapperTableModel<T> model) {
        assertEquals(keyType, field.keyType());
        if (keyType != null) {
            if (keyType == KeyType.HASH) {
                assertEquals(field, model.hashKey());
            } else if (keyType == KeyType.RANGE) {
                assertEquals(field, model.rangeKeyIfExists());
                assertEquals(field, model.rangeKey());
            }
        }
    }

    /**
     * Assert that the field contains the LSIs.
     */
    private static <T, V> void assertFieldGsiNames(List<String> names, KeyType keyType, DynamoDbMapperFieldModel<T, V> field,
                                                   DynamoDbMapperTableModel<T> model) {
        assertEquals(names == null ? 0 : names.size(), field.globalSecondaryIndexNames(keyType).size());
        assertEquals(true, field.indexed());
        if (names != null) {
            for (final String name : names) {
                assertEquals(true, field.globalSecondaryIndexNames(keyType).contains(name));
                assertEquals(true, model.globalSecondaryIndex(name) != null);
                assertEquals(true, !model.globalSecondaryIndexes().isEmpty());
            }
        }
    }

    /**
     * Assert that the field contains the LSIs.
     */
    private static <T, V> void assertFieldLsiNames(List<String> names, DynamoDbMapperFieldModel<T, V> field,
                                                   DynamoDbMapperTableModel<T> model) {
        assertEquals(names == null ? 0 : names.size(), field.localSecondaryIndexNames().size());
        assertEquals(true, field.indexed());
        if (names != null) {
            for (final String name : names) {
                assertEquals(true, field.localSecondaryIndexNames().contains(name));
                assertEquals(true, model.localSecondaryIndex(name) != null);
                assertEquals(true, !model.localSecondaryIndexes().isEmpty());
            }
        }
    }

    /**
     * Test mappings.
     */
    @Test
    public void testHashAndRangeKey() {
        final Object obj = new AutoKeyAndVal<String>() {
            @DynamoDbHashKey(attributeName = "hk")
            public String getKey() {
                return super.getKey();
            }

            @DynamoDbRangeKey(attributeName = "rk")
            public String getVal() {
                return super.getVal();
            }

            public void setVal(final String val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        assertFieldKeyType(KeyType.HASH, model.field("hk"), model);
        assertFieldKeyType(KeyType.RANGE, model.field("rk"), model);
    }

    /**
     * Test mappings.
     */
    @Test(expected = DynamoDbMappingException.class)
    public void testHashAndRangeKeyConflict() {
        final Object obj = new AutoKeyAndVal<String>() {
            @DynamoDbHashKey
            @DynamoDbRangeKey
            public String getKey() {
                return super.getKey();
            }
        };
        getTable(obj);
    }

    /**
     * Test mappings.
     */
    @Test
    public void testNamed() {
        final Object obj = new AutoKeyAndVal<String>() {
            @DynamoDbNamed("value")
            public String getVal() {
                return super.getVal();
            }

            public void setVal(final String val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        assertEquals(2, model.fields().size());
        assertNotNull(model.field("key"));
        assertNotNull(model.field("value"));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAttributeTypeAsNumber() {
        final Object obj = new AutoKeyAndVal<String>() {
            @DynamoDbTyped(DynamoDbAttributeType.N)
            public String getVal() {
                return super.getVal();
            }

            public void setVal(final String val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAttributeType.N, val.attributeType());
    }

    @Test
    public void testAttributeTypeAsAttributeValueNumber() {
        final Object obj = new AutoKeyAndVal<AttributeValue>() {
            @DynamoDbTyped(DynamoDbAttributeType.N)
            public AttributeValue getVal() {
                return super.getVal();
            }

            public void setVal(final AttributeValue val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, AttributeValue> val = model.field("val");
        assertEquals(DynamoDbAttributeType.N, val.attributeType());
        assertEquals("123", val.convert(AttributeValue.builder().n("123").build()).n());
        assertEquals("123", val.unconvert(AttributeValue.builder().n("123").build()).n());
    }

    @Test
    public void testAttributeTypeAsAttributeValueMap() {
        final Object obj = new AutoKeyAndVal<AttributeValue>() {
            @DynamoDbTyped(DynamoDbAttributeType.M)
            public AttributeValue getVal() {
                return super.getVal();
            }

            public void setVal(final AttributeValue val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, AttributeValue> val = model.field("val");
        assertEquals(DynamoDbAttributeType.M, val.attributeType());

        Map<String, AttributeValue> map = new HashMap<String, AttributeValue>();
        map.put("A", AttributeValue.builder().n("123").build());
        map = Collections.unmodifiableMap(map);

        assertEquals("123", val.convert(AttributeValue.builder().m(map).build()).m().get("A").n());
        assertEquals("123", val.unconvert(AttributeValue.builder().m(map).build()).m().get("A").n());
    }

    /**
     * Test mappings.
     */
    @Test
    public void testScalarAttributeStringTimeZone() {
        final Object obj = new AutoKeyAndVal<TimeZone>() {
            @DynamoDbHashKey
            public String getKey() {
                return super.getKey();
            }

            @DynamoDbScalarAttribute(type = ScalarAttributeType.S)
            public TimeZone getVal() {
                return super.getVal();
            }

            public void setVal(final TimeZone val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, TimeZone> val = model.field("val");
        assertEquals(DynamoDbAttributeType.S, val.attributeType());
        assertEquals("America/New_York", val.convert(TimeZone.getTimeZone("America/New_York")).s());
        assertEquals("America/New_York", val.unconvert(AttributeValue.builder().s("America/New_York").build()).getID());
    }

    /**
     * Test mappings.
     */
    @Test
    public void testScalarAttributeStringLocale() {
        final Object obj = new AutoKeyAndVal<Locale>() {
            @DynamoDbHashKey
            public String getKey() {
                return super.getKey();
            }

            @DynamoDbScalarAttribute(type = ScalarAttributeType.S)
            public Locale getVal() {
                return super.getVal();
            }

            public void setVal(final Locale val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Locale> val = model.field("val");
        assertEquals(DynamoDbAttributeType.S, val.attributeType());
        assertEquals("en-CA", val.convert(new Locale("en", "CA")).s());
        assertEquals("en-CA", val.unconvert(AttributeValue.builder().s("en-CA").build()).toString().replaceAll("_", "-"));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testScalarAttributeBinaryUuid() {
        final Object obj = new AutoKeyAndVal<UUID>() {
            @DynamoDbHashKey
            public String getKey() {
                return super.getKey();
            }

            @DynamoDbScalarAttribute(type = ScalarAttributeType.B)
            public UUID getVal() {
                return super.getVal();
            }

            public void setVal(final UUID val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        assertEquals(DynamoDbAttributeType.B, model.field("val").attributeType());
        final UUID val = UUID.randomUUID();
        final AttributeValue converted = model.field("val").convert(val);
        assertNotNull(converted.b());
        assertEquals(val, model.field("val").unconvert(converted));
    }

    @Test
    public void testScalarAttributeAttributeName() {
        final Object obj = new AutoKeyAndVal<String>() {
            @DynamoDbHashKey
            public String getKey() {
                return super.getKey();
            }

            @DynamoDbScalarAttribute(attributeName = "value", type = ScalarAttributeType.S)
            public String getVal() {
                return super.getVal();
            }

            public void setVal(final String val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = models.getTable((Class<Object>) obj.getClass());
        final DynamoDbMapperFieldModel<Object, String> val = model.field("value");
        assertEquals(DynamoDbAttributeType.S, val.attributeType());
    }

    /**
     * Test mappings.
     */
    @Test
    public void testIgnore() {
        final Object obj = new AutoKeyAndVal<String>() {
            private String ignore;

            @DynamoDbAttribute(attributeName = "value")
            public String getVal() {
                return super.getVal();
            }

            public void setVal(final String val) {
                super.setVal(val);
            }

            @DynamoDbIgnore
            @DynamoDbAttribute(attributeName = "ignore")
            public String getIgnore() {
                return this.ignore;
            }

            public void setIgnore(final String ignore) {
                this.ignore = ignore;
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        assertEquals(2, model.fields().size());
        assertNotNull(model.field("key"));
        assertNotNull(model.field("value"));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testConvertedBool() {
        final Object obj = new AutoKeyAndVal<Boolean>() {
            @DynamoDbConvertedBool(DynamoDbConvertedBool.Format.Y_N)
            public Boolean getVal() {
                return super.getVal();
            }

            public void setVal(final Boolean val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Boolean> val = model.field("val");
        assertEquals(DynamoDbAttributeType.S, val.attributeType());
        assertEquals("Y", val.convert(Boolean.TRUE).s());
        assertEquals(Boolean.TRUE, val.unconvert(AttributeValue.builder().s("Y").build()));
        assertEquals("N", val.convert(Boolean.FALSE).s());
        assertEquals(Boolean.FALSE, val.unconvert(AttributeValue.builder().s("N").build()));
        assertEquals(null, val.convert(null));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedHashKeyString() {
        final Object obj = new AutoKeyAndVal<String>() {
            @DynamoDbAttribute
            public String getVal() {
                return super.getVal();
            }

            public void setVal(final String val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> key = model.field("key");
        assertFieldKeyType(KeyType.HASH, key, model);
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, key.getGenerateStrategy());
        assertNotNull(key.generate(null));
        assertNotNull(key.generate(UUID.randomUUID().toString()));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedRangeKeyUuid() {
        final Object obj = new AutoKeyAndVal<UUID>() {
            @DynamoDbRangeKey
            @DynamoDbAutoGeneratedKey
            public UUID getVal() {
                return super.getVal();
            }

            public void setVal(final UUID val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertFieldKeyType(KeyType.RANGE, val, model);
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertNotNull(val.generate(null));
        assertNotNull(val.generate(UUID.randomUUID()));
    }

    /**
     * Test mappings.
     */
    @Test(expected = DynamoDbMappingException.class)
    public void testAutoGeneratedConflict() {
        final Object obj = new AutoKeyAndVal<String>() {
            @DynamoDbHashKey
            @DynamoDbAutoGeneratedKey
            @DynamoDbVersionAttribute
            public String getKey() {
                return super.getKey();
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
    }

    /**
     * Test mappings.
     */
    @Test(expected = DynamoDbMappingException.class)
    public void testAutoGeneratedVersionUuid() {
        final Object obj = new AutoKeyAndVal<UUID>() {
            @DynamoDbVersionAttribute
            public UUID getVal() {
                return super.getVal();
            }

            public void setVal(final UUID val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        val.generate(null); //<- should fail
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedVersionBigInteger() {
        final Object obj = new AutoKeyAndVal<BigInteger>() {
            @DynamoDbVersionAttribute
            public BigInteger getVal() {
                return super.getVal();
            }

            public void setVal(final BigInteger val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(true, val.versioned());
        assertEquals(DynamoDbAutoGenerateStrategy.ALWAYS, val.getGenerateStrategy());
        assertEquals(BigInteger.ONE, val.generate(null));
        assertEquals(BigInteger.valueOf((int) 2), val.generate(BigInteger.ONE));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedVersionByte() {
        final Object obj = new AutoKeyAndVal<Byte>() {
            @DynamoDbVersionAttribute
            public Byte getVal() {
                return super.getVal();
            }

            public void setVal(final Byte val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(true, val.versioned());
        assertEquals(DynamoDbAutoGenerateStrategy.ALWAYS, val.getGenerateStrategy());
        assertEquals(Byte.valueOf((byte) 1), val.generate(null));
        assertEquals(Byte.valueOf((byte) 2), val.generate(Byte.valueOf((byte) 1)));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedVersionBytePrimitive() {
        final Object obj = new AutoKeyAndVal<String>() {
            private byte rvn;

            @DynamoDbAttribute
            public String getVal() {
                return super.getVal();
            }

            public void setVal(final String val) {
                super.setVal(val);
            }

            @DynamoDbVersionAttribute
            public byte getRvn() {
                return this.rvn;
            }

            public void setRvn(final byte rvn) {
                this.rvn = rvn;
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> rvn = model.field("rvn");
        assertEquals(true, rvn.versioned());
        assertEquals(DynamoDbAutoGenerateStrategy.ALWAYS, rvn.getGenerateStrategy());
        assertEquals(Byte.valueOf((byte) 1), rvn.generate(null));
        assertEquals(Byte.valueOf((byte) 2), rvn.generate(Byte.valueOf((byte) 1)));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedVersionInteger() {
        final Object obj = new AutoKeyAndVal<Integer>() {
            @DynamoDbVersionAttribute
            public Integer getVal() {
                return super.getVal();
            }

            public void setVal(final Integer val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(true, val.versioned());
        assertEquals(DynamoDbAutoGenerateStrategy.ALWAYS, val.getGenerateStrategy());
        assertEquals(Integer.valueOf((int) 1), val.generate(null));
        assertEquals(Integer.valueOf((int) 2), val.generate(Integer.valueOf((int) 1)));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedVersionIntegerPrimitive() {
        final Object obj = new AutoKeyAndVal<String>() {
            private int rvn;

            @DynamoDbAttribute
            public String getVal() {
                return super.getVal();
            }

            public void setVal(final String val) {
                super.setVal(val);
            }

            @DynamoDbVersionAttribute
            public int getRvn() {
                return this.rvn;
            }

            public void setRvn(final int rvn) {
                this.rvn = rvn;
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> rvn = model.field("rvn");
        assertEquals(true, rvn.versioned());
        assertEquals(DynamoDbAutoGenerateStrategy.ALWAYS, rvn.getGenerateStrategy());
        assertEquals(Integer.valueOf((int) 1), rvn.generate(null));
        assertEquals(Integer.valueOf((int) 2), rvn.generate(Integer.valueOf((int) 1)));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedVersionLong() {
        final Object obj = new AutoKeyAndVal<Long>() {
            @DynamoDbVersionAttribute
            public Long getVal() {
                return super.getVal();
            }

            public void setVal(final Long val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(true, val.versioned());
        assertEquals(DynamoDbAutoGenerateStrategy.ALWAYS, val.getGenerateStrategy());
        assertEquals(Long.valueOf((long) 1), val.generate(null));
        assertEquals(Long.valueOf((long) 2), val.generate(Long.valueOf((long) 1)));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedVersionLongPrimitive() {
        final Object obj = new AutoKeyAndVal<String>() {
            private long rvn;

            @DynamoDbAttribute
            public String getVal() {
                return super.getVal();
            }

            public void setVal(final String val) {
                super.setVal(val);
            }

            @DynamoDbVersionAttribute
            public long getRvn() {
                return this.rvn;
            }

            public void setRvn(final long rvn) {
                this.rvn = rvn;
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> rvn = model.field("rvn");
        assertEquals(true, rvn.versioned());
        assertEquals(DynamoDbAutoGenerateStrategy.ALWAYS, rvn.getGenerateStrategy());
        assertEquals(Long.valueOf((long) 1), rvn.generate(null));
        assertEquals(Long.valueOf((long) 2), rvn.generate(Long.valueOf((long) 1)));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedVersionshort() {
        final Object obj = new AutoKeyAndVal<Short>() {
            @DynamoDbVersionAttribute
            public Short getVal() {
                return super.getVal();
            }

            public void setVal(final Short val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(true, val.versioned());
        assertEquals(DynamoDbAutoGenerateStrategy.ALWAYS, val.getGenerateStrategy());
        assertEquals(Short.valueOf((short) 1), val.generate(null));
        assertEquals(Short.valueOf((short) 2), val.generate(Short.valueOf((short) 1)));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedVersionshortPrimitive() {
        final Object obj = new AutoKeyAndVal<String>() {
            private short rvn;

            @DynamoDbAttribute
            public String getVal() {
                return super.getVal();
            }

            public void setVal(final String val) {
                super.setVal(val);
            }

            @DynamoDbVersionAttribute
            public short getRvn() {
                return this.rvn;
            }

            public void setRvn(final short rvn) {
                this.rvn = rvn;
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> rvn = model.field("rvn");
        assertEquals(true, rvn.versioned());
        assertEquals(DynamoDbAutoGenerateStrategy.ALWAYS, rvn.getGenerateStrategy());
        assertEquals(Short.valueOf((short) 1), rvn.generate(null));
        assertEquals(Short.valueOf((short) 2), rvn.generate(Short.valueOf((short) 1)));
    }

    /**
     * Test mappings.
     */
    @Test(expected = DynamoDbMappingException.class)
    public void testAutoGeneratedTimestampUuid() {
        final Object obj = new AutoKeyAndVal<UUID>() {
            @DynamoDbAutoGeneratedTimestamp
            public UUID getVal() {
                return super.getVal();
            }

            public void setVal(final UUID val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedTimestampCalendar() {
        final Object obj = new AutoKeyAndVal<Calendar>() {
            @DynamoDbAutoGeneratedTimestamp
            public Calendar getVal() {
                return super.getVal();
            }

            public void setVal(final Calendar val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.ALWAYS, val.getGenerateStrategy());
        assertNotNull(val.generate(null));
        assertNotNull(val.generate(Calendar.getInstance()));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedTimestampDateKey() {
        final Object obj = new AutoKeyAndVal<Date>() {
            @DynamoDbRangeKey
            @DynamoDbAutoGeneratedTimestamp(strategy = DynamoDbAutoGenerateStrategy.CREATE)
            public Date getVal() {
                return super.getVal();
            }

            public void setVal(final Date val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertFieldKeyType(KeyType.RANGE, val, model);
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertNotNull(val.generate(null));
        assertNotNull(val.generate(new Date()));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedTimestampDateVal() {
        final Object obj = new AutoKeyAndVal<Date>() {
            @DynamoDbAutoGeneratedTimestamp
            public Date getVal() {
                return super.getVal();
            }

            public void setVal(final Date val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.ALWAYS, val.getGenerateStrategy());
        assertNotNull(val.generate(null));
        assertNotNull(val.generate(new Date()));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedTimestampLong() {
        final Object obj = new AutoKeyAndVal<Long>() {
            @DynamoDbAutoGeneratedTimestamp
            public Long getVal() {
                return super.getVal();
            }

            public void setVal(final Long val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.ALWAYS, val.getGenerateStrategy());
        assertNotNull(val.generate(null));
        assertNotNull(val.generate(System.currentTimeMillis()));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedDefaultByteBuffer() {
        final Object obj = new AutoKeyAndVal<ByteBuffer>() {
            @DynamoDbAutoGeneratedDefault("default-val")
            public ByteBuffer getVal() {
                return super.getVal();
            }

            public void setVal(final ByteBuffer val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertNotNull(val.generate(null));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedDefaultBigDecimal() {
        final Object obj = new AutoKeyAndVal<BigDecimal>() {
            @DynamoDbAutoGeneratedDefault("1234.5")
            public BigDecimal getVal() {
                return super.getVal();
            }

            public void setVal(final BigDecimal val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertEquals(BigDecimal.valueOf(1234.5D), val.generate(null));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedDefaultBigInteger() {
        final Object obj = new AutoKeyAndVal<BigInteger>() {
            @DynamoDbAutoGeneratedDefault("1234")
            public BigInteger getVal() {
                return super.getVal();
            }

            public void setVal(final BigInteger val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertEquals(BigInteger.valueOf(1234), val.generate(null));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedDefaultBoolean_true() {
        final Object obj = new AutoKeyAndVal<Boolean>() {
            @DynamoDbAutoGeneratedDefault("true")
            public Boolean getVal() {
                return super.getVal();
            }

            public void setVal(final Boolean val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertEquals(Boolean.TRUE, val.generate(null));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedDefaultBoolean_0() {
        final Object obj = new AutoKeyAndVal<Boolean>() {
            @DynamoDbAutoGeneratedDefault("0")
            public Boolean getVal() {
                return super.getVal();
            }

            public void setVal(final Boolean val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertEquals(Boolean.FALSE, val.generate(null));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedDefaultBoolean_1() {
        final Object obj = new AutoKeyAndVal<Boolean>() {
            @DynamoDbAutoGeneratedDefault("1")
            public Boolean getVal() {
                return super.getVal();
            }

            public void setVal(final Boolean val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertEquals(Boolean.TRUE, val.generate(null));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedDefaultBoolean_y() {
        final Object obj = new AutoKeyAndVal<Boolean>() {
            @DynamoDbAutoGeneratedDefault("y")
            public Boolean getVal() {
                return super.getVal();
            }

            public void setVal(final Boolean val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertEquals(Boolean.TRUE, val.generate(null));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedDefaultBoolean_Y() {
        final Object obj = new AutoKeyAndVal<Boolean>() {
            @DynamoDbAutoGeneratedDefault("Y")
            public Boolean getVal() {
                return super.getVal();
            }

            public void setVal(final Boolean val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertEquals(Boolean.TRUE, val.generate(null));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedDefaultByte() {
        final Object obj = new AutoKeyAndVal<Byte>() {
            @DynamoDbAutoGeneratedDefault("1")
            public Byte getVal() {
                return super.getVal();
            }

            public void setVal(final Byte val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertEquals(Byte.valueOf((byte) 1), val.generate(null));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedDefaultCharacter() {
        final Object obj = new AutoKeyAndVal<Character>() {
            @DynamoDbAutoGeneratedDefault("A")
            public Character getVal() {
                return super.getVal();
            }

            public void setVal(final Character val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertEquals(Character.valueOf('A'), val.generate(null));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedDefaultCurrency() {
        final Object obj = new AutoKeyAndVal<java.util.Currency>() {
            @DynamoDbAutoGeneratedDefault("CAD")
            public java.util.Currency getVal() {
                return super.getVal();
            }

            public void setVal(final java.util.Currency val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertEquals(java.util.Currency.getInstance("CAD"), val.generate(null));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedDefaultDouble() {
        final Object obj = new AutoKeyAndVal<Double>() {
            @DynamoDbAutoGeneratedDefault("1234.5")
            public Double getVal() {
                return super.getVal();
            }

            public void setVal(final Double val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertEquals(Double.valueOf(1234.5D), val.generate(null));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedDefaultEnum() {
        final Object obj = new AutoKeyAndVal<TimeUnit>() {
            @DynamoDbTypeConvertedEnum
            @DynamoDbAutoGeneratedDefault("SECONDS")
            public TimeUnit getVal() {
                return super.getVal();
            }

            public void setVal(final TimeUnit val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertEquals(TimeUnit.SECONDS, val.generate(null));
        assertEquals(TimeUnit.SECONDS, val.generate(TimeUnit.MILLISECONDS));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedDefaultFloat() {
        final Object obj = new AutoKeyAndVal<Float>() {
            @DynamoDbAutoGeneratedDefault("1234.5")
            public Float getVal() {
                return super.getVal();
            }

            public void setVal(final Float val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertEquals(Float.valueOf(1234.5F), val.generate(null));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedDefaultInteger() {
        final Object obj = new AutoKeyAndVal<Integer>() {
            @DynamoDbAutoGeneratedDefault("1234")
            public Integer getVal() {
                return super.getVal();
            }

            public void setVal(final Integer val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertEquals(Integer.valueOf((int) 1234), val.generate(null));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedDefaultLong() {
        final Object obj = new AutoKeyAndVal<Long>() {
            @DynamoDbAutoGeneratedDefault("1234")
            public Long getVal() {
                return super.getVal();
            }

            public void setVal(final Long val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertEquals(Long.valueOf((long) 1234), val.generate(null));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedDefaultShort() {
        final Object obj = new AutoKeyAndVal<Short>() {
            @DynamoDbAutoGeneratedDefault("1234")
            public Short getVal() {
                return super.getVal();
            }

            public void setVal(final Short val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertEquals(Short.valueOf((short) 1234), val.generate(null));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedDefaultString() {
        final Object obj = new AutoKeyAndVal<String>() {
            @DynamoDbAutoGeneratedDefault("default-val")
            public String getVal() {
                return super.getVal();
            }

            public void setVal(final String val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertEquals("default-val", val.generate(null));
        assertEquals("default-val", val.generate("not-default"));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedDefaultTimeZone() {
        final Object obj = new AutoKeyAndVal<TimeZone>() {
            @DynamoDbAutoGeneratedDefault("America/New_York")
            public TimeZone getVal() {
                return super.getVal();
            }

            public void setVal(final TimeZone val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertEquals(TimeZone.getTimeZone("America/New_York"), val.generate(null));
        assertEquals(TimeZone.getTimeZone("America/New_York"), val.generate(TimeZone.getTimeZone("America/Los_Angeles")));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testAutoGeneratedDefaultUuid() {
        final Object obj = new AutoKeyAndVal<UUID>() {
            @DynamoDbAutoGeneratedDefault("12345678-1234-1234-1234-123456789012")
            public UUID getVal() {
                return super.getVal();
            }

            public void setVal(final UUID val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> val = model.field("val");
        assertEquals(DynamoDbAutoGenerateStrategy.CREATE, val.getGenerateStrategy());
        assertEquals(UUID.fromString("12345678-1234-1234-1234-123456789012"), val.generate(null));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testIndexHashKeyGlobalSecondaryIndexName() {
        final Object obj = new AutoKeyAndVal<String>() {
            @DynamoDbIndexHashKey(attributeName = "gsi_hk", globalSecondaryIndexName = "gsi")
            public String getVal() {
                return super.getVal();
            }

            public void setVal(final String val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> gsi_hk = model.field("gsi_hk");
        assertFieldGsiNames(Arrays.asList("gsi"), KeyType.HASH, gsi_hk, model);
        assertFieldGsiNames(null, KeyType.RANGE, gsi_hk, model);
        assertFieldLsiNames(null, gsi_hk, model);
    }

    /**
     * Test mappings.
     */
    @Test
    public void testIndexHashKeyGlobalSecondaryIndexNames() {
        final Object obj = new AutoKeyAndVal<String>() {
            @DynamoDbIndexHashKey(attributeName = "gsi_hk", globalSecondaryIndexNames = "gsi")
            public String getVal() {
                return super.getVal();
            }

            public void setVal(final String val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> gsi_hk = model.field("gsi_hk");
        assertFieldGsiNames(Arrays.asList("gsi"), KeyType.HASH, gsi_hk, model);
        assertFieldGsiNames(null, KeyType.RANGE, gsi_hk, model);
        assertFieldLsiNames(null, gsi_hk, model);
    }

    /**
     * Test mappings.
     */
    @Test
    public void testIndexRangeKeyGlobalSecondaryIndexName() {
        final Object obj = new AutoKeyAndVal<String>() {
            private String gsi;

            @DynamoDbIndexHashKey(attributeName = "gsi_hk", globalSecondaryIndexName = "gsi")
            public String getVal() {
                return super.getVal();
            }

            public void setVal(final String val) {
                super.setVal(val);
            }

            @DynamoDbIndexRangeKey(attributeName = "gsi_rk", globalSecondaryIndexName = "gsi")
            public String getGsi() {
                return this.gsi;
            }

            public void setGsi(final String gsi) {
                this.gsi = gsi;
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> gsi_hk = model.field("gsi_hk");
        assertFieldGsiNames(Arrays.asList("gsi"), KeyType.HASH, gsi_hk, model);
        assertFieldGsiNames(null, KeyType.RANGE, gsi_hk, model);
        assertFieldLsiNames(null, gsi_hk, model);
        final DynamoDbMapperFieldModel<Object, Object> gsi_rk = model.field("gsi_rk");
        assertFieldGsiNames(null, KeyType.HASH, gsi_rk, model);
        assertFieldGsiNames(Arrays.asList("gsi"), KeyType.RANGE, gsi_rk, model);
        assertFieldLsiNames(null, gsi_rk, model);
    }

    /**
     * Test mappings.
     */
    @Test
    public void testIndexRangeKeyGlobalSecondaryIndexNames() {
        final Object obj = new AutoKeyAndVal<String>() {
            private String gsi;

            @DynamoDbIndexHashKey(attributeName = "gsi_hk", globalSecondaryIndexName = "gsi")
            public String getVal() {
                return super.getVal();
            }

            public void setVal(final String val) {
                super.setVal(val);
            }

            @DynamoDbIndexRangeKey(attributeName = "gsi_rk", globalSecondaryIndexNames = "gsi")
            public String getGsi() {
                return this.gsi;
            }

            public void setGsi(final String gsi) {
                this.gsi = gsi;
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> gsi_hk = model.field("gsi_hk");
        assertFieldGsiNames(Arrays.asList("gsi"), KeyType.HASH, gsi_hk, model);
        assertFieldGsiNames(null, KeyType.RANGE, gsi_hk, model);
        assertFieldLsiNames(null, gsi_hk, model);
        final DynamoDbMapperFieldModel<Object, Object> gsi_rk = model.field("gsi_rk");
        assertFieldGsiNames(null, KeyType.HASH, gsi_rk, model);
        assertFieldGsiNames(Arrays.asList("gsi"), KeyType.RANGE, gsi_rk, model);
        assertFieldLsiNames(null, gsi_rk, model);
    }

    /**
     * Test mappings.
     */
    @Test
    public void testIndexRangeKeyiLocalSecondaryIndexName() {
        final Object obj = new AutoKeyAndVal<String>() {
            @DynamoDbIndexRangeKey(attributeName = "lsi_rk", localSecondaryIndexName = "lsi")
            public String getVal() {
                return super.getVal();
            }

            public void setVal(final String val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> lsi_rk = model.field("lsi_rk");
        assertFieldLsiNames(Arrays.asList("lsi"), lsi_rk, model);
    }

    /**
     * Test mappings.
     */
    @Test
    public void testIndexRangeKeyLocalSecondaryIndexNames() {
        final Object obj = new AutoKeyAndVal<String>() {
            @DynamoDbIndexRangeKey(attributeName = "lsi_rk", localSecondaryIndexNames = "lsi")
            public String getVal() {
                return super.getVal();
            }

            public void setVal(final String val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        final DynamoDbMapperFieldModel<Object, Object> lsi_rk = model.field("lsi_rk");
        assertFieldLsiNames(Arrays.asList("lsi"), lsi_rk, model);
    }

    @Test
    public void testFlattened() {
        final Object obj = new AutoKeyAndVal<DateRange>() {
            @DynamoDbFlattened(attributes = {
                    @DynamoDbAttribute(mappedBy = "start", attributeName = "DateRangeStart"),
                    @DynamoDbAttribute(mappedBy = "end", attributeName = "DateRangeEnd")})
            public DateRange getVal() {
                return super.getVal();
            }

            public void setVal(final DateRange val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        assertEquals(3, model.fields().size());
        assertEquals("DateRangeStart", model.field("DateRangeStart").name());
        assertEquals("DateRangeEnd", model.field("DateRangeEnd").name());
    }

    /**
     * Test mappings.
     */
    @Test
    public void testFlattenedNotAllSpecified() {
        final Object obj = new AutoKeyAndVal<DateRange>() {
            @DynamoDbFlattened(attributes = {
                    @DynamoDbAttribute(mappedBy = "start", attributeName = "DateRangeStart")})
            public DateRange getVal() {
                return super.getVal();
            }

            public void setVal(final DateRange val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        assertEquals(2, model.fields().size());
        assertEquals("DateRangeStart", model.field("DateRangeStart").name());
    }

    /**
     * Test mappings.
     */
    @Test(expected = DynamoDbMappingException.class)
    public void testFlattenedInvalidMappedBy() {
        final Object obj = new AutoKeyAndVal<DateRange>() {
            @DynamoDbFlattened(attributes = {
                    @DynamoDbAttribute(mappedBy = "xstart", attributeName = "DateRangeStart"),
                    @DynamoDbAttribute(mappedBy = "xend", attributeName = "DateRangeEnd")})
            public DateRange getVal() {
                return super.getVal();
            }

            public void setVal(final DateRange val) {
                super.setVal(val);
            }
        };
        getTable(obj);
    }

    /**
     * Test mappings.
     */
    @Test
    public void testFlattenedMultipleSameType() {
        final Object obj = new AutoKeyAndVal<Currency>() {
            private Currency other;

            @DynamoDbFlattened(attributes = {
                    @DynamoDbAttribute(mappedBy = "amount", attributeName = "firstAmount"),
                    @DynamoDbAttribute(mappedBy = "unit", attributeName = "firstUnit")})
            public Currency getVal() {
                return super.getVal();
            }

            public void setVal(final Currency val) {
                super.setVal(val);
            }

            @DynamoDbFlattened(attributes = {
                    @DynamoDbAttribute(mappedBy = "amount", attributeName = "secondAmount"),
                    @DynamoDbAttribute(mappedBy = "unit", attributeName = "secondUnit")})
            public Currency getOther() {
                return this.other;
            }

            public void setOther(final Currency other) {
                this.other = other;
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);
        assertEquals(5, model.fields().size());
        assertEquals("firstAmount", model.field("firstAmount").name());
        assertEquals("firstUnit", model.field("firstUnit").name());
        assertEquals("secondAmount", model.field("secondAmount").name());
        assertEquals("secondUnit", model.field("secondUnit").name());
    }

    /**
     * Test mappings.
     */
    @Test
    public void testTableAndDocument() {
        models.getTable(TableAndDocument.class);
    }

    /**
     * Test mappings.
     */
    @Test
    public void testInheritedWithNoTable() {
        final Object obj = new KeyAndVal<String, String>() {
            @DynamoDbHashKey(attributeName = "hk")
            public String getKey() {
                return super.getKey();
            }

            public void setKey(String key) {
                super.setKey(key);
            }

            @DynamoDbAttribute(attributeName = "value")
            public String getVal() {
                return super.getVal();
            }

            public void setVal(String val) {
                super.setVal(val);
            }
        };
        final DynamoDbMapperTableModel<Object> model = getTable(obj);

        final DynamoDbMapperFieldModel<Object, Object> key = model.field("hk");
        assertNotNull(key);
        assertEquals(KeyType.HASH, key.keyType());
        assertEquals(DynamoDbAttributeType.S, key.attributeType());

        final DynamoDbMapperFieldModel<Object, Object> val = model.field("value");
        assertNotNull(val);
        assertEquals(DynamoDbAttributeType.S, val.attributeType());
    }

    /**
     * Test mappings to make sure the bridge method is ruled out.
     */
    @Test
    public void testFindRelevantGettersWithBridgeMethod() {
        final DynamoDbMapperTableModel<SubClass> model = models.getTable(SubClass.class);
        assertEquals("only two getter should be returned", 2, model.fields().size());
        assertEquals("return type should be Integer rather than Object", DynamoDbAttributeType.N, model.field("t").attributeType());
    }

    /**
     * Test mappings.
     */
    @Test
    public void testNonMappedInheritedProperties() {
        final DynamoDbMapperTableModel<NonMappedInheritedProperties> model = models.getTable(NonMappedInheritedProperties.class);
        assertEquals(2, model.fields().size());
        assertNotNull(model.field("doUse"));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testInheritedProperties() {
        final DynamoDbMapperTableModel<BaseTablePojo> model1 = models.getTable(BaseTablePojo.class);
        assertEquals(3, model1.fields().size());
        assertNotNull(model1.field("hashKeyOnField"));
        assertNotNull(model1.field("rangeKeyOnGetter"));
        final DynamoDbMapperTableModel<TablePojoSubclass> model2 = models.getTable(TablePojoSubclass.class);
        assertEquals(4, model2.fields().size());
        assertNotNull(model2.field("hashKeyOnField"));
        assertNotNull(model2.field("rangeKeyOnGetter"));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testPojoWithGetterAnnotations() {
        PojoAsserts.assertAll(models.getTable(PojoWithGetterAnnotations.class));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testPojoWithFieldAnnotations() {
        PojoAsserts.assertAll(models.getTable(PojoWithFieldAnnotations.class));
    }

    /**
     * Test mappings.
     */
    @Test
    public void testPojoWithMixedAnnotations() {
        PojoAsserts.assertAll(models.getTable(PojoWithMixedAnnotations.class));
    }

    /**
     * Pojo field assersions.
     */
    private static enum PojoAsserts {
        hashKey(KeyType.HASH, null),
        rangeKey(KeyType.RANGE, DynamoDbAutoGenerateStrategy.CREATE),
        indexHashKey(null, null),
        indexRangeKey(null, null),
        actualAttrName(null, null),
        versionedAttr(null, DynamoDbAutoGenerateStrategy.ALWAYS),
        marshallingAttr(null, null);
        private final DynamoDbAutoGenerateStrategy generateStrategy;
        private final KeyType keyType;

        private PojoAsserts(final KeyType keyType, final DynamoDbAutoGenerateStrategy generateStrategy) {
            this.generateStrategy = generateStrategy;
            this.keyType = keyType;
        }

        public static <T> void assertAll(final DynamoDbMapperTableModel<T> model) {
            for (final PojoAsserts asserts : PojoAsserts.values()) {
                final DynamoDbMapperFieldModel<T, Object> field = model.field(asserts.name());
                assertNotNull(field);
                assertFieldKeyType(asserts.keyType, field, model);
                assertEquals(asserts.generateStrategy, field.getGenerateStrategy());
                assertEquals(0, field.localSecondaryIndexNames().size());
            }
            assertEquals(PojoAsserts.values().length, model.fields().size());
        }
    }

    @DynamoDbDocument
    @DynamoDbTable(tableName = "")
    public static class TableAndDocument extends AutoKeyAndVal<String> {
        public String getVal() {
            return super.getVal();
        }

        public void setVal(final String val) {
            super.setVal(val);
        }
    }

    @DynamoDbTable(tableName = "")
    private abstract static class SuperGenericClass<T> {
        private String id;

        @DynamoDbHashKey
        public final String getId() {
            return this.id;
        }

        public final void setId(String id) {
            this.id = id;
        }

        public abstract T getT();

        public abstract void setT(T t);
    }

    @DynamoDbTable(tableName = "GenericString")
    private static class SubClass extends SuperGenericClass<Integer> {
        private Integer t;

        @Override
        public Integer getT() {
            return t;
        }

        @Override
        public void setT(Integer t) {
            this.t = t;
        }
    }

    @DynamoDbTable(tableName = "table")
    private static class BaseTablePojo {
        @DynamoDbHashKey
        private String hashKeyOnField;
        private String rangeKeyOnGetter;
        private String attrNoAnnotation;
        @DynamoDbIgnore
        private String ignoredAttr;

        public String getHashKeyOnField() {
            return hashKeyOnField;
        }

        public void setHashKeyOnField(String hashKeyOnField) {
            this.hashKeyOnField = hashKeyOnField;
        }

        @DynamoDbRangeKey
        public String getRangeKeyOnGetter() {
            return rangeKeyOnGetter;
        }

        public void setRangeKeyOnGetter(String rangeKeyOnGetter) {
            this.rangeKeyOnGetter = rangeKeyOnGetter;
        }

        public String getAttrNoAnnotation() {
            return attrNoAnnotation;
        }

        public void setAttrNoAnnotation(String attrNoAnnotation) {
            this.attrNoAnnotation = attrNoAnnotation;
        }

        public String getIgnoredAttr() {
            return ignoredAttr;
        }

        public void setIgnoredAttr(String ignoredAttr) {
            this.ignoredAttr = ignoredAttr;
        }
    }

    @DynamoDbTable(tableName = "table")
    private static class TablePojoSubclass extends BaseTablePojo {
        private String ignoredAttr;

        @Override
        public String getIgnoredAttr() {
            return ignoredAttr;
        }

        @Override
        public void setIgnoredAttr(String ignoredAttr) {
            this.ignoredAttr = ignoredAttr;
        }
    }

    /**
     * A POJO model that uses getter annotations.
     */
    @DynamoDbTable(tableName = "table")
    private static class PojoWithGetterAnnotations {
        private String hashKey;
        private String rangeKey;
        private String indexHashKey;
        private String indexRangeKey;
        private String annotatedAttr;
        private Long versionedAttr;
        private String marshallingAttr;
        private String ignoredAttr;

        @DynamoDbHashKey
        public String getHashKey() {
            return hashKey;
        }

        public void setHashKey(String hashKey) {
            this.hashKey = hashKey;
        }

        @DynamoDbRangeKey
        @DynamoDbAutoGeneratedKey
        public String getRangeKey() {
            return rangeKey;
        }

        public void setRangeKey(String rangeKey) {
            this.rangeKey = rangeKey;
        }

        @DynamoDbIndexHashKey(globalSecondaryIndexName = "index")
        public String getIndexHashKey() {
            return indexHashKey;
        }

        public void setIndexHashKey(String indexHashKey) {
            this.indexHashKey = indexHashKey;
        }

        @DynamoDbIndexRangeKey(globalSecondaryIndexName = "index")
        public String getIndexRangeKey() {
            return indexRangeKey;
        }

        public void setIndexRangeKey(String indexRangeKey) {
            this.indexRangeKey = indexRangeKey;
        }

        @DynamoDbAttribute(attributeName = "actualAttrName")
        public String getAnnotatedAttr() {
            return annotatedAttr;
        }

        public void setAnnotatedAttr(String annotatedAttr) {
            this.annotatedAttr = annotatedAttr;
        }

        @DynamoDbVersionAttribute
        public Long getVersionedAttr() {
            return versionedAttr;
        }

        public void setVersionedAttr(Long versionedAttr) {
            this.versionedAttr = versionedAttr;
        }

        @DynamoDbTypeConverted(converter = RandomUuidMarshaller.class)
        public String getMarshallingAttr() {
            return marshallingAttr;
        }

        public void setMarshallingAttr(String marshallingAttr) {
            this.marshallingAttr = marshallingAttr;
        }

        @DynamoDbIgnore
        public String getIgnoredAttr() {
            return ignoredAttr;
        }

        public void setIgnoredAttr(String ignoredAttr) {
            this.ignoredAttr = ignoredAttr;
        }
    }

    /**
     * The same model as defined in PojoWithGetterAnnotations, but uses field
     * annotations instead.
     */
    @DynamoDbTable(tableName = "table")
    private static class PojoWithFieldAnnotations {
        @DynamoDbHashKey
        private String hashKey;
        @DynamoDbRangeKey
        @DynamoDbAutoGeneratedKey
        private String rangeKey;
        @DynamoDbIndexHashKey(globalSecondaryIndexName = "index")
        private String indexHashKey;
        @DynamoDbIndexRangeKey(globalSecondaryIndexName = "index")
        private String indexRangeKey;
        @DynamoDbAttribute(attributeName = "actualAttrName")
        private String annotatedAttr;
        @DynamoDbVersionAttribute
        private Long versionedAttr;
        @DynamoDbTypeConverted(converter = RandomUuidMarshaller.class)
        private String marshallingAttr;
        @DynamoDbIgnore
        private String ignoredAttr;

        public String getHashKey() {
            return hashKey;
        }

        public void setHashKey(String hashKey) {
            this.hashKey = hashKey;
        }

        public String getRangeKey() {
            return rangeKey;
        }

        public void setRangeKey(String rangeKey) {
            this.rangeKey = rangeKey;
        }

        public String getIndexHashKey() {
            return indexHashKey;
        }

        public void setIndexHashKey(String indexHashKey) {
            this.indexHashKey = indexHashKey;
        }

        public String getIndexRangeKey() {
            return indexRangeKey;
        }

        public void setIndexRangeKey(String indexRangeKey) {
            this.indexRangeKey = indexRangeKey;
        }

        public String getAnnotatedAttr() {
            return annotatedAttr;
        }

        public void setAnnotatedAttr(String annotatedAttr) {
            this.annotatedAttr = annotatedAttr;
        }

        public Long getVersionedAttr() {
            return versionedAttr;
        }

        public void setVersionedAttr(Long versionedAttr) {
            this.versionedAttr = versionedAttr;
        }

        public String getMarshallingAttr() {
            return marshallingAttr;
        }

        public void setMarshallingAttr(String marshallingAttr) {
            this.marshallingAttr = marshallingAttr;
        }

        public String getIgnoredAttr() {
            return ignoredAttr;
        }

        public void setIgnoredAttr(String ignoredAttr) {
            this.ignoredAttr = ignoredAttr;
        }
    }

    /**
     * The same model as defined in PojoWithGetterAnnotations, but uses both getter and field
     * annotations.
     */
    @DynamoDbTable(tableName = "table")
    private static class PojoWithMixedAnnotations {
        @DynamoDbHashKey
        private String hashKey;
        private String rangeKey;
        @DynamoDbIndexHashKey(globalSecondaryIndexName = "index")
        private String indexHashKey;
        private String indexRangeKey;
        @DynamoDbAttribute(attributeName = "actualAttrName")
        private String annotatedAttr;
        private Long versionedAttr;
        @DynamoDbTypeConverted(converter = RandomUuidMarshaller.class)
        private String marshallingAttr;
        private String ignoredAttr;

        public String getHashKey() {
            return hashKey;
        }

        public void setHashKey(String hashKey) {
            this.hashKey = hashKey;
        }

        @DynamoDbRangeKey
        @DynamoDbAutoGeneratedKey
        public String getRangeKey() {
            return rangeKey;
        }

        public void setRangeKey(String rangeKey) {
            this.rangeKey = rangeKey;
        }

        public String getIndexHashKey() {
            return indexHashKey;
        }

        public void setIndexHashKey(String indexHashKey) {
            this.indexHashKey = indexHashKey;
        }

        @DynamoDbIndexRangeKey(globalSecondaryIndexName = "index")
        public String getIndexRangeKey() {
            return indexRangeKey;
        }

        public void setIndexRangeKey(String indexRangeKey) {
            this.indexRangeKey = indexRangeKey;
        }

        public String getAnnotatedAttr() {
            return annotatedAttr;
        }

        public void setAnnotatedAttr(String annotatedAttr) {
            this.annotatedAttr = annotatedAttr;
        }

        @DynamoDbVersionAttribute
        public Long getVersionedAttr() {
            return versionedAttr;
        }

        public void setVersionedAttr(Long versionedAttr) {
            this.versionedAttr = versionedAttr;
        }

        public String getMarshallingAttr() {
            return marshallingAttr;
        }

        public void setMarshallingAttr(String marshallingAttr) {
            this.marshallingAttr = marshallingAttr;
        }

        @DynamoDbIgnore
        public String getIgnoredAttr() {
            return ignoredAttr;
        }

        public void setIgnoredAttr(String ignoredAttr) {
            this.ignoredAttr = ignoredAttr;
        }
    }

    public abstract class AbstractNonMappedInheritedProperties {
        private String doNotUse;

        public String getDoNotUse() {
            return this.doNotUse;
        }

        public void setDoNotUse(final String doNotUse) {
            this.doNotUse = doNotUse;
        }
    }

    @DynamoDbTable(tableName = "aws-java-sdk-test")
    public class NonMappedInheritedProperties extends AbstractNonMappedInheritedProperties {
        private String id;
        private String doUse;

        @DynamoDbHashKey
        public final String getId() {
            return this.id;
        }

        public final void setId(String id) {
            this.id = id;
        }

        public String getDoUse() {
            return this.doUse;
        }

        public void setDoUse(final String doUse) {
            this.doUse = doUse;
        }
    }

}
