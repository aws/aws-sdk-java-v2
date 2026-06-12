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

package software.amazon.awssdk.services.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Verifies that the generated {@code fastX} factories on {@link AttributeValue} (emitted under the
 * {@code generateFastUnionConstructors} DynamoDB codegen customization) produce objects indistinguishable
 * from the builder path, {@code AttributeValue.builder().x(v).build()}, for present values, null arguments,
 * and the auto-construct sentinel; and that collection members are defensively copied and stored unmodifiable.
 */
class AttributeValueFastConstructorTest {

    private static final SdkBytes BYTES = SdkBytes.fromUtf8String("b");

    static Stream<Arguments> presentValueCases() {
        return Stream.of(
            arg("S", () -> AttributeValue.fastS("v"), () -> AttributeValue.builder().s("v").build()),
            arg("N", () -> AttributeValue.fastN("1"), () -> AttributeValue.builder().n("1").build()),
            arg("B", () -> AttributeValue.fastB(BYTES), () -> AttributeValue.builder().b(BYTES).build()),
            arg("SS", () -> AttributeValue.fastSs(Arrays.asList("a", "b")),
                () -> AttributeValue.builder().ss(Arrays.asList("a", "b")).build()),
            arg("NS", () -> AttributeValue.fastNs(Arrays.asList("1", "2")),
                () -> AttributeValue.builder().ns(Arrays.asList("1", "2")).build()),
            arg("BS", () -> AttributeValue.fastBs(Arrays.asList(BYTES)),
                () -> AttributeValue.builder().bs(Arrays.asList(BYTES)).build()),
            arg("M", () -> AttributeValue.fastM(singletonMap("k", AttributeValue.fromS("x"))),
                () -> AttributeValue.builder().m(singletonMap("k", AttributeValue.fromS("x"))).build()),
            arg("L", () -> AttributeValue.fastL(Arrays.asList(AttributeValue.fromS("x"))),
                () -> AttributeValue.builder().l(Arrays.asList(AttributeValue.fromS("x"))).build()),
            arg("BOOL", () -> AttributeValue.fastBool(true), () -> AttributeValue.builder().bool(true).build()),
            arg("NUL", () -> AttributeValue.fastNul(true), () -> AttributeValue.builder().nul(true).build())
        );
    }

    static Stream<Arguments> nullValueCases() {
        return Stream.of(
            arg("S", () -> AttributeValue.fastS(null), () -> AttributeValue.builder().s(null).build()),
            arg("N", () -> AttributeValue.fastN(null), () -> AttributeValue.builder().n(null).build()),
            arg("B", () -> AttributeValue.fastB(null), () -> AttributeValue.builder().b(null).build()),
            arg("SS", () -> AttributeValue.fastSs(null), () -> AttributeValue.builder().ss((List<String>) null).build()),
            arg("NS", () -> AttributeValue.fastNs(null), () -> AttributeValue.builder().ns((List<String>) null).build()),
            arg("BS", () -> AttributeValue.fastBs(null), () -> AttributeValue.builder().bs((List<SdkBytes>) null).build()),
            arg("M", () -> AttributeValue.fastM(null),
                () -> AttributeValue.builder().m((Map<String, AttributeValue>) null).build()),
            arg("L", () -> AttributeValue.fastL(null), () -> AttributeValue.builder().l((List<AttributeValue>) null).build()),
            arg("BOOL", () -> AttributeValue.fastBool(null), () -> AttributeValue.builder().bool(null).build()),
            arg("NUL", () -> AttributeValue.fastNul(null), () -> AttributeValue.builder().nul(null).build())
        );
    }

    @ParameterizedTest(name = "fastX present value matches builder: {0}")
    @MethodSource("presentValueCases")
    void fastX_presentValue_isIndistinguishableFromBuilder(String name, Supplier<AttributeValue> fast,
                                                            Supplier<AttributeValue> builder) {
        assertIndistinguishable(fast.get(), builder.get());
    }

    @ParameterizedTest(name = "fastX null argument matches builder: {0}")
    @MethodSource("nullValueCases")
    void fastX_nullArgument_isIndistinguishableFromBuilder(String name, Supplier<AttributeValue> fast,
                                                           Supplier<AttributeValue> builder) {
        AttributeValue fastValue = fast.get();
        AttributeValue builtValue = builder.get();
        assertThat(fastValue.type())
            .as("type() for null %s must match builder", name)
            .isEqualTo(builtValue.type());
        assertIndistinguishable(fastValue, builtValue);
    }

    @Test
    void fastCollection_emptyInput_matchesBuilderAndReportsMemberType() {
        AttributeValue fastValue = AttributeValue.fastM(new HashMap<>());
        AttributeValue builtValue = AttributeValue.builder().m(new HashMap<>()).build();
        assertThat(fastValue.type()).isEqualTo(AttributeValue.Type.M);
        assertIndistinguishable(fastValue, builtValue);
    }

    @Test
    void fastList_defensivelyCopiesAndStoresUnmodifiable() {
        List<AttributeValue> input = new ArrayList<>();
        input.add(AttributeValue.fromS("first"));
        AttributeValue value = AttributeValue.fastL(input);

        input.add(AttributeValue.fromS("mutated-after-construction"));
        assertThat(value.l()).hasSize(1);
        assertThatThrownBy(() -> value.l().add(AttributeValue.fromS("x")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void fastMap_defensivelyCopiesAndStoresUnmodifiable() {
        Map<String, AttributeValue> input = new HashMap<>();
        input.put("k", AttributeValue.fromS("first"));
        AttributeValue value = AttributeValue.fastM(input);

        input.put("k2", AttributeValue.fromS("mutated-after-construction"));
        assertThat(value.m()).hasSize(1);
        assertThatThrownBy(() -> value.m().put("x", AttributeValue.fromS("x")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void fastStringSet_defensivelyCopiesAndStoresUnmodifiable() {
        List<String> input = new ArrayList<>(Arrays.asList("a"));
        AttributeValue value = AttributeValue.fastSs(input);

        input.add("b");
        assertThat(value.ss()).containsExactly("a");
        assertThatThrownBy(() -> value.ss().add("x")).isInstanceOf(UnsupportedOperationException.class);
    }

    private static void assertIndistinguishable(AttributeValue fast, AttributeValue built) {
        assertThat(fast).isEqualTo(built);
        assertThat(built).isEqualTo(fast);
        assertThat(fast.hashCode()).isEqualTo(built.hashCode());
        assertThat(fast.type()).isEqualTo(built.type());
        assertThat(fast.toString()).isEqualTo(built.toString());
        assertThat(fast.sdkFields()).hasSameSizeAs(built.sdkFields());
        for (int i = 0; i < fast.sdkFields().size(); i++) {
            software.amazon.awssdk.core.SdkField<?> fastField = fast.sdkFields().get(i);
            software.amazon.awssdk.core.SdkField<?> builtField = built.sdkFields().get(i);
            assertThat(fastField.getValueOrDefault(fast))
                .as("sdkField %s", fastField.memberName())
                .isEqualTo(builtField.getValueOrDefault(built));
        }
        assertThat(fast.hasSs()).isEqualTo(built.hasSs());
        assertThat(fast.hasNs()).isEqualTo(built.hasNs());
        assertThat(fast.hasBs()).isEqualTo(built.hasBs());
        assertThat(fast.hasM()).isEqualTo(built.hasM());
        assertThat(fast.hasL()).isEqualTo(built.hasL());
    }

    private static Map<String, AttributeValue> singletonMap(String k, AttributeValue v) {
        Map<String, AttributeValue> m = new HashMap<>();
        m.put(k, v);
        return m;
    }

    private static Arguments arg(String name, Supplier<AttributeValue> fast, Supplier<AttributeValue> builder) {
        return Arguments.of(name, fast, builder);
    }
}
