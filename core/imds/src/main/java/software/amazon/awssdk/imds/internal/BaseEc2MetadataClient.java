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

import static software.amazon.awssdk.imds.internal.Ec2MetadataEndpointProvider.DEFAULT_ENDPOINT_PROVIDER;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.imds.Ec2MetadataClientBuilder;
import software.amazon.awssdk.imds.Ec2MetadataRetryPolicy;
import software.amazon.awssdk.imds.EndpointMode;
import software.amazon.awssdk.imds.TokenCacheStrategy;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.NonBlocking;
import software.amazon.awssdk.utils.cache.OneCallerBlocks;

@SdkInternalApi
public abstract class BaseEc2MetadataClient {

    protected static final Duration DEFAULT_TOKEN_TTL = Duration.of(21_600, ChronoUnit.SECONDS);
    private static final Logger log = Logger.loggerFor(BaseEc2MetadataClient.class);

    protected final Ec2MetadataRetryPolicy retryPolicy;
    protected final URI endpoint;
    protected final RequestMarshaller requestMarshaller;
    protected final Duration tokenTtl;

    protected BaseEc2MetadataClient(Ec2MetadataClientBuilder<?, ?> builder) {
        this.retryPolicy = Validate.getOrDefault(builder.getRetryPolicy(), Ec2MetadataRetryPolicy.builder()::build);
        this.tokenTtl = Validate.getOrDefault(builder.getTokenTtl(), () -> DEFAULT_TOKEN_TTL);
        this.endpoint = getEndpoint(builder);
        this.requestMarshaller = new RequestMarshaller(this.endpoint);
    }

    private URI getEndpoint(Ec2MetadataClientBuilder<?, ?> builder) {
        URI builderEndpoint = builder.getEndpoint();
        EndpointMode builderEndpointMode = builder.getEndpointMode();
        Validate.mutuallyExclusive("Only one of 'endpoint' or 'endpointMode' must be specified, but not both",
                                   builderEndpoint, builderEndpointMode);
        if (builderEndpoint != null) {
            return builderEndpoint;
        }
        if (builderEndpointMode != null) {
            return URI.create(DEFAULT_ENDPOINT_PROVIDER.resolveEndpoint(builderEndpointMode));
        }
        EndpointMode resolvedEndpointMode = DEFAULT_ENDPOINT_PROVIDER.resolveEndpointMode();
        return URI.create(DEFAULT_ENDPOINT_PROVIDER.resolveEndpoint(resolvedEndpointMode));
    }

    protected CachedSupplier.PrefetchStrategy getPrefetchStrategy(TokenCacheStrategy strategy) {
        Validate.notNull(strategy, "TokenCacheStrategy must not be null");
        switch (strategy) {
            case NONE: return null;
            case BLOCKING: return new OneCallerBlocks();
            case NON_BLOCKING: return new NonBlocking("IMDS-TokenCache");
            default:
                throw new IllegalArgumentException(
                    String.format("TokenCacheStrategy '%s' does not have a corresponding PrefetchStrategy", strategy));
        }
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
        boolean maxAttemptReached = retryPolicyContext.retriesAttempted() >= retryPolicy.getNumRetries();
        if (maxAttemptReached) {
            return false;
        }
        return error instanceof RetryableException || error.getCause() instanceof RetryableException;
    }

}
