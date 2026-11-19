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
 * Verifies that the generated {@code createX} factories on {@link AttributeValue} (emitted under the
 * {@code generateFastUnionConstructors} DynamoDB codegen customization) produce objects indistinguishable
 * from the builder path, {@code AttributeValue.builder().x(v).build()}, for present values, null arguments,
 * and the auto-construct sentinel; and that collection members are defensively copied and stored unmodifiable.
 */
class AttributeValueCreateConstructorTest {

    private static final SdkBytes BYTES = SdkBytes.fromUtf8String("b");

    static Stream<Arguments> presentValueCases() {
        return Stream.of(
            arg("S", () -> AttributeValue.createS("v"), () -> AttributeValue.builder().s("v").build()),
            arg("N", () -> AttributeValue.createN("1"), () -> AttributeValue.builder().n("1").build()),
            arg("B", () -> AttributeValue.createB(BYTES), () -> AttributeValue.builder().b(BYTES).build()),
            arg("SS", () -> AttributeValue.createSs(Arrays.asList("a", "b")),
                () -> AttributeValue.builder().ss(Arrays.asList("a", "b")).build()),
            arg("NS", () -> AttributeValue.createNs(Arrays.asList("1", "2")),
                () -> AttributeValue.builder().ns(Arrays.asList("1", "2")).build()),
            arg("BS", () -> AttributeValue.createBs(Arrays.asList(BYTES)),
                () -> AttributeValue.builder().bs(Arrays.asList(BYTES)).build()),
            arg("M", () -> AttributeValue.createM(singletonMap("k", AttributeValue.fromS("x"))),
                () -> AttributeValue.builder().m(singletonMap("k", AttributeValue.fromS("x"))).build()),
            arg("L", () -> AttributeValue.createL(Arrays.asList(AttributeValue.fromS("x"))),
                () -> AttributeValue.builder().l(Arrays.asList(AttributeValue.fromS("x"))).build()),
            arg("BOOL", () -> AttributeValue.createBool(true), () -> AttributeValue.builder().bool(true).build()),
            arg("NUL", () -> AttributeValue.createNul(true), () -> AttributeValue.builder().nul(true).build())
        );
    }

    static Stream<Arguments> nullValueCases() {
        return Stream.of(
            arg("S", () -> AttributeValue.createS(null), () -> AttributeValue.builder().s(null).build()),
            arg("N", () -> AttributeValue.createN(null), () -> AttributeValue.builder().n(null).build()),
            arg("B", () -> AttributeValue.createB(null), () -> AttributeValue.builder().b(null).build()),
            arg("SS", () -> AttributeValue.createSs(null), () -> AttributeValue.builder().ss((List<String>) null).build()),
            arg("NS", () -> AttributeValue.createNs(null), () -> AttributeValue.builder().ns((List<String>) null).build()),
            arg("BS", () -> AttributeValue.createBs(null), () -> AttributeValue.builder().bs((List<SdkBytes>) null).build()),
            arg("M", () -> AttributeValue.createM(null),
                () -> AttributeValue.builder().m((Map<String, AttributeValue>) null).build()),
            arg("L", () -> AttributeValue.createL(null), () -> AttributeValue.builder().l((List<AttributeValue>) null).build()),
            arg("BOOL", () -> AttributeValue.createBool(null), () -> AttributeValue.builder().bool(null).build()),
            arg("NUL", () -> AttributeValue.createNul(null), () -> AttributeValue.builder().nul(null).build())
        );
    }

    @ParameterizedTest(name = "createX present value matches builder: {0}")
    @MethodSource("presentValueCases")
    void createX_presentValue_isIndistinguishableFromBuilder(String name, Supplier<AttributeValue> fast,
                                                            Supplier<AttributeValue> builder) {
        assertIndistinguishable(fast.get(), builder.get());
    }

    @ParameterizedTest(name = "createX null argument matches builder: {0}")
    @MethodSource("nullValueCases")
    void createX_nullArgument_isIndistinguishableFromBuilder(String name, Supplier<AttributeValue> fast,
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
        AttributeValue fastValue = AttributeValue.createM(new HashMap<>());
        AttributeValue builtValue = AttributeValue.builder().m(new HashMap<>()).build();
        assertThat(fastValue.type()).isEqualTo(AttributeValue.Type.M);
        assertIndistinguishable(fastValue, builtValue);
    }

    @Test
    void fastList_defensivelyCopiesAndStoresUnmodifiable() {
        List<AttributeValue> input = new ArrayList<>();
        input.add(AttributeValue.fromS("first"));
        AttributeValue value = AttributeValue.createL(input);

        input.add(AttributeValue.fromS("mutated-after-construction"));
        assertThat(value.l()).hasSize(1);
        assertThatThrownBy(() -> value.l().add(AttributeValue.fromS("x")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void fastMap_defensivelyCopiesAndStoresUnmodifiable() {
        Map<String, AttributeValue> input = new HashMap<>();
        input.put("k", AttributeValue.fromS("first"));
        AttributeValue value = AttributeValue.createM(input);

        input.put("k2", AttributeValue.fromS("mutated-after-construction"));
        assertThat(value.m()).hasSize(1);
        assertThatThrownBy(() -> value.m().put("x", AttributeValue.fromS("x")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void fastStringSet_defensivelyCopiesAndStoresUnmodifiable() {
        List<String> input = new ArrayList<>(Arrays.asList("a"));
        AttributeValue value = AttributeValue.createSs(input);

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

    @Test
    void createX_toBuilderRoundTrip_preservesTypeAndValue() {
        AttributeValue fast = AttributeValue.createS("hello");
        AttributeValue roundTripped = fast.toBuilder().build();
        assertThat(roundTripped).isEqualTo(fast);
        assertThat(roundTripped.type()).isEqualTo(AttributeValue.Type.S);
        assertThat(roundTripped.s()).isEqualTo("hello");
    }

    @Test
    void createX_toBuilderMutation_matchesBuilderBehavior() {
        AttributeValue fast = AttributeValue.createS("hello");
        AttributeValue built = AttributeValue.builder().s("hello").build();
        AttributeValue fastMutated = fast.toBuilder().n("42").build();
        AttributeValue builtMutated = built.toBuilder().n("42").build();
        assertThat(fastMutated.type()).isEqualTo(builtMutated.type());
        assertThat(fastMutated.n()).isEqualTo(builtMutated.n());
        assertThat(fastMutated.s()).isEqualTo(builtMutated.s());
    }

    @Test
    void fastBool_false_isTreatedAsPresent() {
        AttributeValue fast = AttributeValue.createBool(false);
        AttributeValue built = AttributeValue.builder().bool(false).build();
        assertThat(fast.type()).isEqualTo(AttributeValue.Type.BOOL);
        assertIndistinguishable(fast, built);
    }

    @Test
    void fastNul_false_isTreatedAsPresent() {
        AttributeValue fast = AttributeValue.createNul(false);
        AttributeValue built = AttributeValue.builder().nul(false).build();
        assertThat(fast.type()).isEqualTo(AttributeValue.Type.NUL);
        assertIndistinguishable(fast, built);
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
