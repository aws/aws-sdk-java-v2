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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.testutils.LogCaptor;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

class TransferManagerLoggingTest {

    @Test
    void transferManager_withCrtClient_shouldNotLogWarnMessages() {

        try (S3AsyncClient s3Crt = S3AsyncClient.crtBuilder()
                                                .region(Region.US_WEST_2)
                                                .credentialsProvider(() -> AwsBasicCredentials.create("foo", "bar"))
                                                .build();
             LogCaptor logCaptor = LogCaptor.create(Level.WARN);
             S3TransferManager tm = S3TransferManager.builder().s3Client(s3Crt).build()) {
            List<LogEvent> events = logCaptor.loggedEvents();
            assertThat(events).isEmpty();
        }
    }

    @Test
    void transferManager_withJavaClient_shouldLogWarnMessage() {


        try (S3AsyncClient s3Crt = S3AsyncClient.builder()
                                                .region(Region.US_WEST_2)
                                                .credentialsProvider(() -> AwsBasicCredentials.create("foo", "bar"))
                                                .build();
             LogCaptor logCaptor = LogCaptor.create(Level.WARN);
             S3TransferManager tm = S3TransferManager.builder().s3Client(s3Crt).build()) {
            List<LogEvent> events = logCaptor.loggedEvents();
            assertLogged(events, Level.WARN, "The provided DefaultS3AsyncClient is not an instance of S3CrtAsyncClient, and "
                                             + "thus multipart upload/download feature is not enabled and resumable file upload"
                                             + " is "
                                             + "not supported. To benefit from maximum throughput, consider using "
                                             + "S3AsyncClient.crtBuilder().build() instead.");
        }
    }

    private static void assertLogged(List<LogEvent> events, org.apache.logging.log4j.Level level, String message) {
        assertThat(events).withFailMessage("Expecting events to not be empty").isNotEmpty();
        LogEvent event = events.remove(0);
        String msg = event.getMessage().getFormattedMessage();
        assertThat(msg).isEqualTo(message);
        assertThat(event.getLevel()).isEqualTo(level);
    }
}
