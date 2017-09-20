/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.s3.handlers;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.interceptor.AwsExecutionAttributes;
import software.amazon.awssdk.interceptor.Context;
import software.amazon.awssdk.interceptor.ExecutionAttributes;
import software.amazon.awssdk.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionMetadata;
import software.amazon.awssdk.services.s3.BucketUtils;
import software.amazon.awssdk.services.s3.S3AdvancedConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;

public class EndpointAddressInterceptor implements ExecutionInterceptor {

    private static List<Class<?>> ACCELERATE_DISABLED_OPERATIONS = Arrays.asList(
            ListBucketsRequest.class, CreateBucketRequest.class, DeleteBucketRequest.class);

    @Override
    public SdkHttpFullRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        SdkHttpFullRequest request = context.httpRequest();
        Object originalRequest = context.request();

        S3AdvancedConfiguration advancedConfiguration =
                (S3AdvancedConfiguration) executionAttributes.getAttribute(AwsExecutionAttributes.SERVICE_ADVANCED_CONFIG);
        SdkHttpFullRequest.Builder mutableRequest = request.toBuilder();

        mutableRequest.endpoint(resolveEndpoint(request.getEndpoint(), originalRequest,
                                                executionAttributes, advancedConfiguration));

        if (advancedConfiguration == null || !advancedConfiguration.pathStyleAccessEnabled()) {
            try {
                String bucketName = getBucketName(originalRequest);

                if (BucketUtils.isValidDnsBucketName(bucketName, false)) {
                    changeToDnsEndpoint(mutableRequest, bucketName);
                }
            } catch (Exception e) {
                // Unable to convert to DNS style addressing. Fall back to continue using path style.
            }
        }

        return mutableRequest.build();
    }

    /**
     * Determine which endpoint to use based on region and {@link S3AdvancedConfiguration}. Will either be a traditional
     * S3 endpoint (i.e. s3.us-east-1.amazonaws.com), the global S3 accelerate endpoint (i.e. s3-accelerate.amazonaws.com) or
     * a regional dualstack endpoint for IPV6 (i.e. s3.dualstack.us-east-1.amazonaws.com).
     */
    private URI resolveEndpoint(URI originalEndpoint,
                                Object originalRequest,
                                ExecutionAttributes executionAttributes,
                                S3AdvancedConfiguration advancedConfiguration) {
        Region region = executionAttributes.getAttribute(AwsExecutionAttributes.AWS_REGION);
        RegionMetadata regionMetadata = RegionMetadata.of(region);
        if (isAccelerateEnabled(advancedConfiguration) && isAccelerateSupported(originalRequest)) {
            return accelerateEndpoint(advancedConfiguration, regionMetadata);
        } else if (advancedConfiguration != null && advancedConfiguration.dualstackEnabled()) {
            return dualstackEndpoint(regionMetadata);
        } else {
            return originalEndpoint;
        }
    }

    private static URI dualstackEndpoint(RegionMetadata metadata) {
        String serviceEndpoint = String.format("%s.%s.%s.%s", "s3", "dualstack", metadata.getName(), metadata.getDomain());
        return toUri(serviceEndpoint);
    }

    /**
     * @return True if accelerate mode is enabled per {@link S3AdvancedConfiguration}, false if not.
     */
    private static boolean isAccelerateEnabled(S3AdvancedConfiguration advancedConfiguration) {
        return advancedConfiguration != null && advancedConfiguration.accelerateModeEnabled();
    }

    /**
     * @param originalRequest Request object to identify the operation.
     * @return True if accelerate is supported for the given operation, false if not.
     */
    private boolean isAccelerateSupported(Object originalRequest) {
        return !ACCELERATE_DISABLED_OPERATIONS.contains(originalRequest.getClass());
    }

    /**
     * @return The endpoint for an S3 accelerate enabled operation. S3 accelerate has a single global endpoint.
     */
    private static URI accelerateEndpoint(S3AdvancedConfiguration advancedConfiguration, RegionMetadata metadata) {
        if (advancedConfiguration.dualstackEnabled()) {
            return toUri("s3-accelerate.dualstack." + metadata.getDomain());
        }
        return toUri("s3-accelerate." + metadata.getDomain());
    }

    private static URI toUri(String endpoint) throws IllegalArgumentException {
        try {
            return new URI(String.format("%s://%s", "https", endpoint));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @ReviewBeforeRelease("Remove reflection here. Have some kind of interface where we can get bucket name or pass it" +
                         "in the handler context")
    private String getBucketName(Object originalRequest) throws IllegalAccessException, InvocationTargetException,
                                                                NoSuchMethodException {
        return (String) originalRequest.getClass()
                                       .getMethod("bucket")
                                       .invoke(originalRequest);
    }

    /**
     * Changes from path style addressing (which the marshallers produce by default, to DNS style or virtual style addressing
     * where the bucket name is prepended to the host. DNS style addressing is preferred due to the better load balancing
     * qualities it provides, path style is an option mainly for proxy based situations and alternative S3 implementations.
     *
     * @param mutableRequest Marshalled HTTP request we are modifying.
     * @param bucketName     Bucket name for this particular operation.
     */
    private void changeToDnsEndpoint(SdkHttpFullRequest.Builder mutableRequest, String bucketName) {
        if (mutableRequest.getEndpoint().getHost().startsWith("s3")) {
            // Replace /bucketName from resourcePath with nothing
            String resourcePath = mutableRequest.getResourcePath().replaceFirst("/" + bucketName, "");

            // Prepend bucket to endpoint
            URI endpoint = invokeSafely(() -> new URI(
                    mutableRequest.getEndpoint().getScheme(), // Existing scheme
                    // replace "s3" with "bucket.s3"
                    mutableRequest.getEndpoint().getHost().replaceFirst("s3", bucketName + "." + "s3"),
                    null,
                    null));

            mutableRequest.endpoint(endpoint);
            mutableRequest.resourcePath(resourcePath);
        }
    }
}
