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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.DelegatingS3Client;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.model.S3Request;

@SdkInternalApi
public final class S3CrossRegionSyncClient extends DelegatingS3Client {
    public S3CrossRegionSyncClient(S3Client s3Client) {
        super(s3Client);
    }

    @Override
    protected <T extends S3Request, ReturnT> ReturnT invokeOperation(T request, Function<T, ReturnT> operation) {

        Optional<String> bucket = request.getValueForField("Bucket", String.class);

        if (bucket.isPresent()) {
            try {
                return operation.apply(requestWithDecoratedEndpointProvider(request, bucket.get()));
            } catch (Exception e) {
                handleOperationFailure(e, bucket.get());
            }
        }

        return operation.apply(request);
    }

    private void handleOperationFailure(Throwable t, String bucket) {
        //TODO: handle failure case
    }

    @SuppressWarnings("unchecked")
    private <T extends S3Request> T requestWithDecoratedEndpointProvider(T request, String bucket) {
        return (T) request.toBuilder()
                          .overrideConfiguration(getOrCreateConfigWithEndpointProvider(request, bucket))
                          .build();
    }

    //TODO: optimize shared sync/async code
    private AwsRequestOverrideConfiguration getOrCreateConfigWithEndpointProvider(S3Request request,
                                                                                  String bucket) {

        AwsRequestOverrideConfiguration requestOverrideConfiguration =
            request.overrideConfiguration().orElseGet(() -> AwsRequestOverrideConfiguration.builder().build());

        EndpointProvider delegateEndpointProvider =
            requestOverrideConfiguration.endpointProvider().orElseGet(() -> serviceClientConfiguration().endpointProvider().get());

        return requestOverrideConfiguration.toBuilder()
                                           .endpointProvider(BucketEndpointProvider.create((S3EndpointProvider) delegateEndpointProvider, bucket))
                                           .build();
    }

    static final class BucketEndpointProvider implements S3EndpointProvider {
        private final S3EndpointProvider delegate;
        private final String bucket;

        private BucketEndpointProvider(S3EndpointProvider delegate, String bucket) {
            this.delegate = delegate;
            this.bucket = bucket;
        }

        public static BucketEndpointProvider create(S3EndpointProvider delegate, String bucket) {
            return new BucketEndpointProvider(delegate, bucket);
        }

        @Override
        public CompletableFuture<Endpoint> resolveEndpoint(S3EndpointParams endpointParams) {
            return delegate.resolveEndpoint(endpointParams);
        }
    }
}
