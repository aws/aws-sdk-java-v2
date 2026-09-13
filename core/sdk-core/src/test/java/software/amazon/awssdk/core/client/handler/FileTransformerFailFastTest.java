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

package software.amazon.awssdk.core.client.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.protocol.VoidSdkResponse;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import utils.HttpTestUtils;
import utils.ValidSdkObjects;

/**
 * Verifies that downloading to a destination that violates the file write option precondition fails client-side, before
 * any request is dispatched, on both the sync and async client handlers.
 */
class FileTransformerFailFastTest {

    @TempDir
    Path tempDir;

    private final SdkRequest request = mock(SdkRequest.class);
    private final Marshaller<SdkRequest> marshaller = mock(Marshaller.class);
    private final HttpResponseHandler<SdkResponse> responseHandler = mock(HttpResponseHandler.class);
    private final HttpResponseHandler<SdkServiceException> errorResponseHandler = mock(HttpResponseHandler.class);

    /**
     * Mirrors the generated {@code getObject(request, path)} overload, which evaluates
     * {@code ResponseTransformer.toFile(path)} inside the call, so the failure surfaces from the call and no request is
     * dispatched.
     */
    @Test
    void syncExecute_toFileDestinationExists_failsFastWithoutDispatchingRequest() throws Exception {
        SdkHttpClient httpClient = mock(SdkHttpClient.class);
        SdkSyncClientHandler handler = new SdkSyncClientHandler(syncClientConfiguration(httpClient));

        Path existingFile = Files.createFile(tempDir.resolve("sync-existing-dest.bin"));

        assertThatThrownBy(() -> handler.execute(clientExecutionParams(), ResponseTransformer.toFile(existingFile)))
            .isInstanceOf(SdkClientException.class)
            .hasRootCauseInstanceOf(FileAlreadyExistsException.class);

        verifyNoInteractions(httpClient);
    }

    /**
     * Guards against over-eager rejection: a destination that does not exist yet must still be requested and written.
     */
    @Test
    void syncExecute_toFileDestinationDoesNotExist_dispatchesRequestAndWritesFile() throws Exception {
        SdkHttpClient httpClient = mock(SdkHttpClient.class);
        ExecutableHttpRequest httpClientCall = mock(ExecutableHttpRequest.class);
        when(httpClient.prepareRequest(any())).thenReturn(httpClientCall);
        when(httpClientCall.call()).thenReturn(
            HttpExecuteResponse.builder()
                               .response(SdkHttpResponse.builder().statusCode(200).build())
                               .responseBody(AbortableInputStream.create(
                                   new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8))))
                               .build());
        when(marshaller.marshall(request)).thenReturn(ValidSdkObjects.sdkHttpFullRequest().build());
        when(responseHandler.handle(any(), any())).thenReturn(VoidSdkResponse.builder().build());

        SdkSyncClientHandler handler = new SdkSyncClientHandler(syncClientConfiguration(httpClient));
        Path freshPath = tempDir.resolve("fresh-dest.bin");

        handler.execute(clientExecutionParams(), ResponseTransformer.toFile(freshPath));

        verify(httpClient).prepareRequest(any());
        assertThat(freshPath).hasContent("hello");
    }

    @Test
    void asyncExecute_toFileDestinationExists_failsFastWithoutDispatchingRequest() throws Exception {
        SdkAsyncHttpClient httpClient = mock(SdkAsyncHttpClient.class);
        SdkAsyncClientHandler handler = new SdkAsyncClientHandler(asyncClientConfiguration(httpClient));

        Path existingFile = Files.createFile(tempDir.resolve("async-existing-dest.bin"));

        CompletableFuture<SdkResponse> responseFuture =
            handler.execute(clientExecutionParams(), AsyncResponseTransformer.toFile(existingFile));

        assertThatThrownBy(() -> responseFuture.get(5, TimeUnit.SECONDS))
            .hasRootCauseInstanceOf(FileAlreadyExistsException.class);
        verify(httpClient, never()).execute(any());
    }

    private ClientExecutionParams<SdkRequest, SdkResponse> clientExecutionParams() {
        when(request.overrideConfiguration()).thenReturn(Optional.empty());
        return new ClientExecutionParams<SdkRequest, SdkResponse>()
            .withInput(request)
            .withMarshaller(marshaller)
            .withResponseHandler(responseHandler)
            .withErrorResponseHandler(errorResponseHandler);
    }

    private SdkClientConfiguration syncClientConfiguration(SdkHttpClient httpClient) {
        return HttpTestUtils.testClientConfiguration().toBuilder()
                            .option(SdkClientOption.SYNC_HTTP_CLIENT, httpClient)
                            .option(SdkClientOption.RETRY_STRATEGY, DefaultRetryStrategy.doNotRetry())
                            .build();
    }

    private SdkClientConfiguration asyncClientConfiguration(SdkAsyncHttpClient httpClient) {
        ScheduledExecutorService scheduledExecutor = mock(ScheduledExecutorService.class);
        return HttpTestUtils.testClientConfiguration().toBuilder()
                            .option(SdkClientOption.ASYNC_HTTP_CLIENT, httpClient)
                            .option(SdkClientOption.RETRY_POLICY, RetryPolicy.none())
                            .option(SdkClientOption.RETRY_STRATEGY, DefaultRetryStrategy.doNotRetry())
                            .option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE, scheduledExecutor)
                            .build();
    }
}
