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

package software.amazon.awssdk.services.s3.internal.endpoints;

import static software.amazon.awssdk.services.s3.internal.endpoints.S3EndpointUtils.accelerateEndpoint;
import static software.amazon.awssdk.services.s3.internal.endpoints.S3EndpointUtils.dualstackEndpoint;
import static software.amazon.awssdk.services.s3.internal.endpoints.S3EndpointUtils.isAccelerateEnabled;
import static software.amazon.awssdk.services.s3.internal.endpoints.S3EndpointUtils.isAccelerateSupported;
import static software.amazon.awssdk.services.s3.internal.endpoints.S3EndpointUtils.isDualstackEnabled;
import static software.amazon.awssdk.services.s3.internal.endpoints.S3EndpointUtils.isPathStyleAccessEnabled;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.RegionMetadata;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.BucketUtils;
import software.amazon.awssdk.services.s3.internal.ConfiguredS3SdkHttpRequest;

/**
 * Returns a new configured HTTP request with a resolved endpoint with either virtual addressing or path style access.
 * Supports accelerate and dual stack.
 */
@SdkInternalApi
public final class S3BucketEndpointResolver implements S3EndpointResolver {

    private S3BucketEndpointResolver() {
    }

    public static S3BucketEndpointResolver create() {
        return new S3BucketEndpointResolver();
    }

    @Override
    public ConfiguredS3SdkHttpRequest applyEndpointConfiguration(S3EndpointResolverContext context) {
        URI endpoint = resolveEndpoint(context);
        SdkHttpRequest.Builder mutableRequest = context.request().toBuilder();
        mutableRequest.uri(endpoint);

        String bucketName = context.originalRequest().getValueForField("Bucket", String.class).orElse(null);
        if (canUseVirtualAddressing(context.serviceConfiguration(), bucketName)) {
            changeToDnsEndpoint(mutableRequest, bucketName);
        }

        return ConfiguredS3SdkHttpRequest.builder()
                                         .sdkHttpRequest(mutableRequest.build())
                                         .build();
    }

    /**
     * Determine which endpoint to use based on region and {@link S3Configuration}. Will either be a traditional
     * S3 endpoint (i.e. s3.us-east-1.amazonaws.com), the global S3 accelerate endpoint (i.e. s3-accelerate.amazonaws.com) or
     * a regional dualstack endpoint for IPV6 (i.e. s3.dualstack.us-east-1.amazonaws.com).
     */
    private static URI resolveEndpoint(S3EndpointResolverContext context) {
        SdkHttpRequest request = context.request();
        String protocol = request.protocol();
        RegionMetadata regionMetadata = RegionMetadata.of(context.region());
        S3Configuration serviceConfiguration = context.serviceConfiguration();

        if (isAccelerateEnabled(serviceConfiguration) && isAccelerateSupported(context.originalRequest())) {
            return accelerateEndpoint(serviceConfiguration, regionMetadata.domain(), protocol);
        }

        if (isDualstackEnabled(serviceConfiguration)) {
            return dualstackEndpoint(regionMetadata.id(), regionMetadata.domain(), protocol);
        }

        return invokeSafely(() -> new URI(protocol, null, request.host(), request.port(), null, null, null));
    }

    private static boolean canUseVirtualAddressing(S3Configuration serviceConfiguration, String bucketName) {
        return !isPathStyleAccessEnabled(serviceConfiguration) && bucketName != null &&
               BucketUtils.isVirtualAddressingCompatibleBucketName(bucketName, false);
    }

    /**
     * Changes from path style addressing (which the marshallers produce by default), to DNS style/virtual style addressing,
     * where the bucket name is prepended to the host. DNS style addressing is preferred due to the better load balancing
     * qualities it provides; path style is an option mainly for proxy based situations and alternative S3 implementations.
     *
     * @param mutableRequest Marshalled HTTP request we are modifying.
     * @param bucketName     Bucket name for this particular operation.
     */
    private static void changeToDnsEndpoint(SdkHttpRequest.Builder mutableRequest, String bucketName) {
        if (mutableRequest.host().startsWith("s3")) {
            String newHost = mutableRequest.host().replaceFirst("s3", bucketName + "." + "s3");
            String newPath = mutableRequest.encodedPath().replaceFirst("/" + bucketName, "");
            mutableRequest.host(newHost).encodedPath(newPath);
        }
    }
}
