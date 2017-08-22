package software.amazon.awssdk.services.sqs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;

/**
 * This is a manually run test that can be run to test idle connection reaping in the SDK.
 * Connections sitting around in the connection pool for too long will eventually be terminated by
 * the AWS end of the connection, and will go into CLOSE_WAIT. If this happens, sockets will sit
 * around in CLOSE_WAIT, still using resources on the client side to manage that socket. At its
 * worse, this can cause the client to be unable to create any new connections until the CLOSE_WAIT
 * sockets are eventually expired and released.
 */
public class SqsConcurrentPerformanceIntegrationTest extends IntegrationTestBase {

    /** Total number of worker threads to hit SQS. */
    private static final int TOTAL_WORKER_THREADS = 30;
    private SQSAsyncClient sqs;

    /**
     * Spins up a pool of threads to make concurrent requests and thus grow the runtime's HTTP
     * connection pool, then sits idle.
     * <p>
     * You can use the netstat command to look at the current sockets connected to SQS and verify
     * that they don't sit around in CLOSE_WAIT, and are correctly being reaped.
     */
    @Test
    @Ignore
    public void testIdleConnectionReaping() throws Exception {
        sqs = SQSAsyncClient.builder().credentialsProvider(getCredentialsProvider()).build();
        sqs = SQSAsyncClient.builder().credentialsProvider(getCredentialsProvider()).build();

        List<WorkerThread> workers = new ArrayList<WorkerThread>();
        for (int i = 0; i < TOTAL_WORKER_THREADS; i++) {
            workers.add(new WorkerThread());
        }

        for (WorkerThread worker : workers) {
            worker.start();
        }

        // Sleep for five minutes to let the sockets go idle
        Thread.sleep(1000 * 60 * 5);

        // Wait for the user to acknowledge test before we exit the JVM
        System.out.println("Test complete");
        waitForUserInput();
    }

    private class WorkerThread extends Thread {
        @Override
        public void run() {
            sqs.listQueues(ListQueuesRequest.builder().build());
            sqs.listQueues(ListQueuesRequest.builder().build());
        }
    }

    private void waitForUserInput() throws IOException {
        new BufferedReader(new InputStreamReader(System.in)).readLine();
    }
}
