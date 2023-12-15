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

package software.amazon.awssdk.thirdparty.org.slf4j.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.testutils.LogCaptor;
import software.amazon.awssdk.thirdparty.org.slf4j.Logger;
import software.amazon.awssdk.thirdparty.org.slf4j.LoggerFactory;
import software.amazon.awssdk.thirdparty.org.slf4j.Marker;
import software.amazon.awssdk.thirdparty.org.slf4j.MarkerFactory;

public class LoggingBindingTest {
    private static final LogCaptor LOG4J_CAPTOR = LogCaptor.create(Level.TRACE);

    @BeforeEach
    public void setup() {
        LOG4J_CAPTOR.clear();
    }

    @AfterAll
    public static void teardown() {
        LOG4J_CAPTOR.close();
    }

    @Test
    public void logMessage_delegatesToRealImplementation() {
        // We obtain an SLF4J Logger via the shaded LoggerFactory interface, and log a debug message through it
        Logger logger = LoggerFactory.getLogger("StaticLoggerBinderTest");
        String message = "This is a message logged from test logMessage_delegatesToRealImplementation()";
        logger.debug(message);

        // We expect the message above to be routed to Log4j, as it is the SLF4J binding we are using
        List<String> loggedMessages = LOG4J_CAPTOR.loggedEvents().stream()
                                                  .map(e -> e.getMessage().getFormattedMessage())
                                                  .collect(Collectors.toList());

        assertThat(loggedMessages).containsExactly(message);
    }

    @Test
    public void logMessage_withMarker_delegatesToRealImplementation() {
        // We obtain an SLF4J Logger via the shaded LoggerFactory interface, and log a debug message through it
        Logger logger = LoggerFactory.getLogger("StaticLoggerBinderTest");
        Marker myMarker = MarkerFactory.getMarker("MyMarker");
        String message = "This is a message logged from test logMessage_withMarker_delegatesToRealImplementation()";
        logger.debug(myMarker, message);

        // We expect the message above to be routed to Log4j, as it is the SLF4J binding we are using
        List<LogEvent> logEvents = LOG4J_CAPTOR.loggedEvents();
        assertThat(logEvents).hasSize(1);

        LogEvent event = logEvents.get(0);
        assertThat(event.getMarker().getName()).isEqualTo("MyMarker");
        assertThat(event.getMessage().getFormattedMessage()).isEqualTo(message);
    }
}
