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

package software.amazon.awssdk.services.dynamodb.model;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Condition;
import org.junit.Test;

public class ItemTest {

    public static final byte[] BYTES1 = "value1".getBytes(UTF_8);
    public static final byte[] BYTES2 = "value2".getBytes(UTF_8);

    @Test
    public void testItemBuilder() {

        Map<String, Object> testMap = new HashMap<>();
        testMap.put("String", "stringValue");
        testMap.put("int", 15);
        testMap.put("byteArray", BYTES1);
        testMap.put("ByteBuffer", ByteBuffer.wrap(BYTES2));
        testMap.put("bool", true);

        List<Long> longs = Arrays.asList(1l, 2l, 3l);

        Map<String, AttributeValue> item =
            Item.builder()
                .attribute("String", "stringValue")
                .attribute("int", 15)
                .attribute("long", 200L)
                .attribute("float", 15.5)
                .attribute("Integer", new Integer(15))
                .attribute("Long", new Long(200))
                .attribute("Float", new Float(15.5))
                .attribute("bool", true)
                .attribute("Boolean", Boolean.TRUE)
                .attribute("bytes", BYTES1)
                .attribute("ByteBuffer", ByteBuffer.wrap(BYTES1))
                .strings("StringsVarArgs", "value1", "value2")
                .strings("StringsCollection", Arrays.asList("value1", "value2"))
                .numbers("NumbersVarArgs", 1, 2.0, 3L)
                .numbers("NumbersCollection", Arrays.asList(1, 2.0, 3L))
                .numbers("LongCollection", longs)
                .byteArrays("bytesVarArgs", BYTES1, BYTES2)
                .byteArrays("bytesCollection", Arrays.asList(BYTES1, BYTES2))
                .byteBuffers("ByteBuffersVarArgs", ByteBuffer.wrap(BYTES1), ByteBuffer.wrap(BYTES2))
                .byteBuffers("ByteBuffersCollection", Arrays.asList(ByteBuffer.wrap(BYTES1), ByteBuffer.wrap(BYTES2)))
                .attribute("list", Arrays.asList(15, 200L, 15.5, "stringValue", BYTES1, ByteBuffer.wrap(BYTES2), true))
                .attribute("map", testMap)
                .attribute("nested", Arrays.asList(15, singletonMap("nestedList", Arrays.asList("stringValue", BYTES1))))
                .build();


        assertThat(item).containsEntry("String", AttributeValue.builder().s("stringValue").build());
        assertThat(item).containsEntry("int", AttributeValue.builder().n("15").build());
        assertThat(item).containsEntry("long", AttributeValue.builder().n("200").build());
        assertThat(item).containsEntry("float", AttributeValue.builder().n("15.5").build());
        assertThat(item).containsEntry("Integer", AttributeValue.builder().n("15").build());
        assertThat(item).containsEntry("Long", AttributeValue.builder().n("200").build());
        assertThat(item).containsEntry("Float", AttributeValue.builder().n("15.5").build());
        assertThat(item).containsEntry("bool", AttributeValue.builder().bool(true).build());
        assertThat(item).containsEntry("Boolean", AttributeValue.builder().bool(true).build());
        assertThat(item).containsEntry("StringsVarArgs", AttributeValue.builder().ss("value1", "value2").build());
        assertThat(item).containsEntry("StringsCollection", AttributeValue.builder().ss("value1", "value2").build());
        assertThat(item).containsEntry("NumbersVarArgs", AttributeValue.builder().ns("1", "2.0", "3").build());
        assertThat(item).containsEntry("NumbersCollection", AttributeValue.builder().ns("1", "2.0", "3").build());
        assertThat(item).containsEntry("LongCollection", AttributeValue.builder().ns("1", "2", "3").build());

        assertThat(item).hasEntrySatisfying("bytes", bytesMatching(BYTES1));
        assertThat(item).hasEntrySatisfying("ByteBuffer", bytesMatching(BYTES1));
        assertThat(item).hasEntrySatisfying("bytesVarArgs", bytesMatching(BYTES1, BYTES2));
        assertThat(item).hasEntrySatisfying("bytesCollection", bytesMatching(BYTES1, BYTES2));
        assertThat(item).hasEntrySatisfying("ByteBuffersVarArgs", bytesMatching(BYTES1, BYTES2));
        assertThat(item).hasEntrySatisfying("ByteBuffersCollection", bytesMatching(BYTES1, BYTES2));

        assertThat(item.get("list").l()).hasSize(7);
        assertThat(item.get("list").l().get(0)).isEqualTo(AttributeValue.builder().n("15").build());
        assertThat(item.get("list").l().get(1)).isEqualTo(AttributeValue.builder().n("200").build());
        assertThat(item.get("list").l().get(2)).isEqualTo(AttributeValue.builder().n("15.5").build());
        assertThat(item.get("list").l().get(3)).isEqualTo(AttributeValue.builder().s("stringValue").build());
        assertThat(item.get("list").l().get(4)).has(bytesMatching(BYTES1));
        assertThat(item.get("list").l().get(5)).has(bytesMatching(BYTES2));
        assertThat(item.get("list").l().get(6)).isEqualTo(AttributeValue.builder().bool(true).build());

        assertThat(item.get("map").m().entrySet()).hasSize(5);
        assertThat(item.get("map").m().get("String")).isEqualTo(AttributeValue.builder().s("stringValue").build());
        assertThat(item.get("map").m().get("int")).isEqualTo(AttributeValue.builder().n("15").build());
        assertThat(item.get("map").m().get("byteArray")).has(bytesMatching(BYTES1));
        assertThat(item.get("map").m().get("ByteBuffer")).has(bytesMatching(BYTES2));
        assertThat(item.get("map").m().get("bool")).isEqualTo(AttributeValue.builder().bool(true).build());

        assertThat(item.get("nested").l()).hasSize(2);
        assertThat(item.get("nested").l().get(0)).isEqualTo(AttributeValue.builder().n("15").build());
        assertThat(item.get("nested").l().get(1).m().entrySet()).hasSize(1);
        assertThat(item.get("nested").l().get(1).m().get("nestedList").l()).hasSize(2);
        assertThat(item.get("nested").l().get(1).m().get("nestedList").l().get(0)).isEqualTo(AttributeValue.builder().s("stringValue").build());
        assertThat(item.get("nested").l().get(1).m().get("nestedList").l().get(1)).has(bytesMatching(BYTES1));
    }

    private static Condition<AttributeValue> bytesMatching(byte[] bytes) {
        return new Condition<>(item -> byteBufferEquals(item.b(), bytes), "bytes matching");
    }

    private static Condition<AttributeValue> bytesMatching(byte[] firstByteArray, byte[] secondByteArray) {
        return new Condition<>(item -> {
            return item.bs().size() == 2 &&
                   byteBufferEquals(item.bs().get(0), firstByteArray) &&
                   byteBufferEquals(item.bs().get(1), secondByteArray);
        }, "List<" + String.valueOf(firstByteArray) + ", " + String.valueOf(secondByteArray) + ">");
    }

    private static boolean byteBufferEquals(ByteBuffer byteBuffer, byte[] bytes) {
        if (byteBuffer.remaining() != bytes.length) {
            return false;
        }
        byte[] actual = new byte[bytes.length];
        byteBuffer.duplicate().get(actual);
        return Arrays.equals(actual, bytes);
    }
}