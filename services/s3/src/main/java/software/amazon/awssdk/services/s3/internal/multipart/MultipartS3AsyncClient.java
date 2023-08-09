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

package software.amazon.awssdk.services.s3.internal.multipart;


import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.DelegatingS3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.UserAgentUtils;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Request;
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration;
import software.amazon.awssdk.utils.Validate;

/**
 * An {@link S3AsyncClient} that automatically converts put, copy requests to their respective multipart call. Note: get is not
 * yet supported.
 *
 * @see MultipartConfiguration
 */
@SdkInternalApi
public final class MultipartS3AsyncClient extends DelegatingS3AsyncClient {

    private static final ApiName USER_AGENT_API_NAME = ApiName.builder().name("hll").version("s3Multipart").build();

    private static final long DEFAULT_MIN_PART_SIZE = 8L * 1024 * 1024;
    private static final long DEFAULT_THRESHOLD = 8L * 1024 * 1024;
    private static final long DEFAULT_API_CALL_BUFFER_SIZE = DEFAULT_MIN_PART_SIZE * 4;

    private final UploadObjectHelper mpuHelper;
    private final CopyObjectHelper copyObjectHelper;

    private MultipartS3AsyncClient(S3AsyncClient delegate, MultipartConfiguration multipartConfiguration) {
        super(delegate);
        MultipartConfiguration validConfiguration = Validate.getOrDefault(multipartConfiguration,
                                                                          MultipartConfiguration.builder()::build);
        long minPartSizeInBytes = Validate.getOrDefault(validConfiguration.minimumPartSizeInBytes(),
                                                        () -> DEFAULT_MIN_PART_SIZE);
        long threshold = Validate.getOrDefault(validConfiguration.thresholdInBytes(),
                                               () -> DEFAULT_THRESHOLD);
        long apiCallBufferSizeInBytes = Validate.getOrDefault(validConfiguration.apiCallBufferSizeInBytes(),
                                                              () -> computeApiCallBufferSize(validConfiguration));
        mpuHelper = new UploadObjectHelper(delegate, minPartSizeInBytes, threshold, apiCallBufferSizeInBytes);
        copyObjectHelper = new CopyObjectHelper(delegate, minPartSizeInBytes, threshold);
    }

    private long computeApiCallBufferSize(MultipartConfiguration multipartConfiguration) {
        return multipartConfiguration.minimumPartSizeInBytes() != null ? multipartConfiguration.minimumPartSizeInBytes() * 4
                                                                       : DEFAULT_API_CALL_BUFFER_SIZE;
    }

    @Override
    public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest putObjectRequest, AsyncRequestBody requestBody) {
        return mpuHelper.uploadObject(putObjectRequest, requestBody);
    }

    @Override
    public CompletableFuture<CopyObjectResponse> copyObject(CopyObjectRequest copyObjectRequest) {
        return copyObjectHelper.copyObject(copyObjectRequest);
    }

    @Override
    public <ReturnT> CompletableFuture<ReturnT> getObject(
        GetObjectRequest getObjectRequest, AsyncResponseTransformer<GetObjectResponse, ReturnT> asyncResponseTransformer) {
        throw new UnsupportedOperationException(
            "Multipart download is not yet supported. Instead use the CRT based S3 client for multipart download.");
    }

    @Override
    public void close() {
        delegate().close();
    }

    public static MultipartS3AsyncClient create(S3AsyncClient client, MultipartConfiguration multipartConfiguration) {
        S3AsyncClient clientWithUserAgent = new DelegatingS3AsyncClient(client) {
            @Override
            protected <T extends S3Request, ReturnT> CompletableFuture<ReturnT> invokeOperation(T request, Function<T,
                CompletableFuture<ReturnT>> operation) {
                T requestWithUserAgent = UserAgentUtils.applyUserAgentInfo(request, c -> c.addApiName(USER_AGENT_API_NAME));
                return operation.apply(requestWithUserAgent);
            }
        };
        return new MultipartS3AsyncClient(clientWithUserAgent, multipartConfiguration);
    }
}
