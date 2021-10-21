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
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.testutils.LogCaptor;
import software.amazon.awssdk.transfer.s3.CompletedTransfer;
import software.amazon.awssdk.transfer.s3.TransferRequest;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgress;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgressSnapshot;
import software.amazon.awssdk.transfer.s3.internal.progress.TransferListenerContext;

public class LoggingTransferListenerTest {

    private static final long TRANSFER_SIZE_IN_BYTES = 1024L;

    private DefaultTransferProgress progress;
    private TransferListenerContext context;
    private LoggingTransferListener listener;

    @Before
    public void setUp() throws Exception {
        TransferProgressSnapshot snapshot = DefaultTransferProgressSnapshot.builder()
                                                                           .transferSizeInBytes(TRANSFER_SIZE_IN_BYTES)
                                                                           .build();
        progress = new DefaultTransferProgress(snapshot);
        context = TransferListenerContext.builder()
                                         .request(mock(TransferRequest.class))
                                         .progressSnapshot(snapshot)
                                         .build();
        listener = LoggingTransferListener.create();
    }

    @Test
    public void test_defaultListener_successfulTransfer() {
        try (LogCaptor logCaptor = new LogCaptor.DefaultLogCaptor(Level.ALL)) {
            invokeSuccessfulLifecycle();
            List<LoggingEvent> events = logCaptor.loggedEvents();
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
        try (LogCaptor logCaptor = new LogCaptor.DefaultLogCaptor(Level.ALL)) {
            listener = LoggingTransferListener.create(5);
            invokeSuccessfulLifecycle();
            List<LoggingEvent> events = logCaptor.loggedEvents();
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
                progress.updateAndGet(p -> p.bytesTransferred(bytes)))));
        }

        listener.transferComplete(context.copy(b -> b.progressSnapshot(progress.snapshot())
                                                     .completedTransfer(mock(CompletedTransfer.class))));
    }

    private void assertLogged(List<LoggingEvent> events, Level level, String message) {
        LoggingEvent event = events.remove(0);
        assertThat(event.getLevel()).isEqualTo(level);
        assertThat(event.getMessage()).isEqualTo(message);
    }
}