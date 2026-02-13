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
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;
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
                .hasMessageContaining("Converter not found for EnhancedType(software.amazon.awssdk.enhanced.dynamodb"
                                      + ".DefaultAttributeConverterProviderTest$CustomUnsupportedType)");
            List<LogEvent> logEvents = logCaptor.loggedEvents();
            assertThat(logEvents).hasSize(1);
            assertThat(logEvents.get(0).getLevel().name()).isEqualTo(Level.DEBUG.name());
            assertThat(logEvents.get(0).getMessage().getFormattedMessage())
                .contains("No converter available for EnhancedType(software.amazon.awssdk.enhanced.dynamodb"
                          + ".DefaultAttributeConverterProviderTest$CustomUnsupportedType)");
        }
    }

    /**
     * A custom type with no converter registered for it.
     */
    private static class CustomUnsupportedType {
    }
}
