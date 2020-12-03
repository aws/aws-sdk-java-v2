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

import static software.amazon.awssdk.services.s3.internal.endpoints.S3EndpointUtils.isAccelerateEnabled;
import static software.amazon.awssdk.services.s3.internal.endpoints.S3EndpointUtils.isArnRegionEnabled;
import static software.amazon.awssdk.services.s3.internal.endpoints.S3EndpointUtils.isDualstackEnabled;
import static software.amazon.awssdk.services.s3.internal.endpoints.S3EndpointUtils.isFipsRegion;
import static software.amazon.awssdk.services.s3.internal.endpoints.S3EndpointUtils.isFipsRegionProvided;
import static software.amazon.awssdk.services.s3.internal.endpoints.S3EndpointUtils.isPathStyleAccessEnabled;
import static software.amazon.awssdk.services.s3.internal.endpoints.S3EndpointUtils.removeFipsIfNeeded;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.PartitionMetadata;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.ConfiguredS3SdkHttpRequest;
import software.amazon.awssdk.services.s3.internal.resource.S3AccessPointBuilder;
import software.amazon.awssdk.services.s3.internal.resource.S3AccessPointResource;
import software.amazon.awssdk.services.s3.internal.resource.S3ArnConverter;
import software.amazon.awssdk.services.s3.internal.resource.S3OutpostAccessPointBuilder;
import software.amazon.awssdk.services.s3.internal.resource.S3OutpostResource;
import software.amazon.awssdk.services.s3.internal.resource.S3Resource;
import software.amazon.awssdk.services.s3.internal.resource.S3ResourceType;
import software.amazon.awssdk.utils.Validate;

/**
 * Returns a new configured HTTP request with a resolved access point endpoint and signing overrides.
 */
@SdkInternalApi
public final class S3AccessPointEndpointResolver implements S3EndpointResolver {

    private static final String S3_OUTPOSTS_NAME = "s3-outposts";

    private S3AccessPointEndpointResolver() {
    }

    public static S3AccessPointEndpointResolver create() {
        return new S3AccessPointEndpointResolver();
    }

    @Override
    public ConfiguredS3SdkHttpRequest applyEndpointConfiguration(S3EndpointResolverContext context) {

        S3Resource s3Resource = S3ArnConverter.create().convertArn(Arn.fromString(getBucketName(context)));
        if (S3ResourceType.fromValue(s3Resource.type()) != S3ResourceType.ACCESS_POINT) {
            throw new IllegalArgumentException("An ARN was passed as a bucket parameter to an S3 operation, "
                                               + "however it does not appear to be a valid S3 access point ARN.");
        }

        Region region = context.region();
        PartitionMetadata clientPartitionMetadata = PartitionMetadata.of(region);

        String arnRegion = validateConfiguration(context, s3Resource);

        S3AccessPointResource s3EndpointResource = Validate.isInstanceOf(S3AccessPointResource.class, s3Resource,
                                  "An ARN was passed as a bucket parameter to an S3 operation, however it does not "
                                  + "appear to be a valid S3 access point ARN.");

        URI accessPointUri = getUriForAccessPointResource(context, arnRegion, clientPartitionMetadata, s3EndpointResource);
        String key = context.originalRequest().getValueForField("Key", String.class).orElse(null);
        SdkHttpRequest httpRequest = context.request().toBuilder()
                                            .protocol(accessPointUri.getScheme())
                                            .host(accessPointUri.getHost())
                                            .port(accessPointUri.getPort())
                                            .encodedPath(key)
                                            .build();

        String signingServiceModification = s3EndpointResource.parentS3Resource()
                                                              .filter(r -> r instanceof S3OutpostResource)
                                                              .map(ignore -> S3_OUTPOSTS_NAME)
                                                              .orElse(null);

        return ConfiguredS3SdkHttpRequest.builder()
                                         .sdkHttpRequest(httpRequest)
                                         .signingRegionModification(Region.of(arnRegion))
                                         .signingServiceModification(signingServiceModification)
                                         .build();
    }

    private String validateConfiguration(S3EndpointResolverContext context, S3Resource s3Resource) {
        Region region = context.region();
        String arnRegion = s3Resource.region().orElseThrow(() -> new IllegalArgumentException(
            "An S3 access point ARN must have a region"));


        S3Configuration serviceConfiguration = context.serviceConfiguration();
        if (isAccelerateEnabled(serviceConfiguration)) {
            throw new IllegalArgumentException("An access point ARN cannot be passed as a bucket parameter to an S3 "
                                               + "operation if the S3 client has been configured with accelerate mode"
                                               + " enabled.");
        }

        if (isPathStyleAccessEnabled(serviceConfiguration)) {
            throw new IllegalArgumentException("An access point ARN cannot be passed as a bucket parameter to an S3 "
                                               + "operation if the S3 client has been configured with path style "
                                               + "addressing enabled.");
        }

        if (context.endpointOverridden()) {
            throw new IllegalArgumentException("An access point ARN cannot be passed as a bucket parameter to an S3"
                                               + " operation if the S3 client has been configured with an endpoint "
                                               + "override.");
        }

        if (!isArnRegionEnabled(serviceConfiguration) && clientRegionDiffersFromArnRegion(region, arnRegion)) {
            throw new IllegalArgumentException(
                String.format("The region field of the ARN being passed as a bucket parameter to an S3 operation "
                              + "does not match the region the client was configured with. To enable this "
                              + "behavior and prevent this exception set 'useArnRegionEnabled' to true in the "
                              + "configuration when building the S3 client. Provided region: '%s'; client region:"
                              + " '%s'.", arnRegion, region));
        }

        String clientPartition = PartitionMetadata.of(region).id();

        if (illegalPartitionConfiguration(s3Resource, clientPartition)) {
            throw new IllegalArgumentException(
                String.format("The partition field of the ARN being passed as a bucket parameter to an S3 operation "
                              + "does not match the partition the S3 client has been configured with. Provided "
                              + "partition: '%s'; client partition: '%s'.", s3Resource.partition().orElse(""),
                              clientPartition));
        }
        return arnRegion;
    }

    private boolean clientRegionDiffersFromArnRegion(Region clientRegion, String arnRegion) {
        return !removeFipsIfNeeded(clientRegion.id()).equals(removeFipsIfNeeded(arnRegion));
    }

    private boolean illegalPartitionConfiguration(S3Resource s3Resource, String clientPartition) {
        return clientPartition == null || clientPartition.isEmpty() || !s3Resource.partition().isPresent()
               || !clientPartition.equals(s3Resource.partition().get());
    }

    private String getBucketName(S3EndpointResolverContext context) {
        return context.originalRequest().getValueForField("Bucket", String.class).orElseThrow(
            () -> new IllegalArgumentException("Bucket name cannot be empty when parsing access points."));
    }

    private URI getUriForAccessPointResource(S3EndpointResolverContext context, String arnRegion,
                                                    PartitionMetadata clientPartitionMetadata,
                                                    S3AccessPointResource s3EndpointResource) {

        boolean dualstackEnabled = isDualstackEnabled(context.serviceConfiguration());
        boolean fipsRegionProvided = isFipsRegionProvided(context.region().toString(), arnRegion,
                                                          isArnRegionEnabled(context.serviceConfiguration()));

        String accountId = s3EndpointResource.accountId().orElseThrow(() -> new IllegalArgumentException(
            "An S3 access point ARN must have an account ID"));
        String accessPointName = s3EndpointResource.accessPointName();

        if (isOutpostAccessPoint(s3EndpointResource)) {
            return getOutpostAccessPointUri(context, arnRegion, clientPartitionMetadata, s3EndpointResource);
        }

        return S3AccessPointBuilder.create()
                                   .accessPointName(accessPointName)
                                   .accountId(accountId)
                                   .fipsEnabled(fipsRegionProvided)
                                   .region(removeFipsIfNeeded(arnRegion))
                                   .protocol(context.request().protocol())
                                   .domain(clientPartitionMetadata.dnsSuffix())
                                   .dualstackEnabled(dualstackEnabled)
                                   .toUri();
    }

    private boolean isOutpostAccessPoint(S3AccessPointResource s3EndpointResource) {
        return s3EndpointResource.parentS3Resource().filter(r -> r instanceof S3OutpostResource).isPresent();
    }

    private URI getOutpostAccessPointUri(S3EndpointResolverContext context, String arnRegion,
                                         PartitionMetadata clientPartitionMetadata, S3AccessPointResource s3EndpointResource) {
        if (isDualstackEnabled(context.serviceConfiguration())) {
            throw new IllegalArgumentException("An Outpost Access Point ARN cannot be passed as a bucket parameter to an S3 "
                                               + "operation if the S3 client has been configured with dualstack");
        }

        if (isFipsRegion(context.region().toString())) {
            throw new IllegalArgumentException("An access point ARN cannot be passed as a bucket parameter to an S3"
                                               + " operation if the S3 client has been configured with a FIPS"
                                               + " enabled region.");
        }

        S3OutpostResource parentResource = (S3OutpostResource) s3EndpointResource.parentS3Resource().get();
        return S3OutpostAccessPointBuilder.create()
                                          .accountId(s3EndpointResource.accountId().get())
                                          .outpostId(parentResource.outpostId())
                                          .region(arnRegion)
                                          .accessPointName(s3EndpointResource.accessPointName())
                                          .protocol(context.request().protocol())
                                          .domain(clientPartitionMetadata.dnsSuffix())
                                          .toUri();
    }

}
