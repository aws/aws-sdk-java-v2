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
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.DelegatingS3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Request;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@SdkInternalApi
public final class S3CrossRegionAsyncClient extends DelegatingS3AsyncClient {

    private final Map<String, CompletableFuture<Region>> bucketToRegionCache = new ConcurrentHashMap<>();

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
                                                                        regionSupplier(bucketName),
                                                                        serviceClientConfiguration().endpointProvider().get()));
        }
        return operation.apply(request).thenApply(CompletableFuture::completedFuture)
                        .exceptionally(exception -> {
                            if (isS3RedirectException(exception.getCause())) {
                                bucketToRegionCache.remove(bucketName);
                                getBucketRegionFromException((S3Exception) exception.getCause())
                                    .ifPresent(
                                        region -> bucketToRegionCache.put(bucketName,
                                                                          CompletableFuture.completedFuture(Region.of(region))));
                                return operation.apply(
                                    requestWithDecoratedEndpointProvider(request,
                                                                         regionSupplier(bucketName),
                                                                         serviceClientConfiguration().endpointProvider().get()));
                            }
                            return CompletableFutureUtils.failedFuture(exception);
                        }).thenCompose(Function.identity());
    }


    private Supplier<Region> regionSupplier(String bucket) {
        CompletableFuture<Region> completableFuture = bucketToRegionCache.computeIfAbsent(bucket, this::regionCompletableFuture);
        return () -> completableFuture.join();
    }

    private CompletableFuture<Region> regionCompletableFuture(String bucketName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ((S3AsyncClient) delegate()).headBucket(HeadBucketRequest.builder().bucket(bucketName).build()).join();
            } catch (Exception exception) {
                if (isS3RedirectException(exception.getCause())) {
                    String region = getBucketRegionFromException((S3Exception) exception.getCause())
                        .orElseThrow(() -> AwsServiceException.create("Region name not found in Redirect error",
                                                                      exception));
                    return Region.of(region);
                }
                throw exception;
            }
            return ((S3AsyncClient) delegate()).serviceClientConfiguration().region();
        });
    }
}
