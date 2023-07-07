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

package software.amazon.awssdk.services.s3.internal.crossregion.endpointprovider;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;

/**
 * Decorator S3EndpointProvider which updates the region with the one that is supplied during its instantiation.
 */
@SdkInternalApi
public class BucketEndpointProvider implements S3EndpointProvider {
    private final S3EndpointProvider delegateEndPointProvider;
    private final Supplier<Region> regionSupplier;

    private BucketEndpointProvider(S3EndpointProvider delegateEndPointProvider, Supplier<Region> regionSupplier) {
        this.delegateEndPointProvider = delegateEndPointProvider;
        this.regionSupplier = regionSupplier;
    }

    public static BucketEndpointProvider create(S3EndpointProvider delegateEndPointProvider, Supplier<Region> regionSupplier) {
        return new BucketEndpointProvider(delegateEndPointProvider, regionSupplier);
    }

    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(S3EndpointParams endpointParams) {
        Region crossRegion = regionSupplier.get();
        return delegateEndPointProvider.resolveEndpoint(
            crossRegion != null ? endpointParams.copy(c -> c.region(crossRegion)) : endpointParams);
    }
}

