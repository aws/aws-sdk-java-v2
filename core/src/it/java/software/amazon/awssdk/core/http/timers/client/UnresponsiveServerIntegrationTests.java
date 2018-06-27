/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.http.timers.client;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.assertClientExecutionTimerExecutorNotCreated;
import static software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.assertNumberOfTasksTriggered;
import static software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.interruptCurrentThreadAfterDelay;
import static software.amazon.awssdk.core.internal.http.timers.TimeoutTestConstants.CLIENT_EXECUTION_TIMEOUT;
import static software.amazon.awssdk.core.internal.http.timers.TimeoutTestConstants.PRECISION_MULTIPLIER;
import static software.amazon.awssdk.core.internal.http.timers.TimeoutTestConstants.TEST_TIMEOUT;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.core.TestPreConditions;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.ClientExecutionTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.UnresponsiveMockServerTestBase;
import software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient;
import software.amazon.awssdk.core.retry.FixedTimeBackoffStrategy;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import utils.HttpTestUtils;

@Ignore
@ReviewBeforeRelease("Add the tests back once execution, request timeout are added back")
public class UnresponsiveServerIntegrationTests extends UnresponsiveMockServerTestBase {

    private static final Duration LONGER_SOCKET_TIMEOUT =
            Duration.ofMillis(CLIENT_EXECUTION_TIMEOUT.toMillis() * PRECISION_MULTIPLIER);
    private static final Duration SHORTER_SOCKET_TIMEOUT =
            Duration.ofMillis(CLIENT_EXECUTION_TIMEOUT.toMillis() / PRECISION_MULTIPLIER);

    private AmazonSyncHttpClient httpClient;

    @BeforeClass
    public static void preConditions() {
        TestPreConditions.assumeNotJava6();
    }

    @Test(timeout = TEST_TIMEOUT)
    public void clientExecutionTimeoutDisabled_SocketTimeoutExceptionIsThrown_NoThreadsCreated() {
        httpClient = HttpTestUtils.testClientBuilder().httpClient(createClientWithSocketTimeout(SHORTER_SOCKET_TIMEOUT)).build();

        try {
            httpClient.requestExecutionBuilder().request(newGetRequest()).execute();
            fail("Exception expected");
        } catch (SdkClientException e) {
            assertThat(e.getCause(), instanceOf(SocketTimeoutException.class));
            assertClientExecutionTimerExecutorNotCreated(httpClient.getClientExecutionTimer());
        }
    }

    /**
     * The client execution timer uses interrupts to abort the client but if another thread
     * interrupts the current thread for another reason we don't want to squash the
     * {@link InterruptedException}. We should set the thread's interrupted status and throw the
     * exception back out (we can't throw the actual {@link InterruptedException} because it's
     * checked)
     */
    @Test(timeout = TEST_TIMEOUT)
    public void interruptCausedBySomethingOtherThanTimer_PropagatesInterruptToCaller() {
        Duration socketTimeout = Duration.ofMillis(100);

        RetryPolicy retryPolicy = RetryPolicy.builder()
                                             .backoffStrategy(new FixedTimeBackoffStrategy(CLIENT_EXECUTION_TIMEOUT))
                                             .numRetries(1)
                                             .build();

        httpClient = new AmazonSyncHttpClient(HttpTestUtils.testClientConfiguration().toBuilder()
                                                           .option(SdkClientOption.SYNC_HTTP_CLIENT,
                                                                   HttpTestUtils.testSdkHttpClient())
                                                           .option(SdkClientOption.RETRY_POLICY, retryPolicy)
                                                           .build());

        // We make sure the first connection has failed due to the socket timeout before
        // interrupting so we know that we are sleeping per the backoff strategy. Apache HTTP
        // client doesn't seem to honor interrupts reliably but Thread.sleep does
        interruptCurrentThreadAfterDelay(socketTimeout.toMillis() * 2);

        try {
            httpClient.requestExecutionBuilder().request(newGetRequest()).execute();
            fail("Exception expected");
        } catch (SdkClientException e) {
            assertTrue(Thread.currentThread().isInterrupted());
            assertThat(e.getCause(), instanceOf(InterruptedException.class));
        }
    }

    @Test(timeout = TEST_TIMEOUT)
    public void clientExecutionTimeoutEnabled_WithLongerSocketTimeout_ThrowsClientExecutionTimeoutException()
            throws IOException {
        httpClient = HttpTestUtils.testClientBuilder().httpClient(createClientWithSocketTimeout(LONGER_SOCKET_TIMEOUT)).build();

        try {
            httpClient.requestExecutionBuilder().request(newGetRequest()).execute();
            fail("Exception expected");
        } catch (SdkClientException e) {
            assertThat(e, instanceOf(ClientExecutionTimeoutException.class));
            assertNumberOfTasksTriggered(httpClient.getClientExecutionTimer(), 1);
        }
    }

    private SdkHttpClient createClientWithSocketTimeout(Duration socketTimeout) {
        return ApacheHttpClient.builder()
                               .socketTimeout(socketTimeout)
                               .build();
    }

    @Test(timeout = TEST_TIMEOUT)
    public void clientExecutionTimeoutEnabled_WithShorterSocketTimeout_ThrowsSocketTimeoutException()
            throws IOException {
        httpClient = HttpTestUtils.testClientBuilder().httpClient(createClientWithSocketTimeout(SHORTER_SOCKET_TIMEOUT)).build();

        try {
            httpClient.requestExecutionBuilder().request(newGetRequest()).execute();
            fail("Exception expected");
        } catch (SdkClientException e) {
            assertThat(e.getCause(), instanceOf(SocketTimeoutException.class));
            assertNumberOfTasksTriggered(httpClient.getClientExecutionTimer(), 0);
        }
    }

}
