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

package software.amazon.awssdk.services.s3control.internal.interceptors;


import static software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.SIGNING_REGION;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.CLIENT_ENDPOINT;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.ENDPOINT_OVERRIDDEN;
import static software.amazon.awssdk.services.s3control.internal.HandlerUtils.ENDPOINT_PREFIX;
import static software.amazon.awssdk.services.s3control.internal.HandlerUtils.S3_OUTPOSTS;
import static software.amazon.awssdk.services.s3control.internal.HandlerUtils.isDualstackEnabled;
import static software.amazon.awssdk.services.s3control.internal.HandlerUtils.isFipsEnabledInClientConfig;
import static software.amazon.awssdk.services.s3control.internal.HandlerUtils.isFipsRegion;
import static software.amazon.awssdk.services.s3control.internal.HandlerUtils.isFipsRegion;
import static software.amazon.awssdk.services.s3control.internal.HandlerUtils.isUseArnRegionEnabledInClientConfig;
import static software.amazon.awssdk.services.s3control.internal.S3ControlInternalExecutionAttribute.S3_ARNABLE_FIELD;

import java.net.URI;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.PartitionMetadata;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.internal.resource.S3OutpostResource;
import software.amazon.awssdk.services.s3.internal.resource.S3Resource;
import software.amazon.awssdk.services.s3.internal.usearnregion.UseArnRegionProviderChain;
import software.amazon.awssdk.services.s3control.S3ControlConfiguration;
import software.amazon.awssdk.services.s3control.internal.S3ArnableField;
import software.amazon.awssdk.services.s3control.internal.S3ControlArnConverter;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Execution interceptor which modifies the HTTP request to S3 Control to
 * change the endpoint to the correct endpoint. This includes prefixing the AWS
 * account identifier and, when enabled, adding in FIPS and dualstack.
 */
@SdkInternalApi
public final class EndpointAddressInterceptor implements ExecutionInterceptor {
    private static final String X_AMZ_OUTPOST_ID_HEADER = "x-amz-outpost-id";
    private static final UseArnRegionProviderChain USE_ARN_REGION_RESOLVER = UseArnRegionProviderChain.create();

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context,
                                            ExecutionAttributes executionAttributes) {
        Optional<Arn> requestArn = getRequestArn(executionAttributes);

        if (requestArn.isPresent()) {
            return resolveHostForOutpostArnRequest(context.httpRequest(), executionAttributes, requestArn.get());
        } else if (isNonArnOutpostRequest(context.request())) {
            return resolveHostForOutpostNonArnRequest(context.httpRequest(), executionAttributes);
        } else {
            return resolveHostForNonOutpostNonArnRequest(context.httpRequest(), executionAttributes);
        }
    }

    private SdkHttpRequest resolveHostForOutpostArnRequest(SdkHttpRequest request,
                                                           ExecutionAttributes executionAttributes,
                                                           Arn arn) {
        S3Resource s3Resource = S3ControlArnConverter.getInstance().convertArn(arn);

        S3ControlConfiguration serviceConfig = getServiceConfig(executionAttributes);
        String signingRegion = executionAttributes.getAttribute(SIGNING_REGION).id();
        String arnRegion = s3Resource.region().orElseThrow(() -> new IllegalArgumentException("Region is missing from ARN."));
        String arnPartion = arn.partition();
        S3Resource parentS3Resource = s3Resource.parentS3Resource().orElse(null);

        Validate.isTrue(!willCallFipsRegion(signingRegion, arnRegion, serviceConfig),
                        "FIPS is not supported for outpost requests.");

        // Even though we validated that we're not *calling* a FIPS region, the client region may still be a FIPS region if we're
        // using the ARN region. For that reason, we need to strip off the "fips" from the signing region before we get the
        // partition to make sure we're not making a cross-partition call.
        signingRegion = removeFipsIfNeeded(signingRegion);

        String signingPartition = PartitionMetadata.of(Region.of(signingRegion)).id();

        S3OutpostResource outpostResource = Validate.isInstanceOf(S3OutpostResource.class, parentS3Resource,
                                                                  "The ARN passed must have a parent outpost resource.");
        Validate.isTrue(!isDualstackEnabled(serviceConfig), "Dual stack endpoints are not supported for outpost requests.");
        Validate.isTrue(arnPartion.equals(signingPartition),
                        "The partition field of the ARN being passed as a bucket parameter to an S3 operation does not match "
                        + "the partition the client has been configured with. Provided partition: '%s'; client partition: '%s'.",
                        arnPartion, signingPartition);
        Validate.isTrue(useArnRegion(serviceConfig) || arnRegion.equals(signingRegion),
                        "The region field of the ARN being passed as a bucket parameter to an operation does not match the "
                        + "region the client was configured with. Provided region: '%s'; client region: '%s'.",
                        arnRegion, signingRegion);

        executionAttributes.putAttribute(SIGNING_REGION, Region.of(arnRegion));
        executionAttributes.putAttribute(SERVICE_SIGNING_NAME, S3_OUTPOSTS);

        SdkHttpRequest.Builder requestBuilder = request.toBuilder()
                                                       .appendHeader(X_AMZ_OUTPOST_ID_HEADER, outpostResource.outpostId());

        if (isEndpointOverridden(executionAttributes)) {
            // Drop endpoint prefix for ARN-based requests
            requestBuilder.host(endpointOverride(executionAttributes).getHost());
        } else {
            String arnPartitionDnsSuffix = PartitionMetadata.of(arnPartion).dnsSuffix();
            requestBuilder.host(String.format("s3-outposts.%s.%s", arnRegion, arnPartitionDnsSuffix));
        }

        return requestBuilder.build();
    }

    private SdkHttpRequest resolveHostForOutpostNonArnRequest(SdkHttpRequest sdkHttpRequest,
                                                              ExecutionAttributes executionAttributes) {
        S3ControlConfiguration serviceConfig = getServiceConfig(executionAttributes);
        Region signingRegion = executionAttributes.getAttribute(SIGNING_REGION);

        Validate.isTrue(!isDualstackEnabled(serviceConfig),
                        "Dual stack is not supported for outpost requests.");
        Validate.isTrue(!isFipsEnabledInClientConfig(serviceConfig) && !isFipsRegion(signingRegion.id()),
                        "FIPS endpoints are not supported for outpost requests.");

        executionAttributes.putAttribute(SERVICE_SIGNING_NAME, S3_OUTPOSTS);

        if (isEndpointOverridden(executionAttributes)) {
            // Preserve endpoint prefix for endpoint-overridden non-ARN-based requests
            return sdkHttpRequest;
        } else {
            String signingDnsSuffix = PartitionMetadata.of(signingRegion).dnsSuffix();
            return sdkHttpRequest.copy(r -> r.host(String.format("s3-outposts.%s.%s", signingRegion, signingDnsSuffix)));
        }
    }

    private SdkHttpRequest resolveHostForNonOutpostNonArnRequest(SdkHttpRequest request,
                                                                 ExecutionAttributes executionAttributes) {
        S3ControlConfiguration serviceConfig = getServiceConfig(executionAttributes);

        boolean isDualStackEnabled = isDualstackEnabled(serviceConfig);
        boolean isFipsEnabledInClient = isFipsEnabledInClientConfig(serviceConfig);

        Validate.isTrue(!isDualStackEnabled || !isFipsEnabledInClient, "Dual stack and FIPS are not supported together.");

        if (isEndpointOverridden(executionAttributes)) {
            Validate.isTrue(!isDualStackEnabled, "Dual stack is not supported with endpoint overrides.");
            Validate.isTrue(!isFipsEnabledInClient, "FIPS is not supported with endpoint overrides.");
            // Preserve endpoint prefix for endpoint-overridden non-ARN-based requests
            return request;
        } else if (isDualStackEnabled) {
            String newEndpointPrefix = String.format("%s.%s", ENDPOINT_PREFIX, "dualstack");
            return request.copy(r -> r.host(request.host().replace(ENDPOINT_PREFIX, newEndpointPrefix)));
        } else if (isFipsEnabledInClient) {
            String newEndpointPrefix = String.format("%s-%s", ENDPOINT_PREFIX, "fips");
            return request.copy(r -> r.host(request.host().replace(ENDPOINT_PREFIX, newEndpointPrefix)));
        } else {
            return request;
        }
    }

    private Optional<Arn> getRequestArn(ExecutionAttributes executionAttributes) {
        return Optional.ofNullable(executionAttributes.getAttribute(S3_ARNABLE_FIELD))
                       .map(S3ArnableField::arn);
    }

    private boolean isNonArnOutpostRequest(SdkRequest request) {
        return request.getValueForField("OutpostId", String.class)
                      .map(StringUtils::isNotBlank)
                      .orElse(false);
    }

    private S3ControlConfiguration getServiceConfig(ExecutionAttributes executionAttributes) {
        return (S3ControlConfiguration) executionAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_CONFIG);
    }

    private boolean useArnRegion(S3ControlConfiguration configuration) {
        // If useArnRegion is false, it was not set to false by the customer, it was simply not enabled
        if (isUseArnRegionEnabledInClientConfig(configuration)) {
            return true;
        }

        return USE_ARN_REGION_RESOLVER.resolveUseArnRegion().orElse(false);
    }

    private boolean isEndpointOverridden(ExecutionAttributes executionAttributes) {
        return Boolean.TRUE.equals(executionAttributes.getAttribute(ENDPOINT_OVERRIDDEN));
    }

    private URI endpointOverride(ExecutionAttributes executionAttributes) {
        return executionAttributes.getAttribute(CLIENT_ENDPOINT);
    }

    private boolean willCallFipsRegion(String signingRegion, String arnRegion, S3ControlConfiguration serviceConfig) {
        if (useArnRegion(serviceConfig)) {
            return isFipsRegion(arnRegion);
        }

        if (serviceConfig.fipsModeEnabled()) {
            return true;
        }

        return isFipsRegion(signingRegion);
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
}
