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

package software.amazon.awssdk.enhanced.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.event.Level;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class DefaultAttributeConverterProviderTest {

    @Test
    void findConverter_whenConverterFound_logsConverterFound() {
        try (LogCaptor logCaptor = new LogCaptor(DefaultAttributeConverterProvider.class, Level.DEBUG)) {
            DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();
            provider.converterFor(EnhancedType.of(String.class));

            List<LogEvent> logEvents = logCaptor.loggedEvents();
            assertThat(logEvents).hasSize(1);
            assertThat(logEvents.get(0).getLevel().name()).isEqualTo(Level.DEBUG.name());
            assertThat(logEvents.get(0).getMessage().getFormattedMessage())
                .contains("Converter for EnhancedType(java.lang.String): software.amazon.awssdk.enhanced.dynamodb.internal"
                          + ".converter.attribute.StringAttributeConverter");
        }
    }

    @Test
    void findConverter_whenConverterNotFound_logsNoConverter() {
        try (LogCaptor logCaptor = new LogCaptor(DefaultAttributeConverterProvider.class, Level.DEBUG)) {
            DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();

            assertThatThrownBy(() -> provider.converterFor(EnhancedType.of(CustomUnsupportedType.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Converter not found for " + EnhancedType.of(CustomUnsupportedType.class));
            List<LogEvent> logEvents = logCaptor.loggedEvents();
            assertThat(logEvents).hasSize(1);
            assertThat(logEvents.get(0).getLevel().name()).isEqualTo(Level.DEBUG.name());
            assertThat(logEvents.get(0).getMessage().getFormattedMessage())
                .contains("No converter available for EnhancedType(software.amazon.awssdk.enhanced.dynamodb"
                          + ".DefaultAttributeConverterProviderTest$CustomUnsupportedType)");
        }
    }

    @ParameterizedTest
    @MethodSource("unsupportedTypes")
    void converterFor_whenUnsupportedType_throwsConverterNotFound(EnhancedType<?> type) {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @ParameterizedTest
    @MethodSource("customConverters")
    <T> void build_whenCustomProviderFirst_usesCustomConverter(AttributeConverter<T> converter) {
        AttributeConverterProvider customProvider = new CustomAttributeConverterProvider(converter);
        StaticTableSchema<Map<String, T>> schema =
            schemaWithAttribute(converter.type())
                .attributeConverterProviders(customProvider, DefaultAttributeConverterProvider.create())
                .build();

        assertThat(schema.converterForAttribute("value")).isSameAs(converter);
    }

    @ParameterizedTest
    @MethodSource("rawCollectionTypes")
    void findConverter_whenRawCollectionType_throwsConverterNotFound(EnhancedType<?> type) {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type + ". Type parameters are required for this type.");
    }

    @ParameterizedTest
    @MethodSource("unsupportedTypes")
    void converterFor_whenMapValueHasNoConverter_throwsConverterNotFound(EnhancedType<?> valueType) {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();
        EnhancedType<?> type = EnhancedType.mapOf(EnhancedType.of(String.class), valueType);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(missingNestedConverterMessage(valueType, type, "value"));
    }

    @ParameterizedTest
    @MethodSource("unsupportedTypes")
    void converterFor_whenSetElementHasNoConverter_throwsConverterNotFound(EnhancedType<?> elementType) {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();
        EnhancedType<?> type = EnhancedType.setOf(elementType);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(missingNestedConverterMessage(elementType, type, "element"));
    }

    @ParameterizedTest
    @MethodSource("unsupportedTypes")
    void converterFor_whenListElementHasNoConverter_throwsConverterNotFound(EnhancedType<?> elementType) {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();
        EnhancedType<?> type = EnhancedType.listOf(elementType);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(missingNestedConverterMessage(elementType, type, "element"));
    }

    @Test
    void build_whenSchemaAttributeIsObject_throwsConverterNotFound() {
        EnhancedType<Object> type = EnhancedType.of(Object.class);

        assertThatThrownBy(() -> schemaWithAttribute(type).build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @ParameterizedTest
    @MethodSource("collectionsWithObjectMembers")
    void build_whenCollectionMemberIsObject_throwsConverterNotFound(EnhancedType<?> type, String role) {
        assertThatThrownBy(() -> schemaWithAttribute(type).build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(missingNestedConverterMessage(EnhancedType.of(Object.class), type, role));
    }

    private static Stream<Arguments> collectionsWithObjectMembers() {
        return Stream.of(
            Arguments.of(EnhancedType.listOf(Object.class), "element"),
            Arguments.of(EnhancedType.setOf(Object.class), "element"),
            Arguments.of(EnhancedType.mapOf(String.class, Object.class), "value"));
    }

    private static <T> StaticTableSchema.Builder<Map<String, T>> schemaWithAttribute(EnhancedType<T> attributeType) {
        return StaticTableSchema.builder(EnhancedType.mapOf(EnhancedType.of(String.class), attributeType))
                                .newItemSupplier(HashMap::new)
                                .addAttribute(attributeType, a -> a.name("value")
                                                                   .getter(item -> item.get("value"))
                                                                   .setter((item, value) -> item.put("value", value)));
    }

    private static String missingNestedConverterMessage(EnhancedType<?> nestedType,
                                                        EnhancedType<?> collectionType,
                                                        String elementOrValue) {
        return "Converter not found for " + nestedType
               + " (" + elementOrValue + " of " + collectionType + "). "
               + "DefaultAttributeConverterProvider resolves collection " + elementOrValue
               + " types only from its own converters.";
    }

    private static Stream<AttributeConverter<?>> customConverters() {
        return Stream.of(new CustomStringConverter<>(EnhancedType.of(Object.class), Object::toString, value -> value),
                         new CustomStringConverter<>(EnhancedType.of(CustomUnsupportedType.class), value -> value.value,
                                                     CustomUnsupportedType::new));
    }

    private static Stream<EnhancedType<?>> unsupportedTypes() {
        return Stream.of(EnhancedType.of(Object.class), EnhancedType.of(CustomUnsupportedType.class));
    }

    private static Stream<EnhancedType<?>> rawCollectionTypes() {
        return Stream.of(EnhancedType.of(Map.class), EnhancedType.of(Set.class), EnhancedType.of(List.class));
    }

    private static final class CustomAttributeConverterProvider implements AttributeConverterProvider {
        private final AttributeConverter<?> converter;

        private CustomAttributeConverterProvider(AttributeConverter<?> converter) {
            this.converter = converter;
        }

        @Override
        public <T> AttributeConverter<T> converterFor(EnhancedType<T> type) {
            return converter.type().equals(type) ? (AttributeConverter<T>) converter : null;
        }
    }

    private static final class CustomStringConverter<T> implements AttributeConverter<T> {
        private final EnhancedType<T> type;
        private final Function<T, String> encoder;
        private final Function<String, T> decoder;

        private CustomStringConverter(EnhancedType<T> type, Function<T, String> encoder, Function<String, T> decoder) {
            this.type = type;
            this.encoder = encoder;
            this.decoder = decoder;
        }

        @Override
        public AttributeValue transformFrom(T input) {
            return AttributeValue.fromS(encoder.apply(input));
        }

        @Override
        public T transformTo(AttributeValue input) {
            return decoder.apply(input.s());
        }

        @Override
        public EnhancedType<T> type() {
            return type;
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.S;
        }
    }

    /**
     * A custom type with no converter registered for it.
     */
    private static class CustomUnsupportedType {
        private final String value;

        private CustomUnsupportedType(String value) {
            this.value = value;
        }
    }
}
