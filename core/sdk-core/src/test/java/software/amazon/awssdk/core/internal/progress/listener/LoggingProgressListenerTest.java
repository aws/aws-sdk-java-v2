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

package software.amazon.awssdk.core.internal.progress.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.internal.progress.ProgressListenerContext;
import software.amazon.awssdk.core.internal.progress.snapshot.DefaultProgressSnapshot;
import software.amazon.awssdk.core.progress.snapshot.ProgressSnapshot;
import software.amazon.awssdk.testutils.LogCaptor;

public class LoggingProgressListenerTest {
    private static final long UPLOAD_SIZE_IN_BYTES = 1024L;
    private static final long DOWNLOAD_SIZE_IN_BYTES = 1024L;
    private DefaultSdkExchangeProgress requestBodyProgress;
    private DefaultSdkExchangeProgress responseBodyProgress;
    private ProgressListenerContext context;
    private LoggingProgressListener listener;

    @BeforeEach
    public void setUp() throws Exception {
        ProgressSnapshot uploadSnapshot = DefaultProgressSnapshot.builder()
                                                           .transferredBytes(0L)
                                                           .totalBytes(UPLOAD_SIZE_IN_BYTES)
                                                           .build();

        ProgressSnapshot downloadSnapshot = DefaultProgressSnapshot.builder()
                                                                 .transferredBytes(0L)
                                                                 .totalBytes(DOWNLOAD_SIZE_IN_BYTES)
                                                                 .build();
        requestBodyProgress = new DefaultSdkExchangeProgress(uploadSnapshot);
        responseBodyProgress = new DefaultSdkExchangeProgress(downloadSnapshot);
        context = ProgressListenerContext.builder()
                                         .request(mock(NoopTestRequest.class))
                                         .uploadProgressSnapshot(uploadSnapshot)
                                         .downloadProgressSnapshot(downloadSnapshot)
                                         .build();
        listener = LoggingProgressListener.create();
    }

    @Test
    public void defaultListener_successfulTransfer() {
        try (LogCaptor logCaptor = LogCaptor.create()) {
            invokeSuccessfulLifecycle();
            List<LogEvent> events = logCaptor.loggedEvents();
            assertLogged(events, Level.INFO, "Request Prepared...");
            assertLogged(events, Level.INFO, "|                    | 0.0%");
            assertLogged(events, Level.INFO, "|=                   | 5.0%");
            assertLogged(events, Level.INFO, "|==                  | 10.0%");
            assertLogged(events, Level.INFO, "|===                 | 15.0%");
            assertLogged(events, Level.INFO, "|====                | 20.0%");
            assertLogged(events, Level.INFO, "|=====               | 25.0%");
            assertLogged(events, Level.INFO, "|======              | 30.0%");
            assertLogged(events, Level.INFO, "|=======             | 35.0%");
            assertLogged(events, Level.INFO, "|========            | 40.0%");
            assertLogged(events, Level.INFO, "|=========           | 45.0%");
            assertLogged(events, Level.INFO, "|==========          | 50.0%");
            assertLogged(events, Level.INFO, "|===========         | 55.0%");
            assertLogged(events, Level.INFO, "|============        | 60.0%");
            assertLogged(events, Level.INFO, "|=============       | 65.0%");
            assertLogged(events, Level.INFO, "|==============      | 70.0%");
            assertLogged(events, Level.INFO, "|===============     | 75.0%");
            assertLogged(events, Level.INFO, "|================    | 80.0%");
            assertLogged(events, Level.INFO, "|=================   | 85.0%");
            assertLogged(events, Level.INFO, "|==================  | 90.0%");
            assertLogged(events, Level.INFO, "|=================== | 95.0%");
            assertLogged(events, Level.INFO, "|====================| 100.0%");
            assertLogged(events, Level.INFO, "Upload Successful! Starting Download...");
            assertLogged(events, Level.INFO, "|                    | 0.0%");
            assertLogged(events, Level.INFO, "|=                   | 5.0%");
            assertLogged(events, Level.INFO, "|==                  | 10.0%");
            assertLogged(events, Level.INFO, "|===                 | 15.0%");
            assertLogged(events, Level.INFO, "|====                | 20.0%");
            assertLogged(events, Level.INFO, "|=====               | 25.0%");
            assertLogged(events, Level.INFO, "|======              | 30.0%");
            assertLogged(events, Level.INFO, "|=======             | 35.0%");
            assertLogged(events, Level.INFO, "|========            | 40.0%");
            assertLogged(events, Level.INFO, "|=========           | 45.0%");
            assertLogged(events, Level.INFO, "|==========          | 50.0%");
            assertLogged(events, Level.INFO, "|===========         | 55.0%");
            assertLogged(events, Level.INFO, "|============        | 60.0%");
            assertLogged(events, Level.INFO, "|=============       | 65.0%");
            assertLogged(events, Level.INFO, "|==============      | 70.0%");
            assertLogged(events, Level.INFO, "|===============     | 75.0%");
            assertLogged(events, Level.INFO, "|================    | 80.0%");
            assertLogged(events, Level.INFO, "|=================   | 85.0%");
            assertLogged(events, Level.INFO, "|==================  | 90.0%");
            assertLogged(events, Level.INFO, "|=================== | 95.0%");
            assertLogged(events, Level.INFO, "|====================| 100.0%");
            assertLogged(events, Level.INFO, "Execution Successful!");
            assertThat(events).isEmpty();
        }
    }

    @Test
    public void test_customTicksListener_successfulTransfer() {
        try (LogCaptor logCaptor = LogCaptor.create()) {
            listener = LoggingProgressListener.create(5);
            invokeSuccessfulLifecycle();
            List<LogEvent> events = logCaptor.loggedEvents();
            assertLogged(events, Level.INFO, "Request Prepared...");
            assertLogged(events, Level.INFO, "|     | 0.0%");
            assertLogged(events, Level.INFO, "|=    | 20.0%");
            assertLogged(events, Level.INFO, "|==   | 40.0%");
            assertLogged(events, Level.INFO, "|===  | 60.0%");
            assertLogged(events, Level.INFO, "|==== | 80.0%");
            assertLogged(events, Level.INFO, "|=====| 100.0%");
            assertLogged(events, Level.INFO, "Upload Successful! Starting Download...");
            assertLogged(events, Level.INFO, "|     | 0.0%");
            assertLogged(events, Level.INFO, "|=    | 20.0%");
            assertLogged(events, Level.INFO, "|==   | 40.0%");
            assertLogged(events, Level.INFO, "|===  | 60.0%");
            assertLogged(events, Level.INFO, "|==== | 80.0%");
            assertLogged(events, Level.INFO, "|=====| 100.0%");
            assertLogged(events, Level.INFO, "Execution Successful!");
            assertThat(events).isEmpty();
        }
    }

    private void invokeSuccessfulLifecycle() {
        listener.requestPrepared(context);

        listener.requestHeaderSent(context);

        for (int i = 0; i <= UPLOAD_SIZE_IN_BYTES; i++) {
            int bytes = i;
            listener.requestBytesSent(context.copy(c -> c.uploadProgressSnapshot(
                requestBodyProgress.updateAndGet(p -> p.transferredBytes((long) bytes)))));
        }
        listener.responseHeaderReceived(context);
        for (int i = 0; i <= DOWNLOAD_SIZE_IN_BYTES; i++) {
            int bytes = i;
            listener.responseBytesReceived(context.copy(c -> c.downloadProgressSnapshot(
                responseBodyProgress.updateAndGet(p -> p.transferredBytes((long) bytes)))));
        }

        listener.executionSuccess(context.copy(b -> b.downloadProgressSnapshot(responseBodyProgress.progressSnapshot())));
    }

    private static void assertLogged(List<LogEvent> events, org.apache.logging.log4j.Level level, String message) {
        assertThat(events).withFailMessage("Expecting events to not be empty").isNotEmpty();
        LogEvent event = events.remove(0);
        String msg = event.getMessage().getFormattedMessage();
        assertThat(msg).isEqualTo(message);
        assertThat(event.getLevel()).isEqualTo(level);
    }
}
