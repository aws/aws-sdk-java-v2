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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

    @Test
    void findConverter_whenConverterIsCached_returnsTheCachedConverter() {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();

        assertThat(provider.converterFor(EnhancedType.of(String.class)))
            .isSameAs(provider.converterFor(EnhancedType.of(String.class)));
    }

    @Test
    void findConverter_whenMapSubtypeHasSupportedEntries_throwsConverterNotFound() {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();
        EnhancedType<HashMap<String, Integer>> type = new EnhancedType<HashMap<String, Integer>>() { };

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("An Object map value fails converter lookup")
    void findConverter_whenMapEntryValueIsObject_throwsConverterNotFound() {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();

        assertThatThrownBy(() -> provider.converterFor(EnhancedType.mapOf(String.class, Object.class)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Object.class));
    }

    @Test
    void findConverter_whenMapEntryValueHasNoConverter_throwsConverterNotFound() {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();

        assertThatThrownBy(() -> provider.converterFor(EnhancedType.mapOf(String.class, CustomUnsupportedType.class)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(CustomUnsupportedType.class));
    }

    @Test
    void findConverter_whenSetHasSupportedEntries_createsSetConverter() {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();

        assertThat(provider.converterFor(EnhancedType.setOf(String.class))
                           .transformFrom(Collections.singleton("value")).ss())
            .containsExactly("value");
    }

    @Test
    void findConverter_whenSetEntryHasNoConverter_throwsConverterNotFound() {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();

        assertThatThrownBy(() -> provider.converterFor(EnhancedType.setOf(CustomUnsupportedType.class)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(CustomUnsupportedType.class));
    }

    @Test
    void findConverter_whenListHasSupportedEntries_createsListConverter() {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();

        assertThat(provider.converterFor(EnhancedType.listOf(String.class))
                           .transformFrom(Collections.singletonList("value")).l())
            .containsExactly(AttributeValue.builder().s("value").build());
    }

    @Test
    void findConverter_whenListEntryHasNoConverter_throwsConverterNotFound() {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();

        assertThatThrownBy(() -> provider.converterFor(EnhancedType.listOf(CustomUnsupportedType.class)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(CustomUnsupportedType.class));
    }

    @Test
    void findConverter_whenTypeIsEnum_createsEnumConverter() {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();

        assertThat(provider.converterFor(EnhancedType.of(TestEnum.class)).transformFrom(TestEnum.VALUE).s())
            .isEqualTo("VALUE");
    }

    @Test
    void findConverter_whenTypeHasTableSchema_createsDocumentConverter() {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();
        TableSchema<TestDocument> schema = StaticTableSchema.builder(TestDocument.class)
                                                            .newItemSupplier(TestDocument::new)
                                                            .addAttribute(String.class, a -> a.name("value")
                                                                                       .getter(TestDocument::value)
                                                                                       .setter(TestDocument::value))
                                                            .build();

        assertThat(provider.converterFor(EnhancedType.documentOf(TestDocument.class, schema))
                           .transformFrom(new TestDocument("value")).m())
            .containsEntry("value", AttributeValue.builder().s("value").build());
    }

    /**
     * A custom type with no converter registered for it.
     */
    private static class CustomUnsupportedType {
    }

    private enum TestEnum {
        VALUE
    }

    private static final class TestDocument {
        private String value;

        private TestDocument() {
        }

        private TestDocument(String value) {
            this.value = value;
        }

        private String value() {
            return value;
        }

        private void value(String value) {
            this.value = value;
        }
    }
}
