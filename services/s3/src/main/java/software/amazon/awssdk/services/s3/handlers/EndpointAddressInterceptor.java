/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.auth.AwsExecutionAttributes;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionMetadata;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.BucketUtils;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

public class EndpointAddressInterceptor implements ExecutionInterceptor {

    private static List<Class<?>> ACCELERATE_DISABLED_OPERATIONS = Arrays.asList(
            ListBucketsRequest.class, CreateBucketRequest.class, DeleteBucketRequest.class);

    @Override
    public SdkHttpFullRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        SdkHttpFullRequest request = context.httpRequest();
        SdkRequest sdkRequest = context.request();

        S3Configuration serviceConfiguration =
                (S3Configuration) executionAttributes.getAttribute(AwsExecutionAttributes.SERVICE_CONFIG);
        SdkHttpFullRequest.Builder mutableRequest = request.toBuilder();

        URI endpoint = resolveEndpoint(request, sdkRequest,
                                       executionAttributes, serviceConfiguration);
        mutableRequest.protocol(endpoint.getScheme())
                      .host(endpoint.getHost())
                      .port(endpoint.getPort())
                      .encodedPath(SdkHttpUtils.appendUri(endpoint.getPath(), mutableRequest.encodedPath()));

        if (serviceConfiguration == null || !serviceConfiguration.pathStyleAccessEnabled()) {
            sdkRequest.getValueForField("Bucket", String.class).ifPresent(b -> {
                if (BucketUtils.isVirtualAddressingCompatibleBucketName(b, false)) {
                    changeToDnsEndpoint(mutableRequest, b);
                }
            });
        }

        return mutableRequest.build();
    }

    /**
     * Determine which endpoint to use based on region and {@link S3Configuration}. Will either be a traditional
     * S3 endpoint (i.e. s3.us-east-1.amazonaws.com), the global S3 accelerate endpoint (i.e. s3-accelerate.amazonaws.com) or
     * a regional dualstack endpoint for IPV6 (i.e. s3.dualstack.us-east-1.amazonaws.com).
     */
    private URI resolveEndpoint(SdkHttpFullRequest request,
                                SdkRequest originalRequest,
                                ExecutionAttributes executionAttributes,
                                S3Configuration serviceConfiguration) {
        Region region = executionAttributes.getAttribute(AwsExecutionAttributes.AWS_REGION);
        RegionMetadata regionMetadata = RegionMetadata.of(region);
        if (isAccelerateEnabled(serviceConfiguration) && isAccelerateSupported(originalRequest)) {
            return accelerateEndpoint(serviceConfiguration, regionMetadata);
        } else if (serviceConfiguration != null && serviceConfiguration.dualstackEnabled()) {
            return dualstackEndpoint(regionMetadata);
        } else {
            return invokeSafely(() -> new URI(request.protocol(), null, request.host(), request.port(), null, null, null));
        }
    }

    private static URI dualstackEndpoint(RegionMetadata metadata) {
        String serviceEndpoint = String.format("%s.%s.%s.%s", "s3", "dualstack", metadata.getName(), metadata.getDomain());
        return toUri(serviceEndpoint);
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
    private boolean isAccelerateSupported(Object originalRequest) {
        return !ACCELERATE_DISABLED_OPERATIONS.contains(originalRequest.getClass());
    }

    /**
     * @return The endpoint for an S3 accelerate enabled operation. S3 accelerate has a single global endpoint.
     */
    private static URI accelerateEndpoint(S3Configuration serviceConfiguration, RegionMetadata metadata) {
        if (serviceConfiguration.dualstackEnabled()) {
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

    /**
     * Changes from path style addressing (which the marshallers produce by default, to DNS style or virtual style addressing
     * where the bucket name is prepended to the host. DNS style addressing is preferred due to the better load balancing
     * qualities it provides, path style is an option mainly for proxy based situations and alternative S3 implementations.
     *
     * @param mutableRequest Marshalled HTTP request we are modifying.
     * @param bucketName     Bucket name for this particular operation.
     */
    private void changeToDnsEndpoint(SdkHttpFullRequest.Builder mutableRequest, String bucketName) {
        if (mutableRequest.host().startsWith("s3")) {
            String newHost = mutableRequest.host().replaceFirst("s3", bucketName + "." + "s3");
            String newPath = mutableRequest.encodedPath().replaceFirst("/" + bucketName, "");

            mutableRequest.host(newHost).encodedPath(newPath);
        }
    }
}
