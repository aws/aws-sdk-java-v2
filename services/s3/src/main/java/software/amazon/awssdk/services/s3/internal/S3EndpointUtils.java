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

package software.amazon.awssdk.services.s3.internal;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.PartitionMetadata;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionMetadata;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.resource.S3AccessPointBuilder;
import software.amazon.awssdk.services.s3.internal.resource.S3AccessPointResource;
import software.amazon.awssdk.services.s3.internal.resource.S3ArnConverter;
import software.amazon.awssdk.services.s3.internal.resource.S3Resource;
import software.amazon.awssdk.services.s3.internal.resource.S3ResourceType;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.utils.Validate;

/**
 * Utilities for working with Amazon S3 bucket names, such as validation and
 * checked to see if they are compatible with DNS addressing.
 */
@SdkInternalApi
public final class S3EndpointUtils {

    private static final List<Class<?>> ACCELERATE_DISABLED_OPERATIONS = Arrays.asList(
        ListBucketsRequest.class, CreateBucketRequest.class, DeleteBucketRequest.class);

    private S3EndpointUtils() {
    }

    /**
     * Returns a new instance of the given {@link SdkHttpRequest} by applying any endpoint changes based on
     * the given {@link S3Configuration} options.
     */
    public static ConfiguredS3SdkHttpRequest applyEndpointConfiguration(SdkHttpRequest request,
                                                                        SdkRequest originalRequest,
                                                                        Region region,
                                                                        S3Configuration serviceConfiguration,
                                                                        boolean endpointOverridden) {
        String bucketName = originalRequest.getValueForField("Bucket", String.class).orElse(null);
        String key = originalRequest.getValueForField("Key", String.class).orElse(null);

        if (bucketName != null && isArn(bucketName)) {
            return applyEndpointConfigurationForAccessPointArn(request, region, endpointOverridden,
                                                               serviceConfiguration, bucketName, key);
        }

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

        return ConfiguredS3SdkHttpRequest.builder()
                                         .sdkHttpRequest(mutableRequest.build())
                                         .build();
    }

    private static ConfiguredS3SdkHttpRequest applyEndpointConfigurationForAccessPointArn(
            SdkHttpRequest request,
            Region region,
            boolean endpointOverridden,
            S3Configuration serviceConfiguration,
            String bucketName,
            String key) {

        Arn resourceArn = Arn.fromString(bucketName);
        S3Resource s3Resource = S3ArnConverter.create().convertArn(resourceArn);

        if (S3ResourceType.fromValue(s3Resource.type()) != S3ResourceType.ACCESS_POINT) {
            throw new IllegalArgumentException("An ARN was passed as a bucket parameter to an S3 operation, "
                                               + "however it does not appear to be a valid S3 access point ARN.");
        }

        String arnRegion = resourceArn.region().orElseThrow(() -> new IllegalArgumentException(
            "An S3 access point ARN must have a region"));

        if (isFipsRegion(region.toString())) {
            throw new IllegalArgumentException("An access point ARN cannot be passed as a bucket parameter to an S3"
                                               + " operation if the S3 client has been configured with a FIPS"
                                               + " enabled region.");
        }

        if (serviceConfiguration != null && serviceConfiguration.accelerateModeEnabled()) {
            throw new IllegalArgumentException("An access point ARN cannot be passed as a bucket parameter to an S3 "
                                               + "operation if the S3 client has been configured with accelerate mode"
                                               + " enabled.");
        }

        if (serviceConfiguration != null && serviceConfiguration.pathStyleAccessEnabled()) {
            throw new IllegalArgumentException("An access point ARN cannot be passed as a bucket parameter to an S3 "
                                               + "operation if the S3 client has been configured with path style "
                                               + "addressing enabled.");
        }

        if (endpointOverridden) {
            throw new IllegalArgumentException("An access point ARN cannot be passed as a bucket parameter to an S3"
                                               + " operation if the S3 client has been configured with an endpoint "
                                               + "override.");
        }

        if (serviceConfiguration == null || !serviceConfiguration.useArnRegionEnabled()) {
            if (!region.id().equals(arnRegion)) {
                throw new IllegalArgumentException(
                    String.format("The region field of the ARN being passed as a bucket parameter to an S3 operation "
                                  + "does not match the region the client was configured with. To enable this "
                                  + "behavior and prevent this exception set 'useArnRegionEnabled' to true in the "
                                  + "configuration when building the S3 client. Provided region: '%s'; client region:"
                                  + " '%s'.", arnRegion, region));
            }
        }

        PartitionMetadata clientPartitionMetadata = PartitionMetadata.of(region);
        String clientPartition = clientPartitionMetadata.id();

        if (clientPartition == null || clientPartition.isEmpty() || !s3Resource.partition().isPresent()
            || !clientPartition.equals(s3Resource.partition().get())) {
            throw new IllegalArgumentException(
                String.format("The partition field of the ARN being passed as a bucket parameter to an S3 operation "
                              + "does not match the partition the S3 client has been configured with. Provided "
                              + "partition: '%s'; client partition: '%s'.", s3Resource.partition().orElse(""),
                              clientPartition));
        }

        S3AccessPointResource s3EndpointResource =
            Validate.isInstanceOf(S3AccessPointResource.class, s3Resource,
                                  "An ARN was passed as a bucket parameter to an S3 operation, however it does not "
                                  + "appear to be a valid S3 access point ARN.");

        // DualstackEnabled considered false by default
        boolean dualstackEnabled = serviceConfiguration != null && serviceConfiguration.dualstackEnabled();

        URI accessPointUri =
            S3AccessPointBuilder.create()
                                .accessPointName(s3EndpointResource.accessPointName())
                                .accountId(
                                    s3EndpointResource.accountId().orElseThrow(() -> new IllegalArgumentException(
                                        "An S3 access point ARN must have an account ID")))
                                .region(arnRegion)
                                .protocol(request.protocol())
                                .domain(clientPartitionMetadata.dnsSuffix())
                                .dualstackEnabled(dualstackEnabled)
                                .toUri();

        SdkHttpRequest httpRequest = request.toBuilder()
                                            .protocol(accessPointUri.getScheme())
                                            .host(accessPointUri.getHost())
                                            .port(accessPointUri.getPort())
                                            .encodedPath(key)
                                            .build();

        return ConfiguredS3SdkHttpRequest.builder()
                                         .sdkHttpRequest(httpRequest)
                                         .signingRegionModification(Region.of(arnRegion))
                                         .build();
    }

    /**
     * Determine which endpoint to use based on region and {@link S3Configuration}. Will either be a traditional
     * S3 endpoint (i.e. s3.us-east-1.amazonaws.com), the global S3 accelerate endpoint (i.e. s3-accelerate.amazonaws.com) or
     * a regional dualstack endpoint for IPV6 (i.e. s3.dualstack.us-east-1.amazonaws.com).
     */
    private static URI resolveEndpoint(SdkHttpRequest request,
                                       SdkRequest originalRequest,
                                       Region region,
                                       S3Configuration serviceConfiguration) {

        String protocol = request.protocol();

        RegionMetadata regionMetadata = RegionMetadata.of(region);

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
    private static boolean isAccelerateSupported(SdkRequest originalRequest) {
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

    private static boolean isArn(String s) {
        return s.startsWith("arn:");
    }

    private static boolean isFipsRegion(String region) {
        return region.startsWith("fips-") || region.endsWith("-fips");
    }
}
