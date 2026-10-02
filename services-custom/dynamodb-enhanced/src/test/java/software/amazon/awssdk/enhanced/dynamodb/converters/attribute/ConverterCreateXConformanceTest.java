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

package software.amazon.awssdk.enhanced.dynamodb.converters.attribute;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.BooleanAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ByteArrayAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.DocumentAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.DurationAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.IntegerAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ListAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LongAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SetAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SdkBytesAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringAttributeConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Verifies that the Enhanced Client converters (which now use {@code AttributeValue.createX()})
 * produce output indistinguishable from the original builder path. This guards against regressions
 * in the builder to createX refactor.
 */
class ConverterCreateXConformanceTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("converterCases")
    void transformFrom_matchesBuilderPath(String name, AttributeValue fromConverter, AttributeValue fromBuilder) {
        assertThat(fromConverter).isEqualTo(fromBuilder);
        assertThat(fromConverter.type()).isEqualTo(fromBuilder.type());
        assertThat(fromConverter.hashCode()).isEqualTo(fromBuilder.hashCode());
        assertThat(fromConverter.toString()).isEqualTo(fromBuilder.toString());
    }

    static Stream<Arguments> converterCases() {
        return Stream.of(
            Arguments.of("StringConverter",
                StringAttributeConverter.create().transformFrom("hello"),
                AttributeValue.builder().s("hello").build()),

            Arguments.of("IntegerConverter",
                IntegerAttributeConverter.create().transformFrom(42),
                AttributeValue.builder().n("42").build()),

            Arguments.of("LongConverter",
                LongAttributeConverter.create().transformFrom(123456789L),
                AttributeValue.builder().n("123456789").build()),

            Arguments.of("BooleanConverter true",
                BooleanAttributeConverter.create().transformFrom(true),
                AttributeValue.builder().bool(true).build()),

            Arguments.of("BooleanConverter false",
                BooleanAttributeConverter.create().transformFrom(false),
                AttributeValue.builder().bool(false).build()),

            Arguments.of("Duration whole seconds",
                DurationAttributeConverter.create().transformFrom(Duration.ofSeconds(90)),
                AttributeValue.builder().n("90").build()),

            Arguments.of("Duration with nanos",
                DurationAttributeConverter.create().transformFrom(Duration.ofSeconds(90, 500_000_000)),
                AttributeValue.builder().n("90.500000000").build()),

            Arguments.of("SetConverter string set",
                SetAttributeConverter.setConverter(StringAttributeConverter.create())
                    .transformFrom(new LinkedHashSet<>(Arrays.asList("a", "b"))),
                AttributeValue.builder().ss("a", "b").build()),

            Arguments.of("SetConverter number set",
                SetAttributeConverter.setConverter(IntegerAttributeConverter.create())
                    .transformFrom(new LinkedHashSet<>(Arrays.asList(1, 2, 3))),
                AttributeValue.builder().ns("1", "2", "3").build()),

            Arguments.of("ListConverter",
                ListAttributeConverter.create(StringAttributeConverter.create())
                    .transformFrom(Arrays.asList("x", "y")),
                AttributeValue.builder().l(
                    AttributeValue.builder().s("x").build(),
                    AttributeValue.builder().s("y").build()).build()),

            Arguments.of("SdkBytesConverter (createB)",
                SdkBytesAttributeConverter.create().transformFrom(SdkBytes.fromUtf8String("binary")),
                AttributeValue.builder().b(SdkBytes.fromUtf8String("binary")).build()),

            Arguments.of("SetConverter byte set (createBs)",
                SetAttributeConverter.setConverter(SdkBytesAttributeConverter.create())
                    .transformFrom(new LinkedHashSet<>(Arrays.asList(
                        SdkBytes.fromUtf8String("a"), SdkBytes.fromUtf8String("b")))),
                AttributeValue.builder().bs(
                    SdkBytes.fromUtf8String("a"), SdkBytes.fromUtf8String("b")).build()),

            Arguments.of("EnhancedAttributeValue null (createNul)",
                EnhancedAttributeValue.nullValue().toAttributeValue(),
                AttributeValue.builder().nul(true).build()),

            Arguments.of("EnhancedAttributeValue map (createM)",
                EnhancedAttributeValue.fromMap(
                    Collections.singletonMap("key", AttributeValue.builder().s("val").build())).toAttributeValue(),
                AttributeValue.builder().m(
                    Collections.singletonMap("key", AttributeValue.builder().s("val").build())).build())
        );
    }
}
