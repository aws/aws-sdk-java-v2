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

package software.amazon.awssdk.services.s3control.internal;

import static software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.SIGNING_REGION;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.ENDPOINT_OVERRIDDEN;
import static software.amazon.awssdk.services.s3control.internal.HandlerUtils.S3_OUTPOSTS;
import static software.amazon.awssdk.services.s3control.internal.HandlerUtils.isDualstackEnabled;
import static software.amazon.awssdk.services.s3control.internal.HandlerUtils.isFipsEnabledInClientConfig;
import static software.amazon.awssdk.services.s3control.internal.HandlerUtils.isFipsRegionProvided;
import static software.amazon.awssdk.services.s3control.internal.HandlerUtils.isUseArnRegionEnabledInClientConfig;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.PartitionMetadata;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.internal.resource.S3OutpostResource;
import software.amazon.awssdk.services.s3.internal.resource.S3Resource;
import software.amazon.awssdk.services.s3.internal.usearnregion.UseArnRegionProviderChain;
import software.amazon.awssdk.services.s3control.S3ControlConfiguration;

@SdkInternalApi
public final class ArnHandler {
    private static final String X_AMZ_OUTPOST_ID_HEADER = "x-amz-outpost-id";
    private static final ArnHandler INSTANCE = new ArnHandler();
    private static final UseArnRegionProviderChain USE_ARN_REGION_RESOLVER = UseArnRegionProviderChain.create();

    private ArnHandler() {
    }

    public static ArnHandler getInstance() {
        return INSTANCE;
    }

    public SdkHttpRequest resolveHostForArn(SdkHttpRequest request,
                                            S3ControlConfiguration configuration,
                                            Arn arn,
                                            ExecutionAttributes executionAttributes) {

        S3Resource s3Resource = S3ControlArnConverter.getInstance().convertArn(arn);

        String clientRegion = executionAttributes.getAttribute(SIGNING_REGION).id();
        String originalArnRegion = s3Resource.region().orElseThrow(() -> new IllegalArgumentException("Region is missing"));

        boolean isFipsEnabled = isFipsEnabledInClientConfig(configuration) || isFipsRegionProvided(clientRegion,
                                                                                                   originalArnRegion,
                                                                                                   useArnRegion(configuration));

        String arnRegion = removeFipsIfNeeded(originalArnRegion);
        validateConfiguration(executionAttributes, arn.partition(), arnRegion, configuration);

        executionAttributes.putAttribute(SIGNING_REGION, Region.of(arnRegion));

        S3Resource parentS3Resource = s3Resource.parentS3Resource().orElse(null);
        if (parentS3Resource instanceof S3OutpostResource) {
            return handleOutpostArn(request, (S3OutpostResource) parentS3Resource, isFipsEnabled, configuration,
                                    executionAttributes);
        } else {
            throw new IllegalArgumentException("Parent resource invalid, outpost resource expected.");
        }

    }

    private SdkHttpRequest handleOutpostArn(SdkHttpRequest request,
                                            S3OutpostResource outpostResource,
                                            boolean isFipsEnabled,
                                            S3ControlConfiguration configuration,
                                            ExecutionAttributes executionAttributes) {
        if (isFipsEnabled) {
            throw new IllegalArgumentException("FIPS endpoints are not supported for outpost ARNs");
        }

        if (isDualstackEnabled(configuration)) {
            throw new IllegalArgumentException("Dualstack endpoints are not supported for outpost ARNs");
        }

        executionAttributes.putAttribute(SERVICE_SIGNING_NAME, S3_OUTPOSTS);

        SdkHttpRequest.Builder requestBuilder = request.toBuilder().appendHeader(X_AMZ_OUTPOST_ID_HEADER,
                                                                                 outpostResource.outpostId());
        String arnRegion = outpostResource.region().orElseThrow(() -> new IllegalArgumentException("arn region is missing"));
        String dnsSuffix = PartitionMetadata.of(Region.of(arnRegion)).dnsSuffix();

        String host = String.format("s3-outposts.%s.%s", arnRegion, dnsSuffix);
        return requestBuilder.host(host).build();
    }

    private void validateConfiguration(ExecutionAttributes executionAttributes, String arnPartition, String arnRegion,
                                       S3ControlConfiguration configuration) {
        String clientRegionString = removeFipsIfNeeded(executionAttributes.getAttribute(SIGNING_REGION).id());
        Region clientRegion = Region.of(clientRegionString);

        if (Boolean.TRUE.equals(executionAttributes.getAttribute(ENDPOINT_OVERRIDDEN))) {
            throw new IllegalArgumentException("An ARN cannot be passed to an "
                                               + " operation if the client has been configured with an endpoint "
                                               + "override.");
        }
        String clientPartition = PartitionMetadata.of(clientRegion).id();

        if (!arnPartition.equals(clientPartition)) {
            throw new IllegalArgumentException("The partition field of the ARN being passed as a bucket parameter to "
                                               + "an S3 operation does not match the partition the client has been configured "
                                               + "with. Provided "
                                               + "partition: '" + arnPartition + "'; client partition: "
                                               + "'" + clientPartition + "'.");
        }

        if (!arnRegion.equals(clientRegionString) && !useArnRegion(configuration)) {
            throw new IllegalArgumentException("The region field of the ARN being passed as a bucket parameter to an "
                                               + "operation does not match the region the client was configured "
                                               + "with. Provided region: '" + arnRegion + "'; client "
                                               + "region: '" + clientRegionString + "'.");
        }
    }

    private String removeFipsIfNeeded(String region) {
        if (region.startsWith("fips-")) {
            return region.replace("fips-", "");
        }

        if (region.endsWith("-fips")) {
            return region.replace("-fips", "");
        }
        return region;
    }

    private boolean useArnRegion(S3ControlConfiguration configuration) {
        // If useArnRegion is false, it was not set to false by the customer, it was simply not enabled
        if (isUseArnRegionEnabledInClientConfig(configuration)) {
            return true;
        }

        return USE_ARN_REGION_RESOLVER.resolveUseArnRegion().orElse(false);
    }
}
