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
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.ENDPOINT_OVERRIDDEN;
import static software.amazon.awssdk.services.s3control.internal.HandlerUtils.ENDPOINT_PREFIX;
import static software.amazon.awssdk.services.s3control.internal.HandlerUtils.S3_OUTPOSTS;
import static software.amazon.awssdk.services.s3control.internal.HandlerUtils.isDualstackEnabled;
import static software.amazon.awssdk.services.s3control.internal.HandlerUtils.isFipsEnabledInClientConfig;
import static software.amazon.awssdk.services.s3control.internal.HandlerUtils.isFipsRegion;
import static software.amazon.awssdk.services.s3control.internal.S3ControlInternalExecutionAttribute.S3_ARNABLE_FIELD;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.PartitionMetadata;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3control.S3ControlConfiguration;
import software.amazon.awssdk.services.s3control.internal.ArnHandler;
import software.amazon.awssdk.services.s3control.internal.S3ArnableField;
import software.amazon.awssdk.services.s3control.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3control.model.ListRegionalBucketsRequest;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Execution interceptor which modifies the HTTP request to S3 Control to
 * change the endpoint to the correct endpoint. This includes prefixing the AWS
 * account identifier and, when enabled, adding in FIPS and dualstack.
 */
@SdkInternalApi
public final class EndpointAddressInterceptor implements ExecutionInterceptor {
    private final ArnHandler arnHandler;

    public EndpointAddressInterceptor() {
        arnHandler = ArnHandler.getInstance();
    }

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context,
                                            ExecutionAttributes executionAttributes) {
        SdkHttpRequest request = context.httpRequest();

        S3ControlConfiguration config = (S3ControlConfiguration) executionAttributes.getAttribute(
            AwsSignerExecutionAttribute.SERVICE_CONFIG);

        S3ArnableField arnableField = executionAttributes.getAttribute(S3_ARNABLE_FIELD);

        if (arnableField != null && arnableField.arn() != null) {
            return arnHandler.resolveHostForArn(request, config, arnableField.arn(), executionAttributes);
        }

        String host;

        // If the request is an non-arn outpost request
        if (isNonArnOutpostRequest(context.request())) {
            host = resolveHostForNonArnOutpostRequest(config, executionAttributes);
        } else {
            host = resolveHost(request, config);
        }

        return request.toBuilder()
                      .host(host)
                      .build();
    }

    private String resolveHostForNonArnOutpostRequest(S3ControlConfiguration configuration,
                                                      ExecutionAttributes executionAttributes) {
        if (Boolean.TRUE.equals(executionAttributes.getAttribute(ENDPOINT_OVERRIDDEN))) {
            throw new IllegalArgumentException("Endpoint must not be overridden");
        }

        if (isDualstackEnabled(configuration)) {
            throw new IllegalArgumentException("Dualstack endpoints are not supported");
        }

        Region region = executionAttributes.getAttribute(SIGNING_REGION);
        if (isFipsEnabledInClientConfig(configuration) || isFipsRegion(region.id())) {
            throw new IllegalArgumentException("FIPS endpoints are not supported");
        }

        executionAttributes.putAttribute(SERVICE_SIGNING_NAME, S3_OUTPOSTS);

        String dnsSuffix = PartitionMetadata.of(region).dnsSuffix();

        return String.format("s3-outposts.%s.%s", region, dnsSuffix);
    }

    /**
     * It should redirect signer if the request is CreateBucketRequest or ListRegionalBucketsRequest with outpostId present
     */
    private boolean isNonArnOutpostRequest(SdkRequest request) {
        if (request instanceof CreateBucketRequest && (StringUtils.isNotBlank(((CreateBucketRequest) request).outpostId()))) {
            return true;
        }

        return request instanceof ListRegionalBucketsRequest &&
               (StringUtils.isNotBlank(((ListRegionalBucketsRequest) request).outpostId()));
    }

    private String resolveHost(SdkHttpRequest request, S3ControlConfiguration configuration) {
        if (isDualstackEnabled(configuration) && isFipsEnabledInClientConfig(configuration)) {
            throw SdkClientException.create("Cannot use both Dual-Stack endpoints and FIPS endpoints");
        }
        String host = request.getUri().getHost();
        if (isDualstackEnabled(configuration)) {
            if (!host.contains(ENDPOINT_PREFIX)) {
                throw SdkClientException.create(String.format("The Dual-Stack option cannot be used with custom endpoints (%s)",
                                                              request.getUri()));
            }
            host = host.replace(ENDPOINT_PREFIX, String.format("%s.%s", ENDPOINT_PREFIX, "dualstack"));
        } else if (isFipsEnabledInClientConfig(configuration)) {
            if (!host.contains(ENDPOINT_PREFIX)) {
                throw SdkClientException.create(String.format("The FIPS option cannot be used with custom endpoints (%s)",
                                                              request.getUri()));
            }
            host = host.replace(ENDPOINT_PREFIX, String.format("%s-%s", ENDPOINT_PREFIX, "fips"));

        }
        return host;
    }
}
