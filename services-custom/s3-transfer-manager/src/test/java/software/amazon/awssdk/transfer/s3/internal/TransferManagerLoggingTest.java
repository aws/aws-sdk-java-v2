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

package software.amazon.awssdk.transfer.s3.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Predicate;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.testutils.LogCaptor;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.utils.internal.SystemSettingUtilsTestBackdoor;

class TransferManagerLoggingTest {

    private static final String EXPECTED_DEBUG_MESSAGE = "The provided S3AsyncClient is neither "
                                                         + "an AWS CRT-based S3 async client (S3AsyncClient.crtBuilder().build()) or "
                                                         + "a Java-based S3 async client (S3AsyncClient.builder().multipartEnabled(true).build()), "
                                                         + "and thus multipart upload/download feature may not be enabled and resumable file upload may not "
                                                         + "be supported. To benefit from maximum throughput, consider using "
                                                         + "S3AsyncClient.crtBuilder().build() or "
                                                         + "S3AsyncClient.builder().multipartEnabled(true).build() instead";

    @Test
    void transferManager_withCrtClient_shouldNotLogWarnMessages() {

        try (S3AsyncClient s3Crt = S3AsyncClient.crtBuilder()
                                                .region(Region.US_WEST_2)
                                                .credentialsProvider(() -> AwsBasicCredentials.create("foo", "bar"))
                                                .build();
             LogCaptor logCaptor = LogCaptor.create(Level.WARN);
             S3TransferManager tm = S3TransferManager.builder().s3Client(s3Crt).build()) {
            List<LogEvent> events = logCaptor.loggedEvents();
            assertThat(events)
                .withFailMessage("A message from S3TransferManager was logged at DEBUG level when none was expected")
                .noneMatch(loggedFromS3TransferManager());
        }
    }

    @Test
    void transferManager_withJavaClientMultiPartNotSet_shouldLogDebugMessage() {

        try (S3AsyncClient s3Crt = S3AsyncClient.builder()
                                                .region(Region.US_WEST_2)
                                                .credentialsProvider(() -> AwsBasicCredentials.create("foo", "bar"))
                                                .build();
             LogCaptor logCaptor = LogCaptor.create(Level.DEBUG);
             S3TransferManager tm = S3TransferManager.builder().s3Client(s3Crt).build()) {
            List<LogEvent> events = logCaptor.loggedEvents();
            assertLogged(events, Level.DEBUG, EXPECTED_DEBUG_MESSAGE);
        }
    }

    @Test
    void transferManager_withJavaClientMultiPartEqualsFalse_shouldLogDebugMessage() {

        try (S3AsyncClient s3Crt = S3AsyncClient.builder()
                                                .region(Region.US_WEST_2)
                                                .credentialsProvider(() -> AwsBasicCredentials.create("foo", "bar"))
                                                .multipartEnabled(false)
                                                .build();
             LogCaptor logCaptor = LogCaptor.create(Level.DEBUG);
             S3TransferManager tm = S3TransferManager.builder().s3Client(s3Crt).build()) {
            List<LogEvent> events = logCaptor.loggedEvents();
            assertLogged(events, Level.DEBUG, EXPECTED_DEBUG_MESSAGE);
        }
    }

    @Test
    void transferManager_withDefaultClient_shouldNotLogDebugMessage() {

        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride("AWS_REGION", "us-east-1");
        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG);
             S3TransferManager tm = S3TransferManager.builder().build()) {
            List<LogEvent> events = logCaptor.loggedEvents();
            assertThat(events)
                .withFailMessage("A message from S3TransferManager was logged at DEBUG level when none was expected")
                .noneMatch(loggedFromS3TransferManager());
        }
        SystemSettingUtilsTestBackdoor.clearEnvironmentVariableOverrides();
    }

    @Test
    void transferManager_withMultiPartEnabledJavaClient_shouldNotLogDebugMessage() {

        try (S3AsyncClient s3Crt = S3AsyncClient.builder()
                                                .region(Region.US_WEST_2)
                                                .credentialsProvider(() -> AwsBasicCredentials.create("foo", "bar"))
                                                .multipartEnabled(true)
                                                .build();
             LogCaptor logCaptor = LogCaptor.create(Level.DEBUG);
             S3TransferManager tm = S3TransferManager.builder().s3Client(s3Crt).build()) {
            List<LogEvent> events = logCaptor.loggedEvents();
            assertThat(events)
                .withFailMessage("A message from S3TransferManager was logged at DEBUG level when none was expected")
                .noneMatch(loggedFromS3TransferManager());
        }
    }

    @Test
    void transferManager_withMultiPartEnabledAndCrossRegionEnabledJavaClient_shouldNotLogDebugMessage() {

        try (S3AsyncClient s3Crt = S3AsyncClient.builder()
                                                .region(Region.US_WEST_2)
                                                .credentialsProvider(() -> AwsBasicCredentials.create("foo", "bar"))
                                                .multipartEnabled(true)
                                                .crossRegionAccessEnabled(true)
                                                .build();
             LogCaptor logCaptor = LogCaptor.create(Level.DEBUG);
             S3TransferManager tm = S3TransferManager.builder().s3Client(s3Crt).build()) {
            List<LogEvent> events = logCaptor.loggedEvents();
            assertThat(events)
                .withFailMessage("A message from S3TransferManager was logged at DEBUG level when none was expected")
                .noneMatch(loggedFromS3TransferManager());
        }
    }

    private static void assertLogged(List<LogEvent> events, org.apache.logging.log4j.Level level, String message) {
        assertThat(events).withFailMessage("Expecting events to not be empty").isNotEmpty();
        LogEvent event = events.remove(0);
        String msg = event.getMessage().getFormattedMessage();
        assertThat(msg).isEqualTo(message);
        assertThat(event.getLevel()).isEqualTo(level);
    }

    private static Predicate<LogEvent> loggedFromS3TransferManager() {
        String tmLoggerName = "software.amazon.awssdk.transfer.s3.S3TransferManager";
        return logEvent -> tmLoggerName.equals(logEvent.getLoggerName());
    }
}
