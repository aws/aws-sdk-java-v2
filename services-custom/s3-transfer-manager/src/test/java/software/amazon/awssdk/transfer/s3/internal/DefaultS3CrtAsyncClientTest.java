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

package software.amazon.awssdk.transfer.s3.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.s3.RequestDataSupplier;
import com.amazonaws.s3.ResponseDataConsumer;
import com.amazonaws.s3.S3NativeClient;
import com.amazonaws.s3.model.GetObjectOutput;
import com.amazonaws.s3.model.GetObjectRequest;
import com.amazonaws.s3.model.PutObjectOutput;
import com.amazonaws.s3.model.PutObjectRequest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@RunWith(MockitoJUnitRunner.class)
public class DefaultS3CrtAsyncClientTest {
    @Mock
    private S3NativeClient mockS3NativeClient;

    @Mock
    private S3NativeClientConfiguration mockConfiguration;

    private S3CrtAsyncClient s3CrtAsyncClient;

    private static ExecutorService executor;

    @BeforeClass
    public static void setUp() {
        executor = Executors.newSingleThreadExecutor();
    }

    @Before
    public void methodSetup() {
        s3CrtAsyncClient = new DefaultS3CrtAsyncClient(mockConfiguration,
                                                       mockS3NativeClient);
        when(mockConfiguration.futureCompletionExecutor()).thenReturn(executor);
    }

    @AfterClass
    public static void cleanUp() {
        executor.shutdown();
    }

    @Test
    public void getObject_cancels_shouldForwardCancellation() {
        CompletableFuture<GetObjectOutput> crtFuture = new CompletableFuture<>();
        when(mockS3NativeClient.getObject(any(GetObjectRequest.class),
                                          any(ResponseDataConsumer.class)))
            .thenReturn(crtFuture);

        CompletableFuture<ResponseBytes<GetObjectResponse>> future =
            s3CrtAsyncClient.getObject(b -> b.bucket("bucket").key("key"),
            AsyncResponseTransformer.toBytes());

        future.cancel(true);
        assertThat(crtFuture).isCancelled();
    }

    @Test
    public void putObject_cancels_shouldForwardCancellation() {
        CompletableFuture<PutObjectOutput> crtFuture = new CompletableFuture<>();
        when(mockS3NativeClient.putObject(any(PutObjectRequest.class),
                                          any(RequestDataSupplier.class)))
            .thenReturn(crtFuture);

        CompletableFuture<PutObjectResponse> future =
            s3CrtAsyncClient.putObject(b -> b.bucket("bucket").key("key"),
                                       AsyncRequestBody.empty());

        future.cancel(true);
        assertThat(crtFuture).isCancelled();
    }

    @Test
    public void putObject_crtFutureCompletedExceptionally_shouldFail() {
        RuntimeException runtimeException = new RuntimeException("test");
        CompletableFuture<PutObjectOutput> crtFuture = new CompletableFuture<>();
        crtFuture.completeExceptionally(runtimeException);
        when(mockS3NativeClient.putObject(any(PutObjectRequest.class),
                                          any(RequestDataSupplier.class)))
            .thenReturn(crtFuture);

        CompletableFuture<PutObjectResponse> future =
            s3CrtAsyncClient.putObject(b -> b.bucket("bucket").key("key"),
                                       AsyncRequestBody.empty());

        assertThatThrownBy(() -> future.join()).hasCause(runtimeException);
    }

    @Test
    public void getObject_crtFutureCompletedExceptionally_shouldFail() {
        RuntimeException runtimeException = new RuntimeException("test");
        CompletableFuture<GetObjectOutput> crtFuture = new CompletableFuture<>();
        crtFuture.completeExceptionally(runtimeException);
        when(mockS3NativeClient.getObject(any(GetObjectRequest.class),
                                          any(ResponseDataConsumer.class)))
            .thenReturn(crtFuture);

        CompletableFuture<ResponseBytes<GetObjectResponse>> future =
            s3CrtAsyncClient.getObject(b -> b.bucket("bucket").key("key"),
                                       AsyncResponseTransformer.toBytes());

        assertThatThrownBy(() -> future.join()).hasCause(runtimeException);
    }

    @Test
    public void putObject_crtFutureCompletedSuccessfully_shouldSucceed() {
        CompletableFuture<PutObjectOutput> crtFuture = new CompletableFuture<>();
        crtFuture.complete(PutObjectOutput.builder().build());
        when(mockS3NativeClient.putObject(any(PutObjectRequest.class),
                                          any(RequestDataSupplier.class)))
            .thenReturn(crtFuture);

        CompletableFuture<PutObjectResponse> future =
            s3CrtAsyncClient.putObject(b -> b.bucket("bucket").key("key"),
                                       AsyncRequestBody.empty());

        assertThat(future.join().sdkHttpResponse().statusText()).isEmpty();
    }

    @Test
    public void closeS3Client_shouldCloseUnderlyingResources() {
        s3CrtAsyncClient.close();
        verify(mockS3NativeClient).close();
        verify(mockConfiguration).close();
    }
}
