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

package software.amazon.awssdk.transfer.s3.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.testutils.LogCaptor;
import software.amazon.awssdk.transfer.s3.model.CompletedObjectTransfer;
import software.amazon.awssdk.transfer.s3.model.TransferObjectRequest;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgress;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgressSnapshot;
import software.amazon.awssdk.transfer.s3.internal.progress.TransferListenerContext;

public class LoggingTransferListenerTest {

    private static final long TRANSFER_SIZE_IN_BYTES = 1024L;

    private DefaultTransferProgress progress;
    private TransferListenerContext context;
    private LoggingTransferListener listener;

    @BeforeEach
    public void setUp() throws Exception {
        TransferProgressSnapshot snapshot = DefaultTransferProgressSnapshot.builder()
                                                                           .transferredBytes(0L)
                                                                           .totalBytes(TRANSFER_SIZE_IN_BYTES)
                                                                           .build();
        progress = new DefaultTransferProgress(snapshot);
        context = TransferListenerContext.builder()
                                         .request(mock(TransferObjectRequest.class))
                                         .progressSnapshot(snapshot)
                                         .build();
        listener = LoggingTransferListener.create();
    }

    @Test
    public void test_defaultListener_successfulTransfer() {
        try (LogCaptor logCaptor = LogCaptor.create()) {
            invokeSuccessfulLifecycle();
            List<LogEvent> events = logCaptor.loggedEvents();
            assertLogged(events, Level.INFO, "Transfer initiated...");
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
            assertLogged(events, Level.INFO, "Transfer complete!");
            assertThat(events).isEmpty();
        }
    }

    @Test
    public void test_customTicksListener_successfulTransfer() {
        try (LogCaptor logCaptor = LogCaptor.create()) {
            listener = LoggingTransferListener.create(5);
            invokeSuccessfulLifecycle();
            List<LogEvent> events = logCaptor.loggedEvents();
            assertLogged(events, Level.INFO, "Transfer initiated...");
            assertLogged(events, Level.INFO, "|     | 0.0%");
            assertLogged(events, Level.INFO, "|=    | 20.0%");
            assertLogged(events, Level.INFO, "|==   | 40.0%");
            assertLogged(events, Level.INFO, "|===  | 60.0%");
            assertLogged(events, Level.INFO, "|==== | 80.0%");
            assertLogged(events, Level.INFO, "|=====| 100.0%");
            assertLogged(events, Level.INFO, "Transfer complete!");
            assertThat(events).isEmpty();
        }
    }

    private void invokeSuccessfulLifecycle() {
        listener.transferInitiated(context);

        for (int i = 0; i <= TRANSFER_SIZE_IN_BYTES; i++) {
            int bytes = i;
            listener.bytesTransferred(context.copy(c -> c.progressSnapshot(
                progress.updateAndGet(p -> p.transferredBytes((long) bytes)))));
        }

        listener.transferComplete(context.copy(b -> b.progressSnapshot(progress.snapshot())
                                                     .completedTransfer(mock(CompletedObjectTransfer.class))));
    }

    private static void assertLogged(List<LogEvent> events, org.apache.logging.log4j.Level level, String message) {
        assertThat(events).withFailMessage("Expecting events to not be empty").isNotEmpty();
        LogEvent event = events.remove(0);
        String msg = event.getMessage().getFormattedMessage();
        assertThat(msg).isEqualTo(message);
        assertThat(event.getLevel()).isEqualTo(level);
    }
}