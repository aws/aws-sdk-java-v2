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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.createMockGetRequest;
import static software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.execute;
import static software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.interruptCurrentThreadAfterDelay;

import java.io.InputStream;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.exception.ClientExecutionTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.MockServerTestBase;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.http.server.MockServer;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient;
import software.amazon.awssdk.core.internal.http.request.SlowExecutionInterceptor;
import software.amazon.awssdk.core.internal.http.response.DummyResponseHandler;
import software.amazon.awssdk.core.internal.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.internal.interceptor.InterceptorContext;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.http.AbortableCallable;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import utils.HttpTestUtils;

@RunWith(MockitoJUnitRunner.class)
@Ignore
@ReviewBeforeRelease("add it back once execution time out is added back")
public class AbortedExceptionClientExecutionTimerIntegrationTest extends MockServerTestBase {

    private AmazonSyncHttpClient httpClient;

    @Mock
    private SdkHttpClient sdkHttpClient;

    @Mock
    private AbortableCallable<SdkHttpFullResponse> abortableCallable;

    @Before
    public void setup() throws Exception {
        when(sdkHttpClient.prepareRequest(any(), any())).thenReturn(abortableCallable);
        httpClient = HttpTestUtils.testClientBuilder().httpClient(sdkHttpClient).build();
        when(abortableCallable.call()).thenReturn(SdkHttpFullResponse.builder()
                                                                     .statusCode(200)
                                                                     .build());
    }

    @Override
    protected MockServer buildMockServer() {
        return new MockServer(MockServer.DummyResponseServerBehavior.build(200, "Hi", "Dummy response"));
    }

    @Test(expected = AbortedException.class)
    public void clientExecutionTimeoutEnabled_aborted_exception_occurs_timeout_not_expired() throws Exception {
        when(abortableCallable.call()).thenThrow(AbortedException.builder().build());

        execute(httpClient, createMockGetRequest());
    }

    @Test(expected = ClientExecutionTimeoutException.class)
    public void clientExecutionTimeoutEnabled_aborted_exception_occurs_timeout_expired() throws Exception {
        // Simulate a slow HTTP request
        when(abortableCallable.call()).thenAnswer(i -> {
            Thread.sleep(10_000);
            return null;
        });

        execute(httpClient, createMockGetRequest());
    }

    /**
     * Tests that a streaming operation has it's request properly cleaned up if the client is interrupted after the
     * response is received.
     *
     * see TT0070103230
     */
    @Test
    public void clientInterruptedDuringResponseHandlers_DoesNotLeakConnection() throws Exception {
        InputStream mockContent = mock(InputStream.class);
        when(abortableCallable.call()).thenReturn(SdkHttpFullResponse.builder()
                                                                     .statusCode(200)
                                                                     .content(new AbortableInputStream(mockContent, () -> { }))
                                                                     .build());
        interruptCurrentThreadAfterDelay(1000);
        try {
            requestBuilder()
                    .originalRequest(NoopTestRequest.builder().build())
                    .executionContext(withInterceptors(new SlowExecutionInterceptor().afterTransmissionWaitInSeconds(10)))
                    .execute(new DummyResponseHandler().leaveConnectionOpen());
            fail("Expected exception");
        } catch (SdkClientException e) {
            assertThat(e.getCause(), instanceOf(InterruptedException.class));
        }

        verify(mockContent).close();
    }

    private AmazonSyncHttpClient.RequestExecutionBuilder requestBuilder() {
        return httpClient.requestExecutionBuilder().request(newGetRequest());
    }

    private ExecutionContext withInterceptors(ExecutionInterceptor... requestHandlers) {
        return ExecutionContext.builder()
                               .signer(new NoOpSigner())
                               .executionAttributes(new ExecutionAttributes())
                               .interceptorContext(InterceptorContext.builder().request(NoopTestRequest.builder().build()).build())
                               .interceptorChain(new ExecutionInterceptorChain(Arrays.asList(requestHandlers)))
                               .build();
    }
}
