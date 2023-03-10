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
import nl.altindag.log.LogCaptor;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        LogCaptor logCaptor = LogCaptor.forClass(LoggingTransferListener.class);
        invokeSuccessfulLifecycle();
        List<String> infoLogs = logCaptor.getInfoLogs();
        assertThat(infoLogs).contains("Transfer initiated...");
        assertThat(infoLogs).contains("|                    | 0.0%");
        assertThat(infoLogs).contains("|=                   | 5.0%");
        assertThat(infoLogs).contains("|==                  | 10.0%");
        assertThat(infoLogs).contains("|===                 | 15.0%");
        assertThat(infoLogs).contains("|====                | 20.0%");
        assertThat(infoLogs).contains("|=====               | 25.0%");
        assertThat(infoLogs).contains("|======              | 30.0%");
        assertThat(infoLogs).contains("|=======             | 35.0%");
        assertThat(infoLogs).contains("|========            | 40.0%");
        assertThat(infoLogs).contains("|=========           | 45.0%");
        assertThat(infoLogs).contains("|==========          | 50.0%");
        assertThat(infoLogs).contains("|===========         | 55.0%");
        assertThat(infoLogs).contains("|============        | 60.0%");
        assertThat(infoLogs).contains("|=============       | 65.0%");
        assertThat(infoLogs).contains("|==============      | 70.0%");
        assertThat(infoLogs).contains("|===============     | 75.0%");
        assertThat(infoLogs).contains("|================    | 80.0%");
        assertThat(infoLogs).contains("|=================   | 85.0%");
        assertThat(infoLogs).contains("|==================  | 90.0%");
        assertThat(infoLogs).contains("|=================== | 95.0%");
        assertThat(infoLogs).contains("|====================| 100.0%");
        assertThat(infoLogs).contains("Transfer complete!");
    }

    @Test
    public void test_customTicksListener_successfulTransfer() {
        LogCaptor logCaptor = LogCaptor.forClass(LoggingTransferListener.class);
        listener = LoggingTransferListener.create(5);
        invokeSuccessfulLifecycle();
        List<String> infoLogs = logCaptor.getInfoLogs();
        assertThat(infoLogs).contains("Transfer initiated...");
        assertThat(infoLogs).contains("|     | 0.0%");
        assertThat(infoLogs).contains("|=    | 20.0%");
        assertThat(infoLogs).contains("|==   | 40.0%");
        assertThat(infoLogs).contains("|===  | 60.0%");
        assertThat(infoLogs).contains("|==== | 80.0%");
        assertThat(infoLogs).contains("|=====| 100.0%");
        assertThat(infoLogs).contains("Transfer complete!");
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