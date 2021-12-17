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

package software.amazon.awssdk.core.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.fail;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.EmptyPublisher;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SdkAsyncClientHandler;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.protocol.VoidSdkResponse;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import utils.HttpTestUtils;
import utils.ValidSdkObjects;

/**
 * Tests to verify that when exceptions are thrown during various stages of
 * execution within the handler, they are reported through the returned {@link
 * java.util.concurrent.CompletableFuture}.
 *
 * @see AsyncClientHandlerInterceptorExceptionTest
 */
public class AsyncClientHandlerExceptionTest {
    private final SdkRequest request = mock(SdkRequest.class);

    private final SdkAsyncHttpClient asyncHttpClient = mock(SdkAsyncHttpClient.class);

    private final Marshaller<SdkRequest> marshaller = mock(Marshaller.class);

    private final HttpResponseHandler<SdkResponse> responseHandler = mock(HttpResponseHandler.class);

    private final HttpResponseHandler<SdkServiceException> errorResponseHandler = mock(HttpResponseHandler.class);

    private SdkAsyncClientHandler clientHandler;

    private ClientExecutionParams<SdkRequest, SdkResponse> executionParams;

    @Before
    public void methodSetup() throws Exception {
        executionParams = new ClientExecutionParams<SdkRequest, SdkResponse>()
                .withInput(request)
                .withMarshaller(marshaller)
                .withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler);

        SdkClientConfiguration config = HttpTestUtils.testClientConfiguration().toBuilder()
                .option(SdkClientOption.ASYNC_HTTP_CLIENT, asyncHttpClient)
                .option(SdkClientOption.RETRY_POLICY, RetryPolicy.none())
                .option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, Runnable::run)
                .build();

        clientHandler = new SdkAsyncClientHandler(config);

        when(request.overrideConfiguration()).thenReturn(Optional.empty());

        when(marshaller.marshall(eq(request))).thenReturn(ValidSdkObjects.sdkHttpFullRequest().build());

        when(responseHandler.handle(any(SdkHttpFullResponse.class), any(ExecutionAttributes.class)))
                .thenReturn(VoidSdkResponse.builder().build());

        when(asyncHttpClient.execute(any(AsyncExecuteRequest.class))).thenAnswer((Answer<CompletableFuture<Void>>) invocationOnMock -> {
            SdkAsyncHttpResponseHandler handler = invocationOnMock.getArgumentAt(0, AsyncExecuteRequest.class).responseHandler();
            handler.onHeaders(SdkHttpFullResponse.builder()
                    .statusCode(200)
                    .build());
            handler.onStream(new EmptyPublisher<>());
            return CompletableFuture.completedFuture(null);
        });
    }

    @Test
    public void marshallerThrowsReportedThroughFuture() throws ExecutionException, InterruptedException {
        final SdkClientException e = SdkClientException.create("Could not marshall");
        when(marshaller.marshall(any(SdkRequest.class))).thenThrow(e);
        doVerify(() -> clientHandler.execute(executionParams), e);
    }

    @Test
    public void responseHandlerThrowsReportedThroughFuture() throws Exception {
        final SdkClientException e = SdkClientException.create("Could not handle response");
        when(responseHandler.handle(any(SdkHttpFullResponse.class), any(ExecutionAttributes.class))).thenThrow(e);
        doVerify(() -> clientHandler.execute(executionParams), e);
    }

    @Test
    public void streamingRequest_marshallingException_shouldInvokeExceptionOccurred() throws Exception {
        AsyncResponseTransformer asyncResponseTransformer = mock(AsyncResponseTransformer.class);
        CompletableFuture<?> future = new CompletableFuture<>();
        when(asyncResponseTransformer.prepare()).thenReturn(future);

        SdkClientException exception = SdkClientException.create("Could not handle response");
        when(marshaller.marshall(any(SdkRequest.class))).thenThrow(exception);

        doVerify(() -> clientHandler.execute(executionParams, asyncResponseTransformer), exception);

        verify(asyncResponseTransformer, times(1)).prepare();
        verify(asyncResponseTransformer, times(1)).exceptionOccurred(exception);
    }

    private void doVerify(Supplier<CompletableFuture<?>> s, final Throwable expectedException) {
        doVerify(s, (thrown) -> thrown.getCause() == expectedException);
    }

    private void doVerify(Supplier<CompletableFuture<?>> s, Predicate<Throwable> assertFn) {
        CompletableFuture<?> cf = s.get();
        try {
            cf.get();
            fail("get() method did not fail as expected.");
        } catch (Throwable t) {
            assertTrue(assertFn.test(t));
        }
    }
}
