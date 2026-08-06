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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.BigDecimalAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.BooleanAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.DoubleAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.FloatAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.InstantAsStringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.IntegerAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ListAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LongAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.MapAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.string.StringStringConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Verifies that {@link MapAttributeConverter} and {@link ListAttributeConverter} correctly handle null
 * values/elements by converting them to DynamoDB NULL type, regardless of the element converter type.
 */
public class CollectionNullValueConverterTest {

    private static final AttributeValue NULL_ATTR = AttributeValue.builder().nul(true).build();

    private static Stream<Arguments> elementConverters() {
        return Stream.of(
            Arguments.of("String", StringAttributeConverter.create()),
            Arguments.of("Boolean", BooleanAttributeConverter.create()),
            Arguments.of("Integer", IntegerAttributeConverter.create()),
            Arguments.of("Long", LongAttributeConverter.create()),
            Arguments.of("Float", FloatAttributeConverter.create()),
            Arguments.of("Double", DoubleAttributeConverter.create()),
            Arguments.of("BigDecimal", BigDecimalAttributeConverter.create()),
            Arguments.of("Instant", InstantAsStringAttributeConverter.create())
        );
    }

    @ParameterizedTest(name = "Map with null {0} value produces DynamoDB NULL")
    @MethodSource("elementConverters")
    void mapConverter_nullValue_producesNullAttributeValue(String name, AttributeConverter<?> elementConverter) {
        @SuppressWarnings("unchecked")
        AttributeConverter<Object> converter = (AttributeConverter<Object>) elementConverter;
        MapAttributeConverter<Map<String, Object>> mapConverter =
            MapAttributeConverter.mapConverter(StringStringConverter.create(), converter);

        Map<String, Object> input = new HashMap<>();
        input.put("key1", null);

        AttributeValue result = mapConverter.transformFrom(input);

        assertThat(result.hasM()).isTrue();
        assertThat(result.m().get("key1")).isEqualTo(NULL_ATTR);
    }

    @ParameterizedTest(name = "List with null {0} element produces DynamoDB NULL")
    @MethodSource("elementConverters")
    void listConverter_nullElement_producesNullAttributeValue(String name, AttributeConverter<?> elementConverter) {
        @SuppressWarnings("unchecked")
        AttributeConverter<Object> converter = (AttributeConverter<Object>) elementConverter;
        ListAttributeConverter<List<Object>> listConverter = ListAttributeConverter.create(converter);

        List<Object> input = Arrays.asList(null, null);

        AttributeValue result = listConverter.transformFrom(input);

        assertThat(result.hasL()).isTrue();
        assertThat(result.l()).hasSize(2);
        assertThat(result.l().get(0)).isEqualTo(NULL_ATTR);
        assertThat(result.l().get(1)).isEqualTo(NULL_ATTR);
    }
}
