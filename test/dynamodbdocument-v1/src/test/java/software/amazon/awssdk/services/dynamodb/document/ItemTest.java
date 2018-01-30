/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.document.utils.FluentArrayList;
import software.amazon.awssdk.services.dynamodb.document.utils.FluentHashSet;
import software.amazon.awssdk.services.dynamodb.document.utils.ValueMap;
import software.amazon.awssdk.utils.Base64Utils;

public class ItemTest {

    @Test
    public void jsonDoubleMax() {
        double[] values = {
                Double.MAX_VALUE, Double.MIN_NORMAL, Double.MIN_VALUE, Double.MIN_NORMAL
        };
        for (double val : values) {
            doJsonDoubleTest(val);
        }
    }

    private void doJsonDoubleTest(double value) {
        Item item1 = new Item().withDouble("double", value);
        final String json = item1.toJsonPretty();
        System.out.println(json);
        Item item2 = Item.fromJson(json);
        assertEquals(json, item2.toJsonPretty());
    }

    @Test
    public void isNull() {
        Item item = new Item();
        assertFalse(item.isNull("test"));

        item.withNull("test");
        assertTrue(item.isNull("test"));

        item.removeAttribute("test");
        assertFalse(item.isNull("test"));

        item.withString("test", "foo");
        assertFalse(item.isNull("test"));
        assertEquals("foo", item.getString("test"));
    }

    @Test
    public void is_null() {
        Item item = new Item();
        assertFalse(item.isNull("test"));

        item.with("test", null);
        assertTrue(item.isNull("test"));

        assertNull(item.get("test"));

        item.removeAttribute("test");
        assertFalse(item.isNull("test"));

        assertNull(item.get("test"));
    }

    @Test
    public void isPresent() {
        Item item = new Item();
        assertFalse(item.isPresent("test"));

        item.withNull("test");
        assertTrue(item.isPresent("test"));

        item.removeAttribute("test");
        assertFalse(item.isPresent("test"));

        item.withString("test", "foo");
        assertTrue(item.isPresent("test"));
    }

    @Test
    public void toBigDecimal_Null() {
        Item item = new Item();
        assertNull(item.getNumber("test"));
    }

    @Test(expected = NumberFormatException.class)
    public void getInt_Null() {
        Item item = new Item();
        item.getInt("test");
    }

    @Test(expected = NumberFormatException.class)
    public void getLong_Null() {
        Item item = new Item();
        item.getLong("test");
    }

    @Test(expected = NumberFormatException.class)
    public void getNumber_NonNumber() {
        Item item = new Item();
        item.withString("test", "foo");
        item.getNumber("test");
    }

    @Test
    public void withNumber() {
        Item item = new Item();
        item.withNumber("test", BigDecimal.ONE);
        assertSame(BigDecimal.ONE, item.getNumber("test"));
        assertTrue(1 == item.getInt("test"));
        assertTrue(1L == item.getLong("test"));
    }

    @Test
    public void withLong() {
        Item item = new Item();
        item.withLong("test", 123L);
        assertTrue(123L == item.getLong("test"));
    }

    @Test
    public void toByteArray() {
        Item item = new Item();
        assertNull(item.getBinary("test"));
        byte[] bytes = {1, 2, 3};
        item.withBinary("test", bytes);
        assertTrue(Arrays.equals(bytes, item.getBinary("test")));
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        item.withBinary("test", bb);
        assertTrue(byte[].class == item.getTypeOf("test"));
        assertTrue(Arrays.equals(bytes, item.getBinary("test")));
        assertTrue(Arrays.equals(bytes, item.getByteBuffer("test").array()));
    }

    @Test(expected = IncompatibleTypeException.class)
    public void toByteArray_IncompatibleTypeException() {
        Item item = new Item();
        item.withString("test", "foo");
        item.getBinary("test");
    }

    @Test
    public void toByteBuffer() {
        Item item = new Item();
        assertNull(item.getByteBuffer("test"));
        byte[] bytes = {1, 2, 3};
        item.withBinary("test", ByteBuffer.wrap(bytes));
        ByteBuffer toByteBuffer = item.getByteBuffer("test");
        assertTrue(Arrays.equals(bytes, toByteBuffer.array()));
        assertTrue(Arrays.equals(bytes, item.getBinary("test")));
        item.withBinary("test", bytes);
        assertSame(byte[].class, item.getTypeOf("test"));
        assertTrue(Arrays.equals(bytes, item.getByteBuffer("test").array()));
    }

    @Test(expected = IncompatibleTypeException.class)
    public void toByteBuffer_IncompatibleTypeException() {
        Item item = new Item();
        item.withString("test", "foo");
        item.getByteBuffer("test");
    }

    @Test
    public void valToString() {
        Item item = new Item();
        item.withNumber("test", BigDecimal.ONE);
        assertEquals("1", item.getString("test"));
        assertNull(item.getString("foo"));
        item.withBoolean("test", false);
        assertEquals("false", item.getString("test"));
    }

    @Test
    public void getStringSet_fromList() {
        Item item = new Item();
        item.withList("test", "a", "b", "c");
        Set<String> ss = item.getStringSet("test");
        assertTrue(ss.size() == 3);
        assertTrue(ss.contains("a"));
        assertTrue(ss.contains("b"));
        assertTrue(ss.contains("c"));
    }

    @Test
    public void getStringSet_fromNumbers() {
        Item item = new Item();
        item.withNumberSet("test", 1, 2);
        Set<String> ss = item.getStringSet("test");
        assertTrue(ss.size() == 2);
        assertTrue(ss.contains("1"));
        assertTrue(ss.contains("2"));
    }

    @Test
    public void getStringSet_fromBooleans() {
        Item item = new Item();
        item.withList("test", true, false);
        Set<String> ss = item.getStringSet("test");
        assertTrue(ss.size() == 2);
        assertTrue(ss.contains("true"));
        assertTrue(ss.contains("false"));
    }

    @Test
    public void getStringSet_fromBoolean() {
        Item item = new Item();
        item.withBoolean("test", true);
        Set<String> ss = item.getStringSet("test");
        assertTrue(ss.size() == 1);
        assertTrue(ss.contains("true"));
    }

    @Test(expected = IncompatibleTypeException.class)
    public void getStringSet_fromBinary() {
        Item item = new Item();
        item.withBinary("test", new byte[] {1, 2});
        item.getStringSet("test");
    }

    @Test
    public void getStringSet_empty() {
        Item item = new Item();
        item.with("test", new FluentArrayList<String>());
        Set<String> ss = item.getStringSet("test");
        assertTrue(ss.size() == 0);
    }

    @Test(expected = IncompatibleTypeException.class)
    public void getStringSet_duplicateElements() {
        Item item = new Item();
        item.withList("test", "a", "b", "a");
        item.getStringSet("test");
    }

    @Test
    public void getStringSet_nullElement() {
        Item item = new Item();
        item.withList("test", "a", null, "c");
        Set<String> ss = item.getStringSet("test");
        assertTrue(ss.size() == 3);
        assertTrue(ss.contains("a"));
        assertTrue(ss.contains(null));
        assertTrue(ss.contains("c"));
    }

    @Test
    public void getNumberSet() {
        Item item = new Item();
        assertNull(item.getNumberSet("test"));
        item.withList("test", BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN);
        Set<BigDecimal> ss = item.getNumberSet("test");
        assertTrue(ss.size() == 3);
        assertTrue(ss.contains(BigDecimal.ZERO));
        assertTrue(ss.contains(BigDecimal.ONE));
        assertTrue(ss.contains(BigDecimal.TEN));
    }

    @Test
    public void getNumberSet_number() {
        Item item = new Item();
        item.withNumber("test", 123);
        Set<BigDecimal> ss = item.getNumberSet("test");
        assertTrue(ss.size() == 1);
        assertTrue(ss.contains(new BigDecimal("123")));
    }

    @Test
    public void getNumberSet_string() {
        Item item = new Item();
        item.withString("test", "123");
        Set<BigDecimal> ss = item.getNumberSet("test");
        assertTrue(ss.size() == 1);
        assertTrue(ss.contains(new BigDecimal("123")));
    }

    @Test
    public void getNumberSet_empty() {
        Item item = new Item();
        item.with("test", new FluentArrayList<BigDecimal>());
        Set<BigDecimal> ss = item.getNumberSet("test");
        assertTrue(ss.size() == 0);
    }

    @Test(expected = IncompatibleTypeException.class)
    public void getNumberSet_duplicateElements() {
        Item item = new Item();
        item.withList("test", BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO);
        item.getNumberSet("test");
    }

    @Test
    public void getNumberSet_nullElement() {
        Item item = new Item();
        item.withList("test", BigDecimal.ZERO, null, BigDecimal.TEN);
        Set<BigDecimal> ss = item.getNumberSet("test");
        assertTrue(ss.size() == 3);
        assertTrue(ss.contains(BigDecimal.ZERO));
        assertTrue(ss.contains(null));
        assertTrue(ss.contains(BigDecimal.TEN));
    }

    @Test
    public void getBinarySet_bytes() {
        Item item = new Item();
        assertNull(item.getBinarySet("test"));
        item.withList("test", new byte[] {1, 2}, new byte[] {3, 4});
        Set<byte[]> bas = item.getBinarySet("test");
        assertTrue(bas.size() == 2);
        boolean a = false;
        boolean b = false;
        for (byte[] ba : bas) {
            if (Arrays.equals(ba, new byte[] {1, 2})) {
                a = true;
            } else if (Arrays.equals(ba, new byte[] {3, 4})) {
                b = true;
            }
        }
        assertTrue(a);
        assertTrue(b);
    }

    @Test
    public void getBinarySet_singleByteArray() {
        Item item = new Item();
        item.with("test", new byte[] {1, 2});
        Set<byte[]> bs = item.getBinarySet("test");
        assertTrue(bs.size() == 1);
        boolean a = false;
        for (byte[] ba : bs) {
            if (Arrays.equals(ba, new byte[] {1, 2})) {
                a = true;
            }
        }
        assertTrue(a);

        Set<ByteBuffer> bbs = item.getByteBufferSet("test");
        assertTrue(bbs.size() == 1);
        a = false;
        for (ByteBuffer ba : bbs) {
            if (Arrays.equals(ba.array(), new byte[] {1, 2})) {
                a = true;
            }
        }
        assertTrue(a);
    }

    @Test
    public void getBinarySet_singleByteBuffer() {
        Item item = new Item();
        item.with("test", ByteBuffer.wrap(new byte[] {1, 2}));
        Set<ByteBuffer> bbs = item.getByteBufferSet("test");
        assertTrue(bbs.size() == 1);
        boolean a = false;
        for (ByteBuffer ba : bbs) {
            if (Arrays.equals(ba.array(), new byte[] {1, 2})) {
                a = true;
            }
        }
        assertTrue(a);
        Set<byte[]> bs = item.getBinarySet("test");
        assertTrue(bs.size() == 1);
        a = false;
        for (byte[] ba : bs) {
            if (Arrays.equals(ba, new byte[] {1, 2})) {
                a = true;
            }
        }
        assertTrue(a);
    }

    @Test
    public void getBinarySet_empty() {
        Item item = new Item();
        item.with("test", new FluentHashSet<byte[]>());
        Set<byte[]> bs = item.getBinarySet("test");
        assertTrue(bs.size() == 0);

        Set<ByteBuffer> bbs = item.getByteBufferSet("test");
        assertTrue(bbs.size() == 0);
    }

    @Test(expected = IncompatibleTypeException.class)
    public void getBinarySet_Incompatible() {
        Item item = new Item();
        item.withString("test", "foo");
        item.getBinarySet("test");
    }

    @Test
    public void getByteBufferSet_empty() {
        Item item = new Item();
        assertNull(item.getByteBufferSet("test"));
        item.with("test", new FluentHashSet<ByteBuffer>());
        Set<byte[]> bs = item.getBinarySet("test");
        assertTrue(bs.size() == 0);

        Set<ByteBuffer> bbs = item.getByteBufferSet("test");
        assertTrue(bbs.size() == 0);
    }

    @Test(expected = IncompatibleTypeException.class)
    public void getByteBufferSet_Incompatible() {
        Item item = new Item();
        item.withString("test", "foo");
        item.getByteBufferSet("test");
    }

    @Test
    public void getByteBufferSet() {
        Item item = new Item();
        item.withList("test", new byte[] {1, 2, 3}, new byte[] {4, 5, 6});
        Set<ByteBuffer> bs = item.getByteBufferSet("test");
        assertTrue(bs.size() == 2);
        boolean a = false, b = false;
        for (ByteBuffer bb : bs) {
            if (Arrays.equals(bb.array(), new byte[] {1, 2, 3})) {
                a = true;
            } else if (Arrays.equals(bb.array(), new byte[] {4, 5, 6})) {
                b = true;
            }
        }
        assertTrue(a);
        assertTrue(b);
    }

    @Test
    public void getList_null() {
        Item item = new Item();
        assertNull(item.getList("test"));
    }

    @Test
    public void getList_list() {
        Item item = new Item().withList("test", "abc", "def");
        List<String> list = item.getList("test");
        assertTrue(list.size() == 2);
        assertEquals("abc", list.get(0));
        assertEquals("def", list.get(1));
    }

    @Test
    public void getList_string() {
        Item item = new Item().withString("test", "foo");
        List<String> list = item.getList("test");
        assertTrue(list.size() == 1);
        assertEquals("foo", list.get(0));
    }

    @Test
    public void toJSON_null() {
        assertNull(new Item().getJson("test"));
        assertNull(new Item().getJsonPretty("test"));
    }

    @Test
    public void fromJSON_null() {
        assertNull(Item.fromJson(null));
    }

    @Test
    public void fromJSON_array() {
        Item item = new Item()
                .withJson("arrayJson", "[\"foo\", \"bar\"]");
        List<String> arrayJson = item.getList("arrayJson");
        String[] expectedArray = new String[] {"foo", "bar"};
        Assert.assertArrayEquals(expectedArray, arrayJson.toArray());
    }

    @Test
    public void fromJSON_map() {
        Item item = new Item()
                .withJson("mapJson", "{\"foo\": \"bar\"}");
        Map<String, String> mapJson = item.getMap("mapJson");
        Assert.assertEquals("bar", mapJson.get("foo"));
    }

    @Test
    public void toFromJSON() {
        Item item = new Item()
                .withString("stringA", "stringV")
                .withFloat("floatA", 123.45f)
                // Jackson will convert byte[] into Base64-encoded binary data
                .withBinary("binaryA", new byte[] {1, 2, 3})
                .withBoolean("booleanA", true)
                .withNull("nullA")
                .withJson("jsonA", "{\"myjson\": 321}")
                .withList("listA", "a", "b", "c")
                .withMap("mapA", new ValueMap().with("map-a", "a").with("map-b", "b"))
                .withStringSet("strSetA", "sa", "sb", "sc")
                .withNumberSet("numSetA", BigDecimal.ONE, BigDecimal.ZERO)
                .withBinarySet("binarySetA", new byte[] {00, 11}, new byte[] {22, 33})
                .withBinarySet("byteBufferSetA",
                        ByteBuffer.wrap(new byte[] {44, 55}),
                        ByteBuffer.wrap(new byte[] {66, 77}));
        String json = item.toJsonPretty();
        System.out.println(json);
        System.out.println("byte[]{1,2,3} => " + Base64Utils.encodeAsString(new byte[] {1, 2, 3}));
        System.out.println("byte[]{00,11} => " + Base64Utils.encodeAsString(new byte[] {00, 11}));
        System.out.println("byte[]{22,33} => " + Base64Utils.encodeAsString(new byte[] {22, 33}));
        System.out.println("byte[]{44,44} => " + Base64Utils.encodeAsString(new byte[] {44, 55}));
        System.out.println("byte[]{66,77} => " + Base64Utils.encodeAsString(new byte[] {66, 77}));
        Item itemTo = Item.fromJson(json);
        System.out.println(itemTo);
        assertTrue(List.class.isAssignableFrom(itemTo.getTypeOf("binarySetA")));
        assertTrue(List.class.isAssignableFrom(itemTo.getTypeOf("byteBufferSetA")));
        itemTo.base64Decode("binaryA", "binarySetA", "byteBufferSetA");
        assertTrue(Arrays.equals(itemTo.getBinary("binaryA"), item.getBinary("binaryA")));
        assertTrue(itemTo.getBinarySet("binarySetA").size() == 2);
        {   // verity the binary content of "binarySetA"
            boolean a = false, b = false;
            for (byte[] bytes : itemTo.getBinarySet("binarySetA")) {
                if (Arrays.equals(bytes, new byte[] {00, 11})) {
                    a = true;
                } else if (Arrays.equals(bytes, new byte[] {22, 33})) {
                    b = true;
                }
            }
            assertTrue(a);
            assertTrue(b);
            assertTrue(Set.class.isAssignableFrom(itemTo.getTypeOf("binarySetA")));
        }
        assertTrue(itemTo.getBinarySet("byteBufferSetA").size() == 2);
        {   // verity the binary content of "byteBufferSetA"
            boolean a = false, b = false;
            for (byte[] bytes : itemTo.getBinarySet("byteBufferSetA")) {
                if (Arrays.equals(bytes, new byte[] {44, 55})) {
                    a = true;
                } else if (Arrays.equals(bytes, new byte[] {66, 77})) {
                    b = true;
                }
            }
            assertTrue(a);
            assertTrue(b);
            assertTrue(Set.class.isAssignableFrom(itemTo.getTypeOf("byteBufferSetA")));
        }
        // JSON doesn't support Set, so all all sets now become lists
        assertTrue(List.class.isAssignableFrom(itemTo.getTypeOf("strSetA")));
        assertTrue(List.class.isAssignableFrom(itemTo.getTypeOf("numSetA")));
        itemTo.convertListsToSets("strSetA", "numSetA");
        assertTrue(Set.class.isAssignableFrom(itemTo.getTypeOf("strSetA")));
        assertTrue(Set.class.isAssignableFrom(itemTo.getTypeOf("numSetA")));
        {
            Set<String> set = itemTo.getStringSet("strSetA");
            assertTrue(set.size() == item.getStringSet("strSetA").size());
            set.containsAll(item.getStringSet("strSetA"));
        }
        {
            Set<BigDecimal> set = itemTo.getNumberSet("numSetA");
            assertTrue(set.size() == item.getStringSet("numSetA").size());
            set.containsAll(item.getNumberSet("numSetA"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void withStringSet_duplicates() {
        new Item().withStringSet("test", "a", "b", "a");
    }

    @Test(expected = IllegalArgumentException.class)
    public void withBigDecimalSet_duplicates() {
        new Item().withBigDecimalSet("test", new BigDecimal("1"), BigDecimal.ONE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void withNumberSet_duplicates() {
        new Item().withNumberSet("test", new BigDecimal("1"), new BigInteger("1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void withNumberSet_duplicates2() {
        new Item().withNumberSet("test", new BigDecimal("1.0"), new Float("1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void withNumberSet_duplicates3() {
        Set<Number> set = new FluentHashSet<Number>().withAll(
                new BigDecimal("1.0"), new Float("1"));
        assertTrue(set.size() == 2);
        // Become duplicates when get converted into BigDecimal
        new Item().withNumberSet("test", set);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidNullInput() {
        new Item().withNumber("test", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullAttrName() {
        new Item().withNull(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void blankAttrName() {
        new Item().withNull("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void withKeyComponents_null() {
        new Item().withKeyComponents();
    }

    @Test(expected = IllegalArgumentException.class)
    public void withKeyComponents_nullComponent() {
        new Item().withKeyComponents((KeyAttribute) null);
    }

    @Test
    public void withKeyComponents() {
        Item item = new Item().withKeyComponents(new KeyAttribute("name", 123));
        Assert.assertTrue(123 == item.getInt("name"));
        Assert.assertTrue(BigDecimal.class == item.getTypeOf("name"));
    }

    @Test(expected = IncompatibleTypeException.class)
    public void getBOOL_null() {
        Item item = new Item();
        item.getBool("test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void withPrimaryKey_null() {
        new Item().withPrimaryKey(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void withPrimaryKey_empty() {
        new Item().withPrimaryKey(new PrimaryKey());
    }

    @Test(expected = IncompatibleTypeException.class)
    public void getBOOL_invalidValue() {
        Item item = new Item().withInt("test", 123);
        item.getBool("test");
    }

    @Test
    public void getBOOL_Boolean() {
        Item item = new Item().withBoolean("test", Boolean.TRUE);
        assertEquals(Boolean.TRUE, item.getBool("test"));
        item.withBoolean("test", Boolean.FALSE);
        assertEquals(Boolean.FALSE, item.getBool("test"));
    }

    @Test
    public void getBOOL_01() {
        Item item = new Item().withString("test", "1");
        assertEquals(Boolean.TRUE, item.getBool("test"));
        item.withString("test", "0");
        assertEquals(Boolean.FALSE, item.getBool("test"));
        item.withString("test", "true");
        assertEquals(Boolean.TRUE, item.getBool("test"));
        item.withString("test", "false");
        assertEquals(Boolean.FALSE, item.getBool("test"));
    }

    @Test
    public void withShort() {
        assertTrue(1 == new Item().withShort("test", (short) 1).getInt("test"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void withShort_emptyName() {
        new Item().withShort(" ", (short) 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void withShort_nullName() {
        new Item().withShort(null, (short) 1);
    }

    @Test
    public void withDouble() {
        assertTrue(1 == new Item().withDouble("test", 1.0).getInt("test"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void withDouble_emptyName() {
        new Item().withDouble(" ", 1.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void withDouble_nullName() {
        new Item().withDouble(null, 1.0);
    }

    // https://github.com/aws/aws-sdk-java/issues/311#issuecomment-64474230
    @Test(expected = ClassCastException.class)
    public void issues311() {
        Map<String, BigInteger> bigIntMap_input = new HashMap<String, BigInteger>();
        bigIntMap_input.put("map_key", new BigInteger("123"));

        Item i = new Item().withMap("item_key", bigIntMap_input);
        Map<String, BigInteger> mapout = i.getMap("item_key");
        @SuppressWarnings("unused")
        BigInteger b = mapout.get("map_key");
    }

    @Test
    public void getRawMap() {
        Map<String, BigInteger> bigIntMap_input = new HashMap<String, BigInteger>();
        bigIntMap_input.put("map_key", new BigInteger("123"));

        Item i = new Item().withMap("item_key", bigIntMap_input);
        Map<String, Object> mapout = i.getRawMap("item_key");
        Object b = mapout.get("map_key");
        assertEquals("123", b.toString());
    }

    @Test
    public void getMapOfNumbers_BigInteger() {
        Map<String, BigInteger> bigIntMap_input = new HashMap<String, BigInteger>();
        bigIntMap_input.put("map_key", new BigInteger("123"));

        Item i = new Item().withMap("item_key", bigIntMap_input);
        Map<String, BigInteger> mapout = i.getMapOfNumbers("item_key", BigInteger.class);
        BigInteger b = mapout.get("map_key");
        assertEquals("123", b.toString());
    }

    @Test
    public void getMapOfNumbers_BigDecimal() {
        Map<String, BigInteger> bigIntMap_input = new HashMap<String, BigInteger>();
        bigIntMap_input.put("map_key", new BigInteger("123"));

        Item i = new Item().withMap("item_key", bigIntMap_input);
        Map<String, BigDecimal> mapout = i.getMapOfNumbers("item_key", BigDecimal.class);
        BigDecimal b = mapout.get("map_key");
        assertEquals("123", b.toString());
    }

    @Test
    public void getMapOfNumbers_Short() {
        Map<String, BigInteger> bigIntMap_input = new HashMap<String, BigInteger>();
        bigIntMap_input.put("map_key", new BigInteger("123"));

        Item i = new Item().withMap("item_key", bigIntMap_input);
        Map<String, Short> mapout = i.getMapOfNumbers("item_key", Short.class);
        Short b = mapout.get("map_key");
        assertEquals("123", b.toString());
    }

    @Test
    public void getMapOfNumbers_Integer() {
        Map<String, BigInteger> bigIntMap_input = new HashMap<String, BigInteger>();
        bigIntMap_input.put("map_key", new BigInteger("123"));

        Item i = new Item().withMap("item_key", bigIntMap_input);
        Map<String, Integer> mapout = i.getMapOfNumbers("item_key", Integer.class);
        Integer b = mapout.get("map_key");
        assertEquals("123", b.toString());
    }

    @Test
    public void getMapOfNumbers_Long() {
        Map<String, BigInteger> bigIntMap_input = new HashMap<String, BigInteger>();
        bigIntMap_input.put("map_key", new BigInteger("123"));

        Item i = new Item().withMap("item_key", bigIntMap_input);
        Map<String, Long> mapout = i.getMapOfNumbers("item_key", Long.class);
        Long b = mapout.get("map_key");
        assertEquals("123", b.toString());
    }

    @Test
    public void getMapOfNumbers_Float() {
        Map<String, BigInteger> bigIntMap_input = new HashMap<String, BigInteger>();
        bigIntMap_input.put("map_key", new BigInteger("123"));

        Item i = new Item().withMap("item_key", bigIntMap_input);
        Map<String, Float> mapout = i.getMapOfNumbers("item_key", Float.class);
        Float b = mapout.get("map_key");
        assertEquals(b.toString(), "123.0", b.toString());
    }

    @Test
    public void getMapOfNumbers_Double() {
        Map<String, BigInteger> bigIntMap_input = new HashMap<String, BigInteger>();
        bigIntMap_input.put("map_key", new BigInteger("123"));

        Item i = new Item().withMap("item_key", bigIntMap_input);
        Map<String, Double> mapout = i.getMapOfNumbers("item_key", Double.class);
        Double b = mapout.get("map_key");
        assertEquals(b.toString(), "123.0", b.toString());
    }

    @Test
    public void getMapOfNumbers_Number() {
        Map<String, BigInteger> bigIntMap_input = new HashMap<String, BigInteger>();
        bigIntMap_input.put("map_key", new BigInteger("123"));

        Item i = new Item().withMap("item_key", bigIntMap_input);
        Map<String, Number> mapout = i.getMapOfNumbers("item_key", Number.class);
        Number b = mapout.get("map_key");
        assertEquals("123", b.toString());
    }

    @Test
    public void getMapOfNumbers_NotExist() {
        Item i = new Item();
        assertNull(i.getMapOfNumbers("item_key", Short.class));
    }

    @Test
    public void getBigInteger() {
        Item i = new Item().withInt("item_key", 123);
        BigInteger b = i.getBigInteger("item_key");
        assertEquals("123", b.toString());

        assertNull(i.getBigInteger("foo"));
    }

    @Test
    public void getShort() {
        Item i = new Item().withInt("item_key", 123);
        short b = i.getShort("item_key");
        assertTrue(b == 123);
    }

    @Test(expected = NumberFormatException.class)
    public void getShortNotExist() {
        Item i = new Item();
        i.getShort("item_key");
    }

    @Test
    public void getFloat() {
        Item i = new Item().withFloat("item_key", 123.45f);
        float b = i.getFloat("item_key");
        assertTrue(b == 123.45f);
    }

    @Test(expected = NumberFormatException.class)
    public void getFloatNotExist() {
        Item i = new Item();
        i.getFloat("item_key");
    }

    @Test
    public void getDouble() {
        Item i = new Item().withDouble("item_key", 123.45);
        double b = i.getFloat("item_key");
        assertTrue(b + "", b > 123.44 && b <= 123.45);
    }

    @Test(expected = NumberFormatException.class)
    public void getDoubleNotExist() {
        Item i = new Item();
        i.getDouble("item_key");
    }

    @Test(expected = IllegalArgumentException.class)
    public void withNullBigInteger() {
        new Item().withBigInteger("foo", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void withNullNumber() {
        new Item().withNumber("foo", null);
    }

    @Test
    public void hasAttribute() {
        assertFalse(new Item().hasAttribute("foo"));
        assertTrue(new Item().with("foo", null).hasAttribute("foo"));
        assertTrue(new Item().with("foo", "fooval").hasAttribute("foo"));
        assertTrue(new Item().with("foo", "fooval").with("bar", "barval").hasAttribute("foo"));
        assertTrue(new Item().with("foo", "fooval").with("bar", "barval").hasAttribute("bar"));
        assertFalse(new Item().with("foo", "fooval").with("bar", "barval").hasAttribute("notExist"));
    }

    @Test
    public void testEquals() {
        assertEquals(new Item().with("foo", "fooval").with("bar", "barval"),
                new Item().with("foo", "fooval").with("bar", "barval"));
        assertEquals(new Item().with("foo", "fooval").with("bar", "barval"),
                new Item().withPrimaryKey(new PrimaryKey("foo", "fooval", "bar", "barval")));

        assertFalse(new Item().equals(new Object()));
        assertFalse(new Item().equals(null));

        Set<Item> items = new HashSet<Item>();
        items.add(new Item().with("foo", "fooval").with("bar", "barval"));
        items.add(new Item().with("foo", "fooval"));
        assertTrue(items.size() == 2);

        assertTrue(items.contains(new Item().with("foo", "fooval")));
        assertTrue(items.contains(new Item().with("foo", "fooval").with("bar", "barval")));
        assertFalse(items.contains(new Item()));

        items.add(new Item());
        assertTrue(items.contains(new Item()));
    }
}
