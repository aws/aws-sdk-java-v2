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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;

/**
 * Utilities for working with Amazon S3 bucket names and endpoints.
 */
@SdkInternalApi
public final class S3EndpointUtils {

    private static final List<Class<?>> ACCELERATE_DISABLED_OPERATIONS = Arrays.asList(
        ListBucketsRequest.class, CreateBucketRequest.class, DeleteBucketRequest.class);

    private S3EndpointUtils() {
    }

    public static String removeFipsIfNeeded(String region) {
        if (region.startsWith("fips-")) {
            return region.replace("fips-", "");
        }

        if (region.endsWith("-fips")) {
            return region.replace("-fips", "");
        }
        return region;
    }

    /**
     * Returns whether a FIPS pseudo region is provided.
     */
    public static boolean isFipsRegionProvided(String clientRegion, String arnRegion, boolean useArnRegion) {
        if (useArnRegion) {
            return isFipsRegion(arnRegion);
        }
        return isFipsRegion(clientRegion);
    }

    public static boolean isFipsRegion(String region) {
        return region.startsWith("fips-") || region.endsWith("-fips");
    }

    /**
     * @return True if accelerate mode is enabled per {@link S3Configuration}, false if not.
     */
    public static boolean isAccelerateEnabled(S3Configuration serviceConfiguration) {
        return serviceConfiguration != null && serviceConfiguration.accelerateModeEnabled();
    }

    /**
     * @param originalRequest Request object to identify the operation.
     * @return True if accelerate is supported for the given operation, false if not.
     */
    public static boolean isAccelerateSupported(SdkRequest originalRequest) {
        return !ACCELERATE_DISABLED_OPERATIONS.contains(originalRequest.getClass());
    }

    /**
     * @return The endpoint for an S3 accelerate enabled operation. S3 accelerate has a single global endpoint.
     */
    public static URI accelerateEndpoint(S3Configuration serviceConfiguration, String domain, String protocol) {
        if (serviceConfiguration.dualstackEnabled()) {
            return toUri(protocol, "s3-accelerate.dualstack." + domain);
        }
        return toUri(protocol, "s3-accelerate." + domain);
    }

    /**
     * @return True if dualstack is enabled per {@link S3Configuration}, false if not.
     */
    public static boolean isDualstackEnabled(S3Configuration serviceConfiguration) {
        return serviceConfiguration != null && serviceConfiguration.dualstackEnabled();
    }

    /**
     * @return dual stack endpoint from given protocol and region metadata
     */
    public static URI dualstackEndpoint(String id, String domain, String protocol) {
        String serviceEndpoint = String.format("%s.%s.%s.%s", "s3", "dualstack", id, domain);
        return toUri(protocol, serviceEndpoint);
    }

    /**
     * @return True if path style access is enabled per {@link S3Configuration}, false if not.
     */
    public static boolean isPathStyleAccessEnabled(S3Configuration serviceConfiguration) {
        return serviceConfiguration != null && serviceConfiguration.pathStyleAccessEnabled();
    }

    public static boolean isArnRegionEnabled(S3Configuration serviceConfiguration) {
        return serviceConfiguration != null && serviceConfiguration.useArnRegionEnabled();
    }

    /**
     * Changes from path style addressing (which the marshallers produce by default, to DNS style or virtual style addressing
     * where the bucket name is prepended to the host. DNS style addressing is preferred due to the better load balancing
     * qualities it provides, path style is an option mainly for proxy based situations and alternative S3 implementations.
     *
     * @param mutableRequest Marshalled HTTP request we are modifying.
     * @param bucketName     Bucket name for this particular operation.
     */
    public static void changeToDnsEndpoint(SdkHttpRequest.Builder mutableRequest, String bucketName) {
        if (mutableRequest.host().startsWith("s3")) {
            String newHost = mutableRequest.host().replaceFirst("s3", bucketName + "." + "s3");
            String newPath = mutableRequest.encodedPath().replaceFirst("/" + bucketName, "");

            mutableRequest.host(newHost).encodedPath(newPath);
        }
    }

    public static boolean isArn(String s) {
        return s.startsWith("arn:");
    }

    private static URI toUri(String protocol, String endpoint) {
        try {
            return new URI(String.format("%s://%s", protocol, endpoint));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
