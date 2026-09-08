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

package software.amazon.awssdk.protocol.tests;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationResponse;

/**
 * A streaming-output-to-file download whose destination already exists cannot succeed, so it is rejected before a request
 * is sent rather than after the response arrives.
 */
@WireMockTest
public class FileDestinationPreflightTest {

    private static final String BODY = "hello from the service";
    private static final String PRE_EXISTING = "pre-existing contents";

    @TempDir
    Path tempDir;

    private Path destination;
    private CountingInterceptor interceptor;
    private URI endpoint;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wireMock) throws IOException {
        endpoint = URI.create(wireMock.getHttpBaseUrl());
        destination = tempDir.resolve("destination.bin");
        Files.write(destination, PRE_EXISTING.getBytes(StandardCharsets.UTF_8));
        interceptor = new CountingInterceptor();
        stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody(BODY)));
    }

    @Test
    void async_existingDestination_failsWithoutSendingARequest() {
        ProtocolRestJsonAsyncClient client = asyncClient(AwsRetryStrategy.doNotRetry());

        CompletableFuture<StreamingOutputOperationResponse> future = client.streamingOutputOperation(r -> {
        }, AsyncResponseTransformer.toFile(destination));

        assertThat(catchThrowable(future::join)).hasRootCauseInstanceOf(FileAlreadyExistsException.class);
        assertThat(httpRequestCount()).isZero();
        assertThat(contentsOf(destination)).isEqualTo(PRE_EXISTING);
    }

    @Test
    void async_existingDestination_defaultRetries_doesNotRetry() {
        ProtocolRestJsonAsyncClient client = asyncClient(AwsRetryStrategy.standardRetryStrategy());

        CompletableFuture<StreamingOutputOperationResponse> future = client.streamingOutputOperation(r -> {
        }, AsyncResponseTransformer.toFile(destination));

        assertThat(catchThrowable(future::join)).hasRootCauseInstanceOf(FileAlreadyExistsException.class);
        assertThat(httpRequestCount()).isZero();
        assertThat(contentsOf(destination)).isEqualTo(PRE_EXISTING);
    }

    /**
     * Asserted deliberately: no execution begins, so there is nothing for the interceptors to observe or report.
     */
    @Test
    void async_existingDestination_noExecutionInterceptorCallbacks() {
        ProtocolRestJsonAsyncClient client = asyncClient(AwsRetryStrategy.doNotRetry());

        CompletableFuture<StreamingOutputOperationResponse> future = client.streamingOutputOperation(r -> {
        }, AsyncResponseTransformer.toFile(destination));
        catchThrowable(future::join);

        assertThat(interceptor.beforeExecution).hasValue(0);
        assertThat(interceptor.modifyHttpRequest).hasValue(0);
        assertThat(interceptor.onExecutionFailure).hasValue(0);
    }

    @Test
    void sync_existingDestination_failsWithoutSendingARequest() {
        ProtocolRestJsonClient client = syncClient();

        Throwable thrown = catchThrowable(() -> client.streamingOutputOperation(r -> {
        }, ResponseTransformer.toFile(destination)));

        assertThat(thrown).hasRootCauseInstanceOf(FileAlreadyExistsException.class);
        assertThat(httpRequestCount()).isZero();
        assertThat(contentsOf(destination)).isEqualTo(PRE_EXISTING);
    }

    /**
     * {@code onExecutionFailure} is owed once an execution has begun, so the rejection happens before {@code beforeExecution}
     * rather than between it and dispatch.
     */
    @Test
    void sync_existingDestination_noExecutionInterceptorCallbacks() {
        ProtocolRestJsonClient client = syncClient();

        catchThrowable(() -> client.streamingOutputOperation(r -> {
        }, ResponseTransformer.toFile(destination)));

        assertThat(interceptor.beforeExecution).hasValue(0);
        assertThat(interceptor.modifyHttpRequest).hasValue(0);
        assertThat(interceptor.onExecutionFailure).hasValue(0);
    }

    @Test
    void async_freshDestination_stillSucceeds() throws IOException {
        Files.delete(destination);
        ProtocolRestJsonAsyncClient client = asyncClient(AwsRetryStrategy.doNotRetry());

        client.streamingOutputOperation(r -> {
        }, AsyncResponseTransformer.toFile(destination)).join();

        assertThat(contentsOf(destination)).isEqualTo(BODY);
        assertThat(httpRequestCount()).isEqualTo(1);
    }

    @Test
    void sync_freshDestination_stillSucceeds() throws IOException {
        Files.delete(destination);
        ProtocolRestJsonClient client = syncClient();

        client.streamingOutputOperation(r -> {
        }, ResponseTransformer.toFile(destination));

        assertThat(contentsOf(destination)).isEqualTo(BODY);
        assertThat(httpRequestCount()).isEqualTo(1);
    }

    /**
     * A dangling symlink is absent to a link-following check, but an exclusive create on it still fails.
     */
    @Test
    void async_danglingSymlinkDestination_failsWithoutSendingARequest() throws IOException {
        Files.delete(destination);
        Path link = tempDir.resolve("dangling-link.bin");
        try {
            Files.createSymbolicLink(link, tempDir.resolve("no-such-target.bin"));
        } catch (UnsupportedOperationException | IOException e) {
            return; // filesystem does not support symlinks
        }

        ProtocolRestJsonAsyncClient client = asyncClient(AwsRetryStrategy.doNotRetry());
        CompletableFuture<StreamingOutputOperationResponse> future = client.streamingOutputOperation(r -> {
        }, AsyncResponseTransformer.toFile(link));

        assertThat(catchThrowable(future::join)).hasRootCauseInstanceOf(FileAlreadyExistsException.class);
        assertThat(httpRequestCount()).isZero();
    }

    @Test
    void async_existingDirectoryDestination_failsWithoutSendingARequest() throws IOException {
        Path directory = Files.createDirectory(tempDir.resolve("a-directory"));
        ProtocolRestJsonAsyncClient client = asyncClient(AwsRetryStrategy.doNotRetry());

        CompletableFuture<StreamingOutputOperationResponse> future = client.streamingOutputOperation(r -> {
        }, AsyncResponseTransformer.toFile(directory));

        assertThat(catchThrowable(future::join)).hasRootCauseInstanceOf(FileAlreadyExistsException.class);
        assertThat(httpRequestCount()).isZero();
    }

    private ProtocolRestJsonAsyncClient asyncClient(RetryStrategy retryStrategy) {
        return ProtocolRestJsonAsyncClient.builder()
                                          .credentialsProvider(credentials())
                                          .region(Region.US_EAST_1)
                                          .endpointOverride(endpoint)
                                          .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor)
                                                                       .retryStrategy(retryStrategy))
                                          .build();
    }

    private ProtocolRestJsonClient syncClient() {
        return ProtocolRestJsonClient.builder()
                                     .credentialsProvider(credentials())
                                     .region(Region.US_EAST_1)
                                     .endpointOverride(endpoint)
                                     .overrideConfiguration(
                                         o -> o.addExecutionInterceptor(interceptor)
                                               .retryStrategy(AwsRetryStrategy.standardRetryStrategy()))
                                     .build();
    }

    private static StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
    }

    private static int httpRequestCount() {
        return WireMock.findAll(postRequestedFor(urlMatching(".*"))).size();
    }

    private static String contentsOf(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "<unreadable: " + e + ">";
        }
    }

    private static final class CountingInterceptor implements ExecutionInterceptor {
        private final AtomicInteger beforeExecution = new AtomicInteger();
        private final AtomicInteger modifyHttpRequest = new AtomicInteger();
        private final AtomicInteger onExecutionFailure = new AtomicInteger();

        @Override
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
            beforeExecution.incrementAndGet();
        }

        @Override
        public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
            modifyHttpRequest.incrementAndGet();
            return context.httpRequest();
        }

        @Override
        public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
            onExecutionFailure.incrementAndGet();
        }
    }
}
