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

package software.amazon.awssdk.services.s3.internal;


import static software.amazon.awssdk.services.s3.internal.S3CrtUtils.createCrtCredentialsProvider;

import com.amazonaws.s3.RequestDataSupplier;
import com.amazonaws.s3.S3NativeClient;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider;
import software.amazon.awssdk.services.s3.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import com.amazonaws.s3.model.PutObjectOutput;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.internal.s3crt.RequestDataSupplierAdapter;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@SdkInternalApi
public final class DefaultS3CrtAsyncClient implements S3CrtAsyncClient {
    private final S3NativeClient s3NativeClient;
    private final S3NativeClientConfiguration configuration;

    public DefaultS3CrtAsyncClient(DefaultS3CrtClientBuilder builder) {
        CredentialsProvider credentialsProvider =  builder.credentialsProvider() == null ? null :
                                                   createCrtCredentialsProvider(builder.credentialsProvider());

        this.configuration = S3NativeClientConfiguration.builder()
                                                        .credentialsProvider(credentialsProvider)
                                                        .signingRegion(builder.region() == null ? null : builder.region().id())
                                                        .partSizeBytes(builder.partSizeBytes())
                                                        .maxThroughputGbps(builder.maxThroughputGbps())
                                                        .build();

        this.s3NativeClient = new S3NativeClient(configuration.signingRegion(),
                                                 configuration.clientBootstrap(),
                                                 configuration.credentialsProvider(),
                                                 configuration.partSizeBytes(),
                                                 configuration.maxThroughputGbps());
    }

    @Override
    public <ReturnT> CompletableFuture<ReturnT> getObject(
        GetObjectRequest getObjectRequest, AsyncResponseTransformer<GetObjectResponse, ReturnT> asyncResponseTransformer) {

        CompletableFuture<ReturnT> future = new CompletableFuture<>();
        com.amazonaws.s3.model.GetObjectRequest crtGetObjectRequest = S3CrtUtils.adaptGetObjectRequest(getObjectRequest);
        CrtResponseDataConsumerAdapter<ReturnT> adapter = new CrtResponseDataConsumerAdapter<>(asyncResponseTransformer);

        CompletableFuture<ReturnT> adapterFuture = adapter.transformerFuture();

        s3NativeClient.getObject(crtGetObjectRequest, adapter);

        adapterFuture.whenComplete((r, t) -> {
            if (t == null) {
                future.complete(r);
            } else {
                future.completeExceptionally(t);
            }
            // TODO: Offload to future completion thread
        });

        return future;
    }

    public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest putObjectRequest, AsyncRequestBody requestBody) {
        com.amazonaws.s3.model.PutObjectRequest adaptedRequest = S3CrtUtils.toCrtPutObjectRequest(putObjectRequest);

        if (adaptedRequest.contentLength() == null && requestBody.contentLength().isPresent()) {
            adaptedRequest = adaptedRequest.toBuilder().contentLength(requestBody.contentLength().get())
                    .build();
        }

        CompletableFuture<PutObjectOutput> putObjectOutputCompletableFuture = s3NativeClient.putObject(adaptedRequest,
                adaptToDataSupplier(requestBody));

        return putObjectOutputCompletableFuture.thenApply(S3CrtUtils::fromCrtPutObjectOutput);
    }

    @Override
    public String serviceName() {
        return "s3";
    }

    @Override
    public void close() {
        s3NativeClient.close();
        configuration.close();
    }

    private static RequestDataSupplier adaptToDataSupplier(AsyncRequestBody requestBody) {
        return new RequestDataSupplierAdapter(requestBody);
    }
}
