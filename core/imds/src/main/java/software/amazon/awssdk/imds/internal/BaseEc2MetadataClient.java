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

package software.amazon.awssdk.imds.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.imds.Ec2MetadataRetryPolicy;
import software.amazon.awssdk.imds.EndpointMode;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public abstract class BaseEc2MetadataClient {

    protected static final Duration DEFAULT_TOKEN_TTL = Duration.of(21_600, ChronoUnit.SECONDS);
    protected static final AttributeMap IMDS_HTTP_DEFAULTS =
        AttributeMap.builder()
                    .put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, Duration.ofSeconds(1))
                    .put(SdkHttpConfigurationOption.READ_TIMEOUT, Duration.ofSeconds(1))
                    .build();

    private static final Logger log = Logger.loggerFor(BaseEc2MetadataClient.class);

    protected final Ec2MetadataRetryPolicy retryPolicy;
    protected final URI endpoint;
    protected final RequestMarshaller requestMarshaller;
    protected final Duration tokenTtl;

    private BaseEc2MetadataClient(Ec2MetadataRetryPolicy retryPolicy, Duration tokenTtl, URI endpoint,
                                  EndpointMode endpointMode) {
        this.retryPolicy = Validate.getOrDefault(retryPolicy, Ec2MetadataRetryPolicy.builder()::build);
        this.tokenTtl = Validate.getOrDefault(tokenTtl, () -> DEFAULT_TOKEN_TTL);
        this.endpoint = getEndpoint(endpoint, endpointMode);
        this.requestMarshaller = new RequestMarshaller(this.endpoint);
    }

    protected BaseEc2MetadataClient(DefaultEc2MetadataClient.Ec2MetadataBuilder builder) {
        this(builder.getRetryPolicy(), builder.getTokenTtl(), builder.getEndpoint(), builder.getEndpointMode());
    }

    protected BaseEc2MetadataClient(DefaultEc2MetadataAsyncClient.Ec2MetadataAsyncBuilder builder) {
        this(builder.getRetryPolicy(), builder.getTokenTtl(), builder.getEndpoint(), builder.getEndpointMode());
    }

    private URI getEndpoint(URI builderEndpoint, EndpointMode builderEndpointMode) {
        Validate.mutuallyExclusive("Only one of 'endpoint' or 'endpointMode' must be specified, but not both",
                                   builderEndpoint, builderEndpointMode);
        if (builderEndpoint != null) {
            return builderEndpoint;
        }
        if (builderEndpointMode != null) {
            return URI.create(Ec2MetadataEndpointProvider.instance().resolveEndpoint(builderEndpointMode));
        }
        EndpointMode resolvedEndpointMode = Ec2MetadataEndpointProvider.instance().resolveEndpointMode();
        return URI.create(Ec2MetadataEndpointProvider.instance().resolveEndpoint(resolvedEndpointMode));
    }

    protected static String uncheckedInputStreamToUtf8(AbortableInputStream inputStream) {
        try {
            return IoUtils.toUtf8String(inputStream);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        } finally {
            IoUtils.closeQuietly(inputStream, log.logger());
        }
    }

    protected boolean shouldRetry(RetryPolicyContext retryPolicyContext, Throwable error) {
        boolean maxAttemptReached = retryPolicyContext.retriesAttempted() >= retryPolicy.numRetries();
        if (maxAttemptReached) {
            return false;
        }
        return error instanceof RetryableException || error.getCause() instanceof RetryableException;
    }

}
