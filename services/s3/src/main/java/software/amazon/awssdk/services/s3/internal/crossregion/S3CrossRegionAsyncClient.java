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

package software.amazon.awssdk.services.s3.internal.crossregion;

import static software.amazon.awssdk.services.s3.internal.crossregion.utils.CrossRegionUtils.getBucketRegionFromException;
import static software.amazon.awssdk.services.s3.internal.crossregion.utils.CrossRegionUtils.isS3RedirectException;
import static software.amazon.awssdk.services.s3.internal.crossregion.utils.CrossRegionUtils.requestWithDecoratedEndpointProvider;
import static software.amazon.awssdk.services.s3.internal.crossregion.utils.CrossRegionUtils.updateUserAgentInConfig;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.DelegatingS3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Request;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@SdkInternalApi
public final class S3CrossRegionAsyncClient extends DelegatingS3AsyncClient {

    private final Map<String, Region> bucketToRegionCache = new ConcurrentHashMap<>();

    public S3CrossRegionAsyncClient(S3AsyncClient s3Client) {
        super(s3Client);
    }

    @Override
    protected <T extends S3Request, ReturnT> CompletableFuture<ReturnT> invokeOperation(
        T request, Function<T, CompletableFuture<ReturnT>> operation) {

        Optional<String> bucket = request.getValueForField("Bucket", String.class);

        AwsRequestOverrideConfiguration overrideConfiguration = updateUserAgentInConfig(request);
        T userAgentUpdatedRequest = (T) request.toBuilder().overrideConfiguration(overrideConfiguration).build();

        if (!bucket.isPresent()) {
            return operation.apply(userAgentUpdatedRequest);
        }
        String bucketName = bucket.get();

        CompletableFuture<ReturnT> returnFuture = new CompletableFuture<>();
        CompletableFuture<ReturnT> apiOperationFuture = bucketToRegionCache.containsKey(bucketName) ?
                                                        operation.apply(
                                                            requestWithDecoratedEndpointProvider(
                                                                userAgentUpdatedRequest,
                                                                () -> bucketToRegionCache.get(bucketName),
                                                                serviceClientConfiguration().endpointProvider().get()
                                                            )
                                                        ) :
                                                        operation.apply(userAgentUpdatedRequest);

        apiOperationFuture.whenComplete(redirectToCrossRegionIfRedirectException(operation,
                                                                                 userAgentUpdatedRequest,
                                                                                 bucketName,
                                                                                 returnFuture));
        return returnFuture;
    }

    private <T extends S3Request, ReturnT> BiConsumer<ReturnT, Throwable> redirectToCrossRegionIfRedirectException(
        Function<T, CompletableFuture<ReturnT>> operation,
        T userAgentUpdatedRequest, String bucketName,
        CompletableFuture<ReturnT> returnFuture) {

        return (response, throwable) -> {
            if (throwable != null) {
                if (isS3RedirectException(throwable)) {
                    bucketToRegionCache.remove(bucketName);
                    requestWithCrossRegion(userAgentUpdatedRequest, operation, bucketName, returnFuture, throwable);
                } else {
                    returnFuture.completeExceptionally(throwable);
                }
            } else {
                returnFuture.complete(response);
            }
        };
    }

    private <T extends S3Request, ReturnT> void requestWithCrossRegion(T request,
                                                                       Function<T, CompletableFuture<ReturnT>> operation,
                                                                       String bucketName,
                                                                       CompletableFuture<ReturnT> returnFuture,
                                                                       Throwable throwable) {

        Optional<String> bucketRegionFromException = getBucketRegionFromException((S3Exception) throwable.getCause());
        if (bucketRegionFromException.isPresent()) {
            sendRequestWithRightRegion(request, operation, bucketName, returnFuture, bucketRegionFromException.get());
        } else {
            fetchRegionAndSendRequest(request, operation, bucketName, returnFuture);
        }
    }

    private <T extends S3Request, ReturnT> void fetchRegionAndSendRequest(T request,
                                                                          Function<T, CompletableFuture<ReturnT>> operation,
                                                                          String bucketName,
                                                                          CompletableFuture<ReturnT> returnFuture) {
        // // TODO: Need to change codegen of Delegating Client to avoid the cast, have taken a backlog item to fix this.
        ((S3AsyncClient) delegate()).headBucket(b -> b.bucket(bucketName)).whenComplete((response,
                                                                                         throwable) -> {
            if (throwable != null) {
                if (isS3RedirectException(throwable)) {
                    bucketToRegionCache.remove(bucketName);
                    Optional<String> bucketRegion = getBucketRegionFromException((S3Exception) throwable.getCause());
                    if (bucketRegion.isPresent()) {
                        sendRequestWithRightRegion(request, operation, bucketName, returnFuture, bucketRegion.get());
                    } else {
                        returnFuture.completeExceptionally(throwable);
                    }
                } else {
                    returnFuture.completeExceptionally(throwable);
                }
            }
        });
    }

    private <T extends S3Request, ReturnT> void sendRequestWithRightRegion(T request,
                                                                           Function<T, CompletableFuture<ReturnT>> operation,
                                                                           String bucketName,
                                                                           CompletableFuture<ReturnT> returnFuture,
                                                                           String region) {
        bucketToRegionCache.put(bucketName, Region.of(region));
        CompletableFuture<ReturnT> newFuture = operation.apply(
            requestWithDecoratedEndpointProvider(request,
                                                 () -> Region.of(region),
                                                 serviceClientConfiguration().endpointProvider().get()));
        CompletableFutureUtils.forwardResultTo(newFuture, returnFuture);
        CompletableFutureUtils.forwardExceptionTo(returnFuture, newFuture);
    }
}