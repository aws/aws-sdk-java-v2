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

package software.amazon.awssdk.core.retry;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.testutils.LogCaptor;

class ClockSkewTest {

    @Test
    void getServerTime_whenDateHeaderContainsControlCharacters_logsEscapedDateAndThrowable() {
        SdkHttpResponse response = SdkHttpResponse.builder()
                                                  .statusCode(400)
                                                  .putHeader("Date", "bad\r\nFORGED")
                                                  .build();

        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            assertThat(ClockSkew.getServerTime(response)).isEmpty();

            assertThat(logCaptor.loggedEvents())
                .filteredOn(event -> event.getLoggerName().equals(ClockSkew.class.getName()))
                .hasSize(2)
                .allSatisfy(event -> assertThat(event.getMessage().getFormattedMessage())
                    .contains("bad\\r\\nFORGED")
                    .doesNotContain("\r", "\n"));
            assertThat(logCaptor.loggedEvents())
                .filteredOn(event -> event.getLevel() == Level.WARN)
                .singleElement()
                .satisfies(event -> assertThat(event.getThrown().toString())
                    .contains("bad\\r\\nFORGED")
                    .doesNotContain("\r", "\n"));
        }
    }
}
