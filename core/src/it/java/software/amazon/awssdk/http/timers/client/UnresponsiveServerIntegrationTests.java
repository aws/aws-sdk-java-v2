/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.timers.client;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.assertClientExecutionTimerExecutorNotCreated;
import static software.amazon.awssdk.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.assertNumberOfTasksTriggered;
import static software.amazon.awssdk.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.interruptCurrentThreadAfterDelay;
import static software.amazon.awssdk.internal.http.timers.TimeoutTestConstants.CLIENT_EXECUTION_TIMEOUT;
import static software.amazon.awssdk.internal.http.timers.TimeoutTestConstants.PRECISION_MULTIPLIER;
import static software.amazon.awssdk.internal.http.timers.TimeoutTestConstants.TEST_TIMEOUT;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.TestPreConditions;
import software.amazon.awssdk.http.AmazonHttpClient;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.UnresponsiveMockServerTestBase;
import software.amazon.awssdk.http.apache.ApacheSdkHttpClientFactory;
import software.amazon.awssdk.http.exception.ClientExecutionTimeoutException;
import software.amazon.awssdk.retry.FixedTimeBackoffStrategy;
import software.amazon.awssdk.retry.PredefinedRetryPolicies;
import software.amazon.awssdk.retry.RetryPolicy;

public class UnresponsiveServerIntegrationTests extends UnresponsiveMockServerTestBase {

    private static final Duration LONGER_SOCKET_TIMEOUT = Duration.ofMillis(CLIENT_EXECUTION_TIMEOUT * PRECISION_MULTIPLIER);
    private static final Duration SHORTER_SOCKET_TIMEOUT = Duration.ofMillis(CLIENT_EXECUTION_TIMEOUT / PRECISION_MULTIPLIER);

    private AmazonHttpClient httpClient;

    @BeforeClass
    public static void preConditions() {
        TestPreConditions.assumeNotJava6();
    }

    @Test(timeout = TEST_TIMEOUT)
    public void clientExecutionTimeoutDisabled_SocketTimeoutExceptionIsThrown_NoThreadsCreated() {
        httpClient = AmazonHttpClient.builder()
                                     .sdkHttpClient(createClientWithSocketTimeout(SHORTER_SOCKET_TIMEOUT))
                                     .clientConfiguration(new LegacyClientConfiguration().withMaxErrorRetry(0))
                                     .build();

        try {
            httpClient.requestExecutionBuilder().request(newGetRequest()).execute();
            fail("Exception expected");
        } catch (AmazonClientException e) {
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
        httpClient = AmazonHttpClient.builder()
                                     .sdkHttpClient(createClientWithSocketTimeout(socketTimeout))
                                     .clientConfiguration(new LegacyClientConfiguration()
                                                                  .withClientExecutionTimeout(CLIENT_EXECUTION_TIMEOUT)
                                                                  .withRetryPolicy(
                                                                          new RetryPolicy(
                                                                                  PredefinedRetryPolicies.DEFAULT_RETRY_CONDITION,
                                                                                  new FixedTimeBackoffStrategy(
                                                                                          CLIENT_EXECUTION_TIMEOUT),
                                                                                  1, false)))
                                     .build();

        // We make sure the first connection has failed due to the socket timeout before
        // interrupting so we know that we are sleeping per the backoff strategy. Apache HTTP
        // client doesn't seem to honor interrupts reliably but Thread.sleep does
        interruptCurrentThreadAfterDelay(socketTimeout.toMillis() * 2);

        try {
            httpClient.requestExecutionBuilder().request(newGetRequest()).execute();
            fail("Exception expected");
        } catch (AmazonClientException e) {
            assertTrue(Thread.currentThread().isInterrupted());
            assertThat(e.getCause(), instanceOf(InterruptedException.class));
        }
    }

    @Test(timeout = TEST_TIMEOUT)
    public void clientExecutionTimeoutEnabled_WithLongerSocketTimeout_ThrowsClientExecutionTimeoutException()
            throws IOException {
        httpClient = AmazonHttpClient.builder()
                                     .sdkHttpClient(createClientWithSocketTimeout(LONGER_SOCKET_TIMEOUT))
                                     .clientConfiguration(new LegacyClientConfiguration()
                                                                  .withClientExecutionTimeout(CLIENT_EXECUTION_TIMEOUT)
                                                                  .withMaxErrorRetry(0))
                                     .build();

        try {
            httpClient.requestExecutionBuilder().request(newGetRequest()).execute();
            fail("Exception expected");
        } catch (AmazonClientException e) {
            assertThat(e, instanceOf(ClientExecutionTimeoutException.class));
            assertNumberOfTasksTriggered(httpClient.getClientExecutionTimer(), 1);
        }
    }

    private SdkHttpClient createClientWithSocketTimeout(Duration socketTimeout) {
        return ApacheSdkHttpClientFactory.builder()
                                         .socketTimeout(socketTimeout)
                                         .build()
                                         .createHttpClient();
    }

    @Test(timeout = TEST_TIMEOUT)
    public void clientExecutionTimeoutEnabled_WithShorterSocketTimeout_ThrowsSocketTimeoutException()
            throws IOException {
        httpClient = AmazonHttpClient.builder()
                                     .sdkHttpClient(createClientWithSocketTimeout(SHORTER_SOCKET_TIMEOUT))
                                     .clientConfiguration(new LegacyClientConfiguration()
                                                                  .withClientExecutionTimeout(CLIENT_EXECUTION_TIMEOUT)
                                                                  .withMaxErrorRetry(0))
                                     .build();

        try {
            httpClient.requestExecutionBuilder().request(newGetRequest()).execute();
            fail("Exception expected");
        } catch (AmazonClientException e) {
            assertThat(e.getCause(), instanceOf(SocketTimeoutException.class));
            assertNumberOfTasksTriggered(httpClient.getClientExecutionTimer(), 0);
        }
    }

}
