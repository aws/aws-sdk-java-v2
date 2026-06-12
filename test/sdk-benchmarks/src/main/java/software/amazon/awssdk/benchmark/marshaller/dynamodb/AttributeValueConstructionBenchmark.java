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

package software.amazon.awssdk.benchmark.marshaller.dynamodb;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Low-level (no mapper, no Enhanced Client) micro-benchmark of {@link AttributeValue} construction:
 * building a DynamoDB item map through the immutable builder ({@code AttributeValue.builder().s(v).build()})
 * versus the generated fast factories ({@code AttributeValue.fastS(v)}), across three payload sizes.
 *
 * Only construction is timed. Raw leaf values (strings, bytes) are produced once in {@link Setup} so the timed
 * region contains nothing but AttributeValue allocation and assembly. The built map is returned so JMH does
 * not dead-code-eliminate it.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 3)
@Fork(2)
public class AttributeValueConstructionBenchmark {

    /** Builds a single AttributeValue for each leaf type; the two impls differ only in builder vs fast factory. */
    interface AvFactory {
        AttributeValue s(String v);

        AttributeValue b(SdkBytes v);

        AttributeValue l(List<AttributeValue> v);

        AttributeValue m(Map<String, AttributeValue> v);
    }

    private static final AvFactory BUILDER = new AvFactory() {
        @Override
        public AttributeValue s(String v) {
            return AttributeValue.builder().s(v).build();
        }

        @Override
        public AttributeValue b(SdkBytes v) {
            return AttributeValue.builder().b(v).build();
        }

        @Override
        public AttributeValue l(List<AttributeValue> v) {
            return AttributeValue.builder().l(v).build();
        }

        @Override
        public AttributeValue m(Map<String, AttributeValue> v) {
            return AttributeValue.builder().m(v).build();
        }
    };

    private static final AvFactory FAST = new AvFactory() {
        @Override
        public AttributeValue s(String v) {
            return AttributeValue.fastS(v);
        }

        @Override
        public AttributeValue b(SdkBytes v) {
            return AttributeValue.fastB(v);
        }

        @Override
        public AttributeValue l(List<AttributeValue> v) {
            return AttributeValue.fastL(v);
        }

        @Override
        public AttributeValue m(Map<String, AttributeValue> v) {
            return AttributeValue.fastM(v);
        }
    };

    public enum Mode {
        BUILDER,
        FAST
    }

    public enum Payload {
        TINY,
        SMALL,
        HUGE
    }

    @State(Scope.Benchmark)
    public static class ConstructionState {
        @Param({"TINY", "SMALL", "HUGE"})
        Payload payload;

        @Param({"BUILDER", "FAST"})
        Mode mode;

        AvFactory factory;
        String str;
        SdkBytes bytes;

        @Setup
        public void setup() {
            factory = mode == Mode.FAST ? FAST : BUILDER;
            str = "abcdefghijklmnop";
            byte[] raw = new byte[16];
            for (int i = 0; i < raw.length; i++) {
                raw[i] = (byte) (i + 1);
            }
            bytes = SdkBytes.fromByteArray(raw);
        }
    }

    @Benchmark
    public Map<String, AttributeValue> construct(ConstructionState s) {
        switch (s.payload) {
            case TINY:
                return tiny(s);
            case SMALL:
                return small(s);
            case HUGE:
                return huge(s);
            default:
                throw new IllegalStateException();
        }
    }

    private static Map<String, AttributeValue> tiny(ConstructionState s) {
        AvFactory f = s.factory;
        Map<String, AttributeValue> item = new LinkedHashMap<>();
        item.put("stringAttr", f.s(s.str));
        return item;
    }

    private static Map<String, AttributeValue> small(ConstructionState s) {
        AvFactory f = s.factory;
        Map<String, AttributeValue> item = new LinkedHashMap<>();
        item.put("stringAttr", f.s(s.str));
        item.put("binaryAttr", f.b(s.bytes));
        List<AttributeValue> list = new ArrayList<>();
        list.add(f.s(s.str));
        list.add(f.b(s.bytes));
        list.add(f.s(s.str));
        item.put("listAttr", f.l(list));
        return item;
    }

    private static Map<String, AttributeValue> huge(ConstructionState s) {
        AvFactory f = s.factory;
        Map<String, AttributeValue> item = new LinkedHashMap<>();
        item.put("hashKey", f.s(s.str));
        item.put("stringAttr", f.s(s.str));
        item.put("binaryAttr", f.b(s.bytes));

        List<AttributeValue> bigList = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            bigList.add(f.s(s.str));
        }
        bigList.add(f.b(s.bytes));

        List<AttributeValue> singleton = new ArrayList<>();
        singleton.add(f.s(s.str));
        bigList.add(f.l(singleton));

        Map<String, AttributeValue> innerMap = new LinkedHashMap<>();
        innerMap.put("attrOne", f.s(s.str));
        bigList.add(f.m(innerMap));

        List<AttributeValue> nestedList = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            nestedList.add(f.s(s.str));
        }
        nestedList.add(f.b(s.bytes));
        nestedList.add(f.s(s.str));
        Map<String, AttributeValue> nestedListMap = new LinkedHashMap<>();
        nestedListMap.put("attrOne", f.s(s.str));
        nestedList.add(f.m(nestedListMap));
        bigList.add(f.l(nestedList));

        item.put("listAttr", f.l(bigList));

        Map<String, AttributeValue> mapAttr = new LinkedHashMap<>();
        mapAttr.put("attrOne", f.s(s.str));
        mapAttr.put("attrTwo", f.b(s.bytes));
        List<AttributeValue> mapList = new ArrayList<>();
        mapList.add(f.s(s.str));
        mapList.add(f.s(s.str));
        mapList.add(f.s(s.str));
        mapList.add(f.s(s.str));
        Map<String, AttributeValue> deepMap = new LinkedHashMap<>();
        deepMap.put("attrOne", f.s(s.str));
        deepMap.put("attrTwo", f.b(s.bytes));
        List<AttributeValue> deepList = new ArrayList<>();
        deepList.add(f.s(s.str));
        deepList.add(f.s(s.str));
        deepList.add(f.s(s.str));
        deepList.add(f.s(s.str));
        deepMap.put("attrThree", f.l(deepList));
        mapList.add(f.m(deepMap));
        mapAttr.put("attrThree", f.l(mapList));
        item.put("mapAttr", f.m(mapAttr));

        return item;
    }
}
