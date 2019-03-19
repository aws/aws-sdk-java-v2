/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionMetadata;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;

/**
 * Utilities for working with Amazon S3 bucket names, such as validation and
 * checked to see if they are compatible with DNS addressing.
 */
@SdkInternalApi
public final class S3EndpointUtils {

    public static final List<Class<?>> ACCELERATE_DISABLED_OPERATIONS = Arrays.asList(
        ListBucketsRequest.class, CreateBucketRequest.class, DeleteBucketRequest.class);

    private S3EndpointUtils() {
    }

    /**
     * Returns a new instance of the given {@link SdkHttpRequest} by applying any endpoint changes based on
     * the given {@link S3Configuration} options.
     */
    public static SdkHttpRequest applyEndpointConfiguration(SdkHttpRequest request,
                                                            Object originalRequest,
                                                            Region region,
                                                            S3Configuration serviceConfiguration,
                                                            String bucketName) {

        SdkHttpRequest.Builder mutableRequest = request.toBuilder();

        URI endpoint = resolveEndpoint(request, originalRequest, region, serviceConfiguration);
        mutableRequest.uri(endpoint);

        if (serviceConfiguration == null || !serviceConfiguration.pathStyleAccessEnabled()) {
            if (bucketName != null) {
                if (BucketUtils.isVirtualAddressingCompatibleBucketName(bucketName, false)) {
                    changeToDnsEndpoint(mutableRequest, bucketName);
                }
            }
        }

        return mutableRequest.build();
    }

    /**
     * Determine which endpoint to use based on region and {@link S3Configuration}. Will either be a traditional
     * S3 endpoint (i.e. s3.us-east-1.amazonaws.com), the global S3 accelerate endpoint (i.e. s3-accelerate.amazonaws.com) or
     * a regional dualstack endpoint for IPV6 (i.e. s3.dualstack.us-east-1.amazonaws.com).
     */
    private static URI resolveEndpoint(SdkHttpRequest request,
                                       Object originalRequest,
                                       Region region,
                                       S3Configuration serviceConfiguration) {
        RegionMetadata regionMetadata = RegionMetadata.of(region);
        String protocol = request.protocol();

        if (isAccelerateEnabled(serviceConfiguration) && isAccelerateSupported(originalRequest)) {
            return accelerateEndpoint(serviceConfiguration, regionMetadata, protocol);
        }

        if (serviceConfiguration != null && serviceConfiguration.dualstackEnabled()) {
            return dualstackEndpoint(regionMetadata, protocol);
        }

        return invokeSafely(() -> new URI(request.protocol(), null, request.host(), request.port(), null, null, null));
    }

    /**
     * Changes from path style addressing (which the marshallers produce by default, to DNS style or virtual style addressing
     * where the bucket name is prepended to the host. DNS style addressing is preferred due to the better load balancing
     * qualities it provides, path style is an option mainly for proxy based situations and alternative S3 implementations.
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

    /**
     * @return dual stack endpoint from given protocol and region metadata
     */
    private static URI dualstackEndpoint(RegionMetadata metadata, String protocol) {
        String serviceEndpoint = String.format("%s.%s.%s.%s", "s3", "dualstack", metadata.id(), metadata.domain());
        return toUri(protocol, serviceEndpoint);
    }

    /**
     * @return True if accelerate mode is enabled per {@link S3Configuration}, false if not.
     */
    private static boolean isAccelerateEnabled(S3Configuration serviceConfiguration) {
        return serviceConfiguration != null && serviceConfiguration.accelerateModeEnabled();
    }

    /**
     * @param originalRequest Request object to identify the operation.
     * @return True if accelerate is supported for the given operation, false if not.
     */
    private static boolean isAccelerateSupported(Object originalRequest) {
        return !ACCELERATE_DISABLED_OPERATIONS.contains(originalRequest.getClass());
    }

    /**
     * @return The endpoint for an S3 accelerate enabled operation. S3 accelerate has a single global endpoint.
     */
    private static URI accelerateEndpoint(S3Configuration serviceConfiguration, RegionMetadata metadata, String protocol) {
        if (serviceConfiguration.dualstackEnabled()) {
            return toUri(protocol, "s3-accelerate.dualstack." + metadata.domain());
        }
        return toUri(protocol, "s3-accelerate." + metadata.domain());
    }

    private static URI toUri(String protocol, String endpoint) {
        try {
            return new URI(String.format("%s://%s", protocol, endpoint));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
