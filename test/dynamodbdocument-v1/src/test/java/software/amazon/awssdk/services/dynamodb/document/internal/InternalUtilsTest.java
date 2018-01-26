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

package software.amazon.awssdk.services.dynamodb.document.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.services.dynamodb.document.internal.InternalUtils.toAttributeValues;
import static software.amazon.awssdk.services.dynamodb.document.internal.InternalUtils.toItemList;
import static software.amazon.awssdk.services.dynamodb.document.internal.InternalUtils.toSimpleList;
import static software.amazon.awssdk.services.dynamodb.document.internal.InternalUtils.toSimpleValue;
import static software.amazon.awssdk.services.dynamodb.document.internal.InternalUtils.valToString;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.document.Expected;
import software.amazon.awssdk.services.dynamodb.document.Item;
import software.amazon.awssdk.services.dynamodb.document.KeyAttribute;
import software.amazon.awssdk.services.dynamodb.document.PrimaryKey;
import software.amazon.awssdk.services.dynamodb.document.utils.FluentHashSet;
import software.amazon.awssdk.services.dynamodb.document.utils.ValueMap;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ExpectedAttributeValue;

public class InternalUtilsTest {

    @Test
    public void nullInput() {
        assertTrue(toItemList(null).size() == 0);
        assertNull(toAttributeValues((Item) null));
        assertNull(toSimpleList(null));
        assertNull(toSimpleValue(null));
        assertNull(valToString(null));
    }

    @Test
    @Ignore // Does not pass anymore because the builder will create a duplicate of the BB
    public void toAttributeValue_ByteBuffer() {
        ByteBuffer bbFrom = ByteBuffer.allocate(10);
        AttributeValue av = InternalUtils.toAttributeValue(bbFrom);
        ByteBuffer bbTo = av.b();
        assertSame(bbFrom, bbTo);
    }

    @Test
    public void toAttributeValue_byteArray() {
        byte[] bytesFrom = {1, 2, 3, 4};
        AttributeValue av = InternalUtils.toAttributeValue(bytesFrom);
        ByteBuffer bbTo = av.b();
        assertTrue(ByteBuffer.wrap(bytesFrom).compareTo(bbTo) == 0);
    }

    @Test
    public void toAttributeValue_Number() {
        {
            AttributeValue av = InternalUtils.toAttributeValue(123);
            String num = av.n();
            assertEquals("123", num);
        }
        {   // 17 decimal places
            AttributeValue av = InternalUtils.toAttributeValue(0.99999999999999999);
            String num = av.n();
            assertEquals("1.0", num);
        }
        {   // 16 decimal places
            AttributeValue av = InternalUtils.toAttributeValue(0.9999999999999999);
            String num = av.n();
            assertEquals("0.9999999999999999", num);
        }
        {
            String numFrom = "0.99999999999999999999999999999999999999";
            AttributeValue av = InternalUtils.toAttributeValue(
                    new BigDecimal(numFrom));
            String numTo = av.n();
            assertEquals(numFrom, numTo);
        }
    }

    @Test
    public void toAttributeValue_emptySet() {
        AttributeValue av = InternalUtils.toAttributeValue(new HashSet<Object>());
        List<String> ss = av.ss();
        assertTrue(ss.size() == 0);
        assertNull(av.ns());
    }

    @Test
    public void toAttributeValue_NumberSet() {
        Set<Number> nsFrom = new FluentHashSet<Number>()
                .with(123)
                .with(123.45)
                .with(Integer.valueOf(678))
                .with(new BigInteger("1234567890123456789012345678901234567890"))
                .with(new BigDecimal("0.99999999999999999999999999999999999999"));
        AttributeValue av = InternalUtils.toAttributeValue(nsFrom);
        assertNull(av.ss());
        List<String> ns = av.ns();
        assertTrue(ns.size() == 5);
        assertTrue(ns.contains("123"));
        assertTrue(ns.contains("123.45"));
        assertTrue(ns.contains("678"));
        assertTrue(ns.contains("1234567890123456789012345678901234567890"));
        assertTrue(ns.contains("0.99999999999999999999999999999999999999"));
    }

    @Test
    public void toAttributeValue_ByteArraySet() {
        byte[] ba1From = new byte[] {1, 2, 3};
        byte[] ba2From = new byte[] {4, 5, 6};
        Set<byte[]> nsFrom = new FluentHashSet<byte[]>()
                .with(ba1From)
                .with(ba2From);
        AttributeValue av = InternalUtils.toAttributeValue(nsFrom);
        assertNull(av.ss());
        List<ByteBuffer> bs = av.bs();
        assertTrue(bs.size() == 2);
        boolean bool1 = false;
        boolean bool2 = false;
        for (ByteBuffer b : bs) {
            if (ByteBuffer.wrap(ba1From).compareTo(b) == 0) {
                bool1 = true;
            } else if (ByteBuffer.wrap(ba2From).compareTo(b) == 0) {
                bool2 = true;
            }
        }
        assertTrue(bool1);
        assertTrue(bool2);
    }

    @Test
    public void toAttributeValue_ByteBufferSet() {
        byte[] ba1From = new byte[] {1, 2, 3};
        byte[] ba2From = new byte[] {4, 5, 6};
        Set<ByteBuffer> nsFrom = new FluentHashSet<ByteBuffer>()
                .with(ByteBuffer.wrap(ba1From))
                .with(ByteBuffer.wrap(ba2From));
        AttributeValue av = InternalUtils.toAttributeValue(nsFrom);
        assertNull(av.ss());
        List<ByteBuffer> bs = av.bs();
        assertTrue(bs.size() == 2);
        boolean bool1 = false;
        boolean bool2 = false;
        for (ByteBuffer b : bs) {
            if (ByteBuffer.wrap(ba1From).compareTo(b) == 0) {
                bool1 = true;
            } else if (ByteBuffer.wrap(ba2From).compareTo(b) == 0) {
                bool2 = true;
            }
        }
        assertTrue(bool1);
        assertTrue(bool2);
    }

    @Test
    public void toAttributeValue_null() {
        AttributeValue av = InternalUtils.toAttributeValue(null);
        assertEquals(Boolean.TRUE, av.nul());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void toAttributeValue_UnsupportedOperationException() {
        InternalUtils.toAttributeValue(new Object());
    }

    @Test
    public void toAttributeValue_emptyMap() {
        AttributeValue av = InternalUtils.toAttributeValue(new HashMap<String, String>());
        Map<String, AttributeValue> m = av.m();
        assertTrue(m.size() == 0);
    }

    @Test
    public void toAttributeValue_emptyList() {
        AttributeValue av = InternalUtils.toAttributeValue(new ArrayList<String>());
        List<AttributeValue> l = av.l();
        assertTrue(l.size() == 0);
    }

    @Test
    public void toAttributeValue_MapOfMap() {
        AttributeValue av = InternalUtils.toAttributeValue(new ValueMap()
                                                                   .with("emptyMap", new ValueMap()));
        Map<String, AttributeValue> m = av.m();
        assertTrue(m.size() == 1);
        AttributeValue emptyMap = m.get("emptyMap");
        Map<String, AttributeValue> mInner = emptyMap.m();
        assertTrue(0 == mInner.size());
    }

    @Test
    public void toSimpleListValue_empty() {
        List<AttributeValue> listFrom = new ArrayList<AttributeValue>();
        List<Object> listTo = toSimpleList(listFrom);
        assertTrue(listTo.size() == 0);
    }

    @Test
    public void toSimpleListValue_null() {
        assertNull(InternalUtils.toSimpleListValue(null));
    }

    @Test
    public void toSimpleListValue() {
        List<AttributeValue> listFrom = new ArrayList<AttributeValue>();
        listFrom.add(AttributeValue.builder().s("test").build());
        listFrom.add(AttributeValue.builder().n("123").build());
        List<Object> listTo = InternalUtils.toSimpleListValue(listFrom);
        assertTrue(listTo.size() == 2);
        assertEquals("test", listTo.get(0));
        assertEquals(new BigDecimal("123"), listTo.get(1));
    }

    @Test
    public void toSimpleValue_null() {
        assertNull(toSimpleValue(null));
        assertNull(toSimpleValue(AttributeValue.builder().nul(Boolean.TRUE).build()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void toSimpleValue_empty() {
        toSimpleValue(AttributeValue.builder().build());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void toSimpleValue_FalseNull() {
        toSimpleValue(AttributeValue.builder().nul(Boolean.FALSE).build());
    }

    @Test
    public void toSimpleValue_NS() {
        Set<BigDecimal> numset = toSimpleValue(
                AttributeValue.builder().ns("123", "456").build());
        assertTrue(numset.size() == 2);
        assertTrue(numset.contains(new BigDecimal("123")));
        assertTrue(numset.contains(new BigDecimal("456")));
    }

    @Test
    public void toSimpleValue_emptyNS() {
        Set<BigDecimal> numset = toSimpleValue(
                AttributeValue.builder().ns(new ArrayList<String>()).build());
        assertTrue(numset.size() == 0);
    }

    @Test
    public void toSimpleValue_M() {
        Map<String, AttributeValue> mapFrom = new HashMap<String, AttributeValue>();
        mapFrom.put("fooBOOL", AttributeValue.builder().bool(Boolean.TRUE).build());
        mapFrom.put("fooString", AttributeValue.builder().s("bar").build());
        Map<String, Object> mapTo = toSimpleValue(
                AttributeValue.builder().m(mapFrom).build());
        assertTrue(mapTo.size() == 2);
        assertEquals(Boolean.TRUE, mapTo.get("fooBOOL"));
        assertEquals("bar", mapTo.get("fooString"));
    }

    @Test
    public void toSimpleValue_emptyM() {
        Map<String, AttributeValue> mapFrom = new HashMap<String, AttributeValue>();
        Map<String, Object> mapTo = toSimpleValue(
                AttributeValue.builder().m(mapFrom).build());
        assertTrue(mapTo.size() == 0);
    }

    @Test
    public void toSimpleValue_ByteArray() {
        byte[] bytesFrom = new byte[] {1, 2, 3};
        ByteBuffer byteBufferTo = ByteBuffer.allocate(3).put(bytesFrom);
        byteBufferTo.rewind();
        byte[] bytesTo = toSimpleValue(
                AttributeValue.builder().b(byteBufferTo).build());
        assertTrue(Arrays.equals(bytesTo, bytesFrom));
    }

    @Test
    public void toSimpleValue_DirectByteBuffer() {
        byte[] bytesFrom = new byte[] {1, 2, 3};
        ByteBuffer byteBufferTo = ByteBuffer.allocateDirect(3).put(bytesFrom);
        byteBufferTo.rewind();
        byte[] bytesTo = toSimpleValue(
                AttributeValue.builder().b(byteBufferTo).build());
        assertTrue(Arrays.equals(bytesTo, bytesFrom));
    }

    @Test(expected = IllegalArgumentException.class)
    public void toExpectedAttributeValueMap_missingComparisonOperator() {
        InternalUtils.toExpectedAttributeValueMap(Arrays.asList(new Expected("attrName")));
    }

    @Test
    public void toExpectedAttributeValueMap() {
        Map<String, ExpectedAttributeValue> to =
                InternalUtils.toExpectedAttributeValueMap(Arrays.asList(
                        new Expected("attr1").exists(),
                        new Expected("attr2").exists()
                                                                       ));
        assertTrue(to.size() == 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toExpectedAttributeValueMap_duplicateAttributeNames() {
        InternalUtils.toExpectedAttributeValueMap(Arrays.asList(
                new Expected("attr1").exists(),
                new Expected("attr1").ge(1)
                                                               ));
    }

    @Test
    public void toAttributeValueMap_nullKeyAttributeCollection() {
        assertNull(InternalUtils.toAttributeValueMap((Collection<KeyAttribute>) null));
    }

    @Test
    public void toAttributeValueMap_nullPrimaryKey() {
        assertNull(InternalUtils.toAttributeValueMap((PrimaryKey) null));
    }

    @Test
    public void toAttributeValueMap_nullKeyAttributes() {
        assertNull(InternalUtils.toAttributeValueMap((KeyAttribute[]) null));
    }

    @Test
    public void toAttributeValueMap_KeyAttributes() {
        Map<String, AttributeValue> map = InternalUtils.toAttributeValueMap(
                new KeyAttribute("hashname", "hashvalue"),
                new KeyAttribute("rangekey", 123));
        AttributeValue av = map.get("hashname");
        assertEquals("hashvalue", av.s());
        av = map.get("rangekey");
        assertEquals("123", av.n());
    }

    @Test
    public void valToString_int() {
        String s = valToString(123.456);
        assertEquals("123.456", s);
    }
}
