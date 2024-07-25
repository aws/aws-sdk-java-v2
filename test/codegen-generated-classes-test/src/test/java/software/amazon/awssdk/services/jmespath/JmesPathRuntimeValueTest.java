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

package software.amazon.awssdk.services.jmespath;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.services.restjsonwithwaiters.jmespath.internal.JmesPathRuntime.Value;
import software.amazon.awssdk.services.restjsonwithwaiters.model.PayloadStructType;
import software.amazon.awssdk.utils.Pair;

class JmesPathRuntimeValueTest {

    private static final Map<String, String> MAP = new HashMap<>();

    @BeforeAll
    static void init() {
        MAP.put("k1", "v1");
        MAP.put("k2", "v2");
    }

    @Test
    void valueReturnsConstructorInput() {
        assertThat(new Value(null).value()).isEqualTo(null);
        assertThat(new Value(5).value()).isEqualTo(5);
        assertThat(new Value("").value()).isEqualTo("");
        assertThat(new Value(true).value()).isEqualTo(true);
        assertThat(new Value(emptyList()).value()).isEqualTo(emptyList());
    }

    @Test
    void valuesReturnsListForm() {
        assertThat(new Value(null).values()).isEqualTo(emptyList());
        assertThat(new Value(5).values()).isEqualTo(singletonList(5));
        assertThat(new Value("").values()).isEqualTo(singletonList(""));
        assertThat(new Value(true).values()).isEqualTo(singletonList(true));
        assertThat(new Value(singletonList("a")).values()).isEqualTo(singletonList("a"));
        assertThat(new Value(simpleSdkPojo()).values()).isEqualTo(singletonList(simpleSdkPojo()));
    }

    @Test
    void booleanValueReturnsConstructorInput() {
        assertThat(new Value(null).booleanValue()).isEqualTo(null);
        assertThatThrownBy(() -> new Value(simpleSdkPojo()).booleanValue()).isInstanceOf(IllegalStateException.class)
                                                                           .hasMessageContaining("Cannot convert type POJO");
        assertThatThrownBy(() -> new Value(5).booleanValue()).isInstanceOf(IllegalStateException.class)
                                                             .hasMessageContaining("Cannot convert type INTEGER");
        assertThat(new Value("").booleanValue()).isEqualTo(false);
        assertThat(new Value(true).booleanValue()).isEqualTo(true);
        assertThatThrownBy(() -> new Value(emptyList()).booleanValue()).isInstanceOf(IllegalStateException.class)
                                                                       .hasMessageContaining("Cannot convert type LIST");
    }

    @Test
    void stringValueReturnsConstructorInput() {
        assertThat(new Value(null).stringValue()).isEqualTo(null);
        assertThatThrownBy(() -> new Value(simpleSdkPojo()).stringValue()).isInstanceOf(IllegalStateException.class)
                                                                          .hasMessageContaining("Cannot convert type POJO");
        assertThat(new Value(5).stringValue()).isEqualTo("5");
        assertThat(new Value("").stringValue()).isEqualTo("");
        assertThat(new Value(true).stringValue()).isEqualTo("true");
        assertThatThrownBy(() -> new Value(emptyList()).stringValue()).isInstanceOf(IllegalStateException.class)
                                                                      .hasMessageContaining("Cannot convert type LIST");
        assertThatThrownBy(() -> new Value(new HashMap<>()).stringValue()).isInstanceOf(IllegalStateException.class)
                                                                          .hasMessageContaining("Cannot convert type MAP");
    }

    @Test
    void stringValuesReturnsListForm() {
        assertThat(new Value(null).stringValues()).isEqualTo(emptyList());
        assertThat(new Value(5).stringValues()).isEqualTo(singletonList("5"));
        assertThat(new Value("").stringValues()).isEqualTo(singletonList(""));
        assertThat(new Value(true).stringValues()).isEqualTo(singletonList("true"));
        assertThat(new Value(singletonList("a")).stringValues()).isEqualTo(singletonList("a"));
        assertThatThrownBy(() -> new Value(new HashMap<>()).stringValues()).isInstanceOf(IllegalStateException.class)
                                                                           .hasMessageContaining("Cannot convert type MAP");
        assertThatThrownBy(() -> new Value(simpleSdkPojo()).stringValues()).isInstanceOf(IllegalStateException.class)
                                                                           .hasMessageContaining("Cannot convert type POJO");
    }

    @Test
    void stringValuesMapReturnsMapForm() {
        assertThat(new Value(createMapOfStrings("k1", "v1")).stringValuesMap()).isEqualTo(createMapOfStrings("k1", "v1"));
        assertThat(new Value(new HashMap<>()).stringValuesMap()).isEqualTo(new HashMap<>());
        assertThat(new Value(null).stringValuesMap()).isEqualTo(new HashMap<>());
        assertThatThrownBy(() -> new Value(5).stringValuesMap()).isInstanceOf(IllegalArgumentException.class)
                                                                .hasMessageContaining("Not of type MAP");
        assertThatThrownBy(() -> new Value(singletonList("a")).stringValuesMap()).isInstanceOf(IllegalArgumentException.class)
                                                                                 .hasMessageContaining("Not of type MAP");
    }

    @Test
    void andBehavesWithBooleans() {
        assertThat(booleanTrue().and(booleanTrue())).isEqualTo(booleanTrue());
        assertThat(booleanFalse().and(booleanTrue())).isEqualTo(booleanFalse());
        assertThat(booleanTrue().and(booleanFalse())).isEqualTo(booleanFalse());
        assertThat(booleanFalse().and(booleanFalse())).isEqualTo(booleanFalse());
    }

    @Test
    void andBehavesWithPojos() {
        Value truePojo1 = simpleSdkPojoValue(Pair.of("foo", "bar"));
        Value truePojo2 = simpleSdkPojoValue(Pair.of("foo", "bar"));
        Value falsePojo1 = simpleSdkPojoValue();
        Value falsePojo2 = simpleSdkPojoValue();

        assertThat(truePojo1.and(truePojo2)).isSameAs(truePojo2);
        assertThat(falsePojo1.and(truePojo1)).isSameAs(falsePojo1);
        assertThat(truePojo1.and(falsePojo1)).isSameAs(falsePojo1);
        assertThat(falsePojo1.and(falsePojo2)).isSameAs(falsePojo1);
    }

    @Test
    void andBehavesWithLists() {
        Value trueList1 = new Value(singletonList("foo"));
        Value trueList2 = new Value(singletonList("foo"));
        Value falseList1 = new Value(emptyList());
        Value falseList2 = new Value(emptyList());

        assertThat(trueList1.and(trueList2)).isSameAs(trueList2);
        assertThat(falseList1.and(trueList1)).isSameAs(falseList1);
        assertThat(trueList1.and(falseList1)).isSameAs(falseList1);
        assertThat(falseList1.and(falseList2)).isSameAs(falseList1);
    }

    @Test
    void andBehavesWithMaps() {
        Value trueMap1 = new Value(createMapOfStrings("k1", "v1"));
        Value trueMap2 = new Value(createMapOfStrings("k1", "v1"));
        Value falseMap1 = new Value(new HashMap<>());
        Value falseMap2 = new Value(new HashMap<>());

        assertThat(trueMap1.and(trueMap2)).isSameAs(trueMap2);
        assertThat(falseMap1.and(trueMap1)).isSameAs(falseMap1);
        assertThat(trueMap1.and(falseMap1)).isSameAs(falseMap1);
        assertThat(falseMap1.and(falseMap2)).isSameAs(falseMap1);
    }

    @Test
    void andBehavesWithStrings() {
        Value trueList1 = new Value("foo");
        Value trueList2 = new Value("foo");
        Value falseList1 = new Value("");
        Value falseList2 = new Value("");

        assertThat(trueList1.and(trueList2)).isSameAs(trueList2);
        assertThat(falseList1.and(trueList1)).isSameAs(falseList1);
        assertThat(trueList1.and(falseList1)).isSameAs(falseList1);
        assertThat(falseList1.and(falseList2)).isSameAs(falseList1);
    }

    @Test
    void orBehavesWithBooleans() {
        assertThat(booleanTrue().or(booleanTrue())).isEqualTo(booleanTrue());
        assertThat(booleanFalse().or(booleanTrue())).isEqualTo(booleanTrue());
        assertThat(booleanTrue().or(booleanFalse())).isEqualTo(booleanTrue());
        assertThat(booleanFalse().or(booleanFalse())).isEqualTo(new Value(null));
    }

    @Test
    void orBehavesWithPojos() {
        Value truePojo1 = simpleSdkPojoValue(Pair.of("foo", "bar"));
        Value truePojo2 = simpleSdkPojoValue(Pair.of("foo", "bar"));
        Value falsePojo1 = simpleSdkPojoValue();
        Value falsePojo2 = simpleSdkPojoValue();

        assertThat(truePojo1.or(truePojo2)).isSameAs(truePojo1);
        assertThat(falsePojo1.or(truePojo1)).isSameAs(truePojo1);
        assertThat(truePojo1.or(falsePojo1)).isSameAs(truePojo1);
        assertThat(falsePojo1.or(falsePojo2)).isEqualTo(new Value(null));
    }

    @Test
    void orBehavesWithLists() {
        Value trueList1 = new Value(singletonList("foo"));
        Value trueList2 = new Value(singletonList("foo"));
        Value falseList1 = new Value(emptyList());
        Value falseList2 = new Value(emptyList());

        assertThat(trueList1.or(trueList2)).isSameAs(trueList1);
        assertThat(falseList1.or(trueList1)).isSameAs(trueList1);
        assertThat(trueList1.or(falseList1)).isSameAs(trueList1);
        assertThat(falseList1.or(falseList2)).isEqualTo(new Value(null));
    }

    @Test
    void orBehavesWithMaps() {
        Value trueMap1 = new Value(createMapOfStrings("k1", "v1"));
        Value trueMap2 = new Value(createMapOfStrings("k1", "v1"));
        Value falseMap1 = new Value(new HashMap<>());
        Value falseMap2 = new Value(new HashMap<>());

        assertThat(trueMap1.or(trueMap2)).isSameAs(trueMap1);
        assertThat(falseMap1.or(trueMap1)).isSameAs(trueMap1);
        assertThat(trueMap1.or(falseMap1)).isSameAs(trueMap1);
        assertThat(falseMap1.or(falseMap2)).isEqualTo(new Value(null));
    }

    @Test
    void orBehavesWithStrings() {
        Value trueList1 = new Value("foo");
        Value trueList2 = new Value("foo");
        Value falseList1 = new Value("");
        Value falseList2 = new Value("");

        assertThat(trueList1.or(trueList2)).isSameAs(trueList1);
        assertThat(falseList1.or(trueList1)).isSameAs(trueList1);
        assertThat(trueList1.or(falseList1)).isSameAs(trueList1);
        assertThat(falseList1.or(falseList2)).isEqualTo(new Value(null));
    }

    @Test
    void notBehaves() {
        assertThat(booleanTrue().not()).isEqualTo(booleanFalse());
        assertThat(booleanFalse().not()).isEqualTo(booleanTrue());
        assertThat(new Value("").not()).isEqualTo(booleanTrue());
    }

    @Test
    void constantBehaves() {
        assertThat(new Value(null).constant(new Value(5))).isEqualTo(new Value(5));
        assertThat(new Value(null).constant(5)).isEqualTo(new Value(5));
    }

    @Test
    void wildcardBehavesWithNull() {
        assertThat(new Value(null).wildcard()).isEqualTo(new Value(null));
    }

    @Test
    void wildcardBehavesWithPojosAndDoesNotFlatten() {
        assertThat(simpleSdkPojoValue(Pair.of("foo", "bar"),
                                      Pair.of("foo2", singletonList("bar")),
                                      Pair.of("foo3", simpleSdkPojo(Pair.of("x", "y"))))
                       .wildcard())
            .isEqualTo(new Value(asList("bar",
                                        singletonList("bar"),
                                        simpleSdkPojo(Pair.of("x", "y")))));
    }

    @Test
    void wildcardBehavesWithLists() {
        assertThat(nestedConstruct().field("foo2").field("baz").wildcard())
            .isEqualTo(new Value(asList(PayloadStructType.builder().payloadMemberTwo("2a").build(),
                                        PayloadStructType.builder().payloadMemberOne("1b").payloadMemberTwo("2b").build())));
    }

    @Test
    void wildcardBehavesWithMaps() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", PayloadStructType.builder().payloadMemberTwo("2a").build());

        assertThat(nestedConstruct().field("foo4").field("bap").wildcard()).isEqualTo(new Value(map));
    }

    @Test
    void wildcardBehavesWithProjectionAndFiltersNullElements() {
        assertThat(nestedConstruct().field("foo2").field("baz").wildcard().field("PayloadMemberOne"))
            .isEqualTo(new Value(asList("1b")));
    }

    @Test
    void flattenBehavesWithNull() {
        assertThat(new Value(null).flatten()).isEqualTo(new Value(null));
    }

    @Test
    void flattenBehavesWithLists() {
        assertThat(new Value(asList("bar",
                                    singletonList("bar"),
                                    simpleSdkPojo(Pair.of("x", "y"))))
                       .flatten())
            .isEqualTo(new Value(asList("bar",
                                        "bar",
                                        simpleSdkPojo(Pair.of("x", "y")))));
    }

    @Test
    void flattenBehavesWithProjectionAndFiltersNullElements() {
        assertThat(nestedConstruct().field("foo2").field("baz").flatten().field("PayloadMemberOne"))
            .isEqualTo(new Value(asList("1b")));
    }

    @Test
    void fieldBehaves() {
        assertThat(new Value(null).field("foo")).isEqualTo(new Value(null));
        assertThat(simpleSdkPojoValue(Pair.of("foo", "bar")).field("foo")).isEqualTo(new Value("bar"));
    }

    @Test
    void filterBehaves() {
        assertThat(new Value(null).filter(x -> new Value(true))).isEqualTo(new Value(null));

        Value listValue = new Value(asList("foo", "bar"));
        assertThat(listValue.filter(x -> new Value(Objects.equals(x.value(), "foo")))).isEqualTo(new Value(asList("foo")));
        assertThat(listValue.filter(x -> new Value(false))).isEqualTo(new Value(emptyList()));
        assertThat(listValue.filter(x -> new Value(true))).isEqualTo(listValue);

        Value mapValue = new Value(MAP);
        Map<String, String> expectedMap = createMapOfStrings("k1", "v1");
        assertThat(mapValue.filter(x -> new Value(Objects.equals(x.value(), "v1")))).isEqualTo(new Value(expectedMap));
        assertThat(mapValue.filter(x -> new Value(Objects.equals(x.value(), "v3")))).isEqualTo(new Value(new HashMap<>()));
        assertThat(mapValue.filter(x -> new Value(Objects.equals(x.value(), "k1")))).isEqualTo(new Value(new HashMap<>()));
    }

    @Test
    void lengthBehaves() {
        assertThat(new Value(null).length()).isEqualTo(new Value(null));
        assertThat(new Value("a").length()).isEqualTo(new Value(1));
        assertThat(simpleSdkPojoValue(Pair.of("a", "b")).length()).isEqualTo(new Value(1));
        assertThat(new Value(singletonList("a")).length()).isEqualTo(new Value(1));
        assertThat(new Value(MAP).length()).isEqualTo(new Value(2));
    }

    @Test
    void keysBehaves() {
        assertThat(new Value(null).keys()).isEqualTo(new Value(emptyList()));
        assertThatThrownBy(() -> new Value("a").keys()).isInstanceOf(IllegalArgumentException.class)
                                                       .hasMessageContaining("Unsupported type for keys function");
        assertThatThrownBy(() -> new Value(asList("a", "b")).keys()).isInstanceOf(IllegalArgumentException.class)
                                                                    .hasMessageContaining("Unsupported type for keys function");
        assertThat(simpleSdkPojoValue(Pair.of("a", "b"), Pair.of("c", "d")).keys()).isEqualTo(new Value(asList("a", "c")));
        assertThat(new Value(MAP).keys()).isEqualTo(new Value(asList("k1", "k2")));
    }


    @Test
    void containsBehaves() {
        assertThat(new Value(null).contains(null)).isEqualTo(new Value(null));
        assertThat(new Value("abcde").contains(new Value("bcd"))).isEqualTo(new Value(true));
        assertThat(new Value("abcde").contains(new Value("f"))).isEqualTo(new Value(false));
        assertThat(new Value(asList("a", "b")).contains(new Value("a"))).isEqualTo(new Value(true));
        assertThat(new Value(asList("a", "b")).contains(new Value("c"))).isEqualTo(new Value(false));
        assertThat(new Value(MAP).contains(new Value("v1"))).isEqualTo(new Value(true));
        assertThat(new Value(MAP).contains(new Value("k1"))).isEqualTo(new Value(false));
    }

    @Test
    void compareIntegerBehaves() {
        assertThat(new Value(1).compare(">", new Value(2))).isEqualTo(new Value(false));
        assertThat(new Value(1).compare(">=", new Value(2))).isEqualTo(new Value(false));
        assertThat(new Value(1).compare("<=", new Value(2))).isEqualTo(new Value(true));
        assertThat(new Value(1).compare("<", new Value(2))).isEqualTo(new Value(true));
        assertThat(new Value(1).compare("==", new Value(2))).isEqualTo(new Value(false));
        assertThat(new Value(1).compare("!=", new Value(2))).isEqualTo(new Value(true));

        assertThat(new Value(1).compare(">", new Value(1))).isEqualTo(new Value(false));
        assertThat(new Value(1).compare(">=", new Value(1))).isEqualTo(new Value(true));
        assertThat(new Value(1).compare("<=", new Value(1))).isEqualTo(new Value(true));
        assertThat(new Value(1).compare("<", new Value(1))).isEqualTo(new Value(false));
        assertThat(new Value(1).compare("==", new Value(1))).isEqualTo(new Value(true));
        assertThat(new Value(1).compare("!=", new Value(1))).isEqualTo(new Value(false));
    }

    @Test
    void multiSelectListBehaves() {
        assertThat(new Value(5).multiSelectList(x -> new Value(1), x -> new Value(2)))
            .isEqualTo(new Value(asList(1, 2)));
    }

    @Test
    void multiSelectHashBehaves() {
        Map<String, Function<Value, Value>> functionMap = new HashMap<>();
        functionMap.put("k1", x -> new Value(1));
        functionMap.put("k2", x -> new Value(2));


        Map<String, Integer> expectedMap = new HashMap<>();
        expectedMap.put("k1", 1);
        expectedMap.put("k2", 2);
        assertThat(new Value(MAP).multiSelectHash(functionMap)).isEqualTo(new Value(expectedMap));
    }

    private Value booleanTrue() {
        return new Value(true);
    }

    private Value booleanFalse() {
        return new Value(false);
    }

    //This construct is useful for testing projections as it contains elements with incomplete data
    private Value nestedConstruct() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", PayloadStructType.builder().payloadMemberTwo("2a").build());

        return simpleSdkPojoValue(Pair.of("foo", "bar"),
                                  Pair.of("foo2", simpleSdkPojo(
                                      Pair.of("baz", asList(
                                          PayloadStructType.builder().payloadMemberTwo("2a").build(),
                                          PayloadStructType.builder().payloadMemberOne("1b").payloadMemberTwo("2b").build()
                                      )))),
                                  Pair.of("foo3", simpleSdkPojo(Pair.of("x", "y"))),
                                  Pair.of("foo4", simpleSdkPojo(Pair.of("bap", map))));
    }

    private Map<String, String> createMapOfStrings(String key, String value) {
        Map<String, String> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    /**
     * Do not use for projections. See {@code SimpleSdkPojo}.
     */
    @SafeVarargs
    private final Value simpleSdkPojoValue(Pair<String, Object>... entry) {
        return new Value(simpleSdkPojo(entry));
    }

    /**
     * Do not use for projections. See {@code SimpleSdkPojo}.
     */
    @SafeVarargs
    private final SdkPojo simpleSdkPojo(Pair<String, Object>... entry) {
        Map<String, Object> result = new HashMap<>();
        Stream.of(entry).forEach(e -> result.put(e.left(), e.right()));
        return new SimpleSdkPojo(result);
    }

    /**
     * The SimpleSdkPojo is useful to quickly construct test objects for most test cases.
     * It is NOT a true SdkPojo because there is no typing and cannot be used to test
     * complex nesting cases. Consider an SdkPojo that contains a typed list of two
     * SdkPojo of a different kind - SimpleSdkPojo cannot reliably be used, because if
     * a field referenced in the expression is null, JmesPathRuntime will assume it doesn't
     * exist and throw an error.
     */
    private static class SimpleSdkPojo implements SdkPojo {
        private final Map<String, Object> map;

        private SimpleSdkPojo(Map<String, Object> map) {
            this.map = map;
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return map.entrySet().stream().map(this::sdkField).collect(Collectors.toList());
        }

        private <T> SdkField<T> sdkField(Map.Entry<String, T> e) {
            @SuppressWarnings("unchecked")
            Class<T> valueClass = (Class<T>) e.getValue().getClass();
            return SdkField.builder(MarshallingType.newType(valueClass))
                           .memberName(e.getKey())
                           .getter(x -> e.getValue())
                           .traits(LocationTrait.builder().build())
                           .build();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SimpleSdkPojo that = (SimpleSdkPojo) o;
            return Objects.equals(map, that.map);
        }

        @Override
        public int hashCode() {
            return Objects.hash(map);
        }
    }
}