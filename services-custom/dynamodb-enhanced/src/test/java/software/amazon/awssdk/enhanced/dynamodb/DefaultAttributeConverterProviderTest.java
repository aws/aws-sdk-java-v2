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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.event.Level;

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

    @Test
    void findConverter_whenObjectType_throwsConverterNotFound() {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();
        EnhancedType<Object> type = EnhancedType.of(Object.class);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @ParameterizedTest
    @MethodSource("rawCollectionTypes")
    void findConverter_whenRawCollectionType_throwsConverterNotFound(EnhancedType<?> type) {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type + ". Type parameters are required for this type.");
    }

    @SuppressWarnings("unchecked")
    private static Stream<EnhancedType<?>> rawCollectionTypes() {
        return Stream.of(EnhancedType.of(Map.class), EnhancedType.of(Set.class), EnhancedType.of(List.class));
    }

    @Test
    @DisplayName("An Object map value fails converter lookup")
    void findConverter_whenMapEntryValueIsObject_throwsConverterNotFound() {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();

        assertThatThrownBy(() -> provider.converterFor(EnhancedType.mapOf(String.class, Object.class)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(missingNestedConverterMessage(EnhancedType.of(Object.class),
                                                      EnhancedType.mapOf(String.class, Object.class),
                                                      "value"));
    }

    @Test
    void findConverter_whenMapEntryValueHasNoConverter_throwsConverterNotFound() {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();

        assertThatThrownBy(() -> provider.converterFor(EnhancedType.mapOf(String.class, CustomUnsupportedType.class)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(missingNestedConverterMessage(EnhancedType.of(CustomUnsupportedType.class),
                                                      EnhancedType.mapOf(String.class, CustomUnsupportedType.class),
                                                      "value"));
    }

    @Test
    void findConverter_whenSetEntryHasNoConverter_throwsConverterNotFound() {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();

        assertThatThrownBy(() -> provider.converterFor(EnhancedType.setOf(CustomUnsupportedType.class)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(missingNestedConverterMessage(EnhancedType.of(CustomUnsupportedType.class),
                                                      EnhancedType.setOf(CustomUnsupportedType.class),
                                                      "element"));
    }

    @Test
    void findConverter_whenListEntryHasNoConverter_throwsConverterNotFound() {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();

        assertThatThrownBy(() -> provider.converterFor(EnhancedType.listOf(CustomUnsupportedType.class)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(missingNestedConverterMessage(EnhancedType.of(CustomUnsupportedType.class),
                                                      EnhancedType.listOf(CustomUnsupportedType.class),
                                                      "element"));
    }

    private static String missingNestedConverterMessage(EnhancedType<?> nestedType,
                                                        EnhancedType<?> collectionType,
                                                        String role) {
        return "Converter not found for " + nestedType
               + " (" + role + " of " + collectionType + "). "
               + "DefaultAttributeConverterProvider resolves collection " + role
               + " types only from its own converters.";
    }

    /**
     * A custom type with no converter registered for it.
     */
    private static class CustomUnsupportedType {
    }
}
