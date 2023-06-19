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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
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

        if (!bucket.isPresent()) {
            return operation.apply(request);
        }
        String bucketName = bucket.get();

        if (bucketToRegionCache.containsKey(bucketName)) {
            return operation.apply(requestWithDecoratedEndpointProvider(request,
                                                                        () -> bucketToRegionCache.get(bucketName),
                                                                        serviceClientConfiguration().endpointProvider().get()));
        }

        CompletableFuture<ReturnT> returnFuture = new CompletableFuture<>();
        operation.apply(request)
                 .whenComplete((r, t) -> {
                     if (t != null) {
                         if (isS3RedirectException(t)) {
                             bucketToRegionCache.remove(bucketName);
                             requestWithCrossRegion(request, operation, bucketName, returnFuture, t);
                             return;
                         }
                         returnFuture.completeExceptionally(t);
                         return;
                     }
                     returnFuture.complete(r);
                 });
        return returnFuture;
    }

    private <T extends S3Request, ReturnT> void requestWithCrossRegion(T request,
                                                                       Function<T, CompletableFuture<ReturnT>> operation,
                                                                       String bucketName,
                                                                       CompletableFuture<ReturnT> returnFuture,
                                                                       Throwable t) {

        Optional<String> bucketRegionFromException = getBucketRegionFromException((S3Exception) t.getCause());
        if (bucketRegionFromException.isPresent()) {
            sendRequestWithRightRegion(request, operation, bucketName, returnFuture,
                                       bucketRegionFromException);
        } else {
            fetchRegionAndSendRequest(request, operation, bucketName, returnFuture);
        }
    }

    private <T extends S3Request, ReturnT> void fetchRegionAndSendRequest(T request,
                                                                          Function<T, CompletableFuture<ReturnT>> operation,
                                                                          String bucketName,
                                                                          CompletableFuture<ReturnT> returnFuture) {

        // // TODO: will fix the casts with separate PR
        ((S3AsyncClient) delegate()).headBucket(b -> b.bucket(bucketName)).whenComplete((response,
                                                                                         throwable) -> {
            if (throwable != null) {
                if (isS3RedirectException(throwable)) {
                    bucketToRegionCache.remove(bucketName);
                    Optional<String> bucketRegion = getBucketRegionFromException((S3Exception) throwable.getCause());

                    if (bucketRegion.isPresent()) {
                        bucketToRegionCache.put(bucketName, Region.of(bucketRegion.get()));
                        sendRequestWithRightRegion(request, operation, bucketName, returnFuture, bucketRegion);
                    } else {
                        returnFuture.completeExceptionally(throwable);
                    }
                } else {
                    returnFuture.completeExceptionally(throwable);
                }
            } else {
                CompletableFuture<ReturnT> newFuture = operation.apply(request);
                CompletableFutureUtils.forwardResultTo(newFuture, returnFuture);
                CompletableFutureUtils.forwardExceptionTo(returnFuture, newFuture);
            }
        });
    }

    private <T extends S3Request, ReturnT> void sendRequestWithRightRegion(T request,
                                                                           Function<T, CompletableFuture<ReturnT>> operation,
                                                                           String bucketName,
                                                                           CompletableFuture<ReturnT> returnFuture,
                                                                           Optional<String> bucketRegionFromException) {
        String region = bucketRegionFromException.get();
        bucketToRegionCache.put(bucketName, Region.of(region));
        doSendRequestWithRightRegion(request, operation, returnFuture, region);
    }

    private <T extends S3Request, ReturnT> void doSendRequestWithRightRegion(T request,
                                                                             Function<T, CompletableFuture<ReturnT>> operation,
                                                                             CompletableFuture<ReturnT> returnFuture,
                                                                             String region) {
        CompletableFuture<ReturnT> newFuture = operation.apply(
            requestWithDecoratedEndpointProvider(request,
                                                 () -> Region.of(region),
                                                 serviceClientConfiguration().endpointProvider().get()));
        CompletableFutureUtils.forwardResultTo(newFuture, returnFuture);
        // forward exception
        CompletableFutureUtils.forwardExceptionTo(returnFuture, newFuture);
    }
}