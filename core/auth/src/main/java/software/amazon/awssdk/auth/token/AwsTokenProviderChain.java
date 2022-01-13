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

package software.amazon.awssdk.auth.token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * An {@link AwsTokenProvider} implementation that chains together multiple token providers.
 *
 * <p>When a caller first requests token from this provider, it calls all the providers in the chain, in the original order
 * specified, until one can provide a token, and then returns that token. If all of the token providers in the
 * chain have been called, and none of them can provide token, then this class will throw an exception indicated that no
 * token is available.</p>
 *
 * <p>By default, this class will remember the first token provider in the chain that was able to provide tokens, and
 * will continue to use that provider when token is requested in the future, instead of traversing the chain each time.
 * This behavior can be controlled through the {@link Builder#reuseLastProviderEnabled(Boolean)} method.</p>
 *
 * <p>This chain implements {@link AutoCloseable}. When closed, it will call the {@link AutoCloseable#close()} on any token
 * providers in the chain that need to be closed.</p>
 */
@SdkPublicApi
public final class AwsTokenProviderChain implements AwsTokenProvider, SdkAutoCloseable {
    private static final Logger log = Logger.loggerFor(AwsTokenProviderChain.class);

    private final List<AwsTokenProvider> awsTokenProviders;

    private final boolean reuseLastProviderEnabled;

    private volatile AwsTokenProvider lastUsedProvider;

    /**
     * @see #builder()
     */
    private AwsTokenProviderChain(BuilderImpl builder) {
        Validate.notEmpty(builder.tokenProviders, "No token providers were specified.");
        this.reuseLastProviderEnabled = builder.reuseLastProviderEnabled;
        this.awsTokenProviders = Collections.unmodifiableList(builder.tokenProviders);
    }

    /**
     * Get a new builder for creating a {@link AwsTokenProviderChain}.
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * Create an AWS token provider chain with default configuration that checks the given token providers.
     * @param awsTokenProviders The token providers that should be checked for token, in the order they should
     *                                be checked.
     * @return A token provider chain that checks the provided token providers in order.
     */
    public static AwsTokenProviderChain of(AwsTokenProvider... awsTokenProviders) {
        return builder().tokenProviders(awsTokenProviders).build();
    }

    @Override
    public AwsToken resolveToken() {
        if (reuseLastProviderEnabled && lastUsedProvider != null) {
            return lastUsedProvider.resolveToken();
        }

        List<String> exceptionMessages = null;
        for (AwsTokenProvider provider : awsTokenProviders) {
            try {
                AwsToken token = provider.resolveToken();

                log.debug(() -> "Loading token from " + provider);

                lastUsedProvider = provider;
                return token;
            } catch (RuntimeException e) {
                // Ignore any exceptions and move onto the next provider
                String message = provider + ": " + e.getMessage();
                log.debug(() -> "Unable to load token from " + message , e);

                if (exceptionMessages == null) {
                    exceptionMessages = new ArrayList<>();
                }
                exceptionMessages.add(message);
            }
        }

        throw SdkClientException.builder()
                                .message("Unable to load token from any of the providers in the chain " +
                                         this + " : " + exceptionMessages)
                                .build();
    }

    @Override
    public void close() {
        awsTokenProviders.forEach(c -> IoUtils.closeIfCloseable(c, null));
    }

    @Override
    public String toString() {
        return ToString.builder("AwsTokenProviderChain")
                       .add("tokenProviders", awsTokenProviders)
                       .build();
    }

    /**
     * A builder for a {@link AwsTokenProviderChain} that allows controlling its behavior.
     */
    public interface Builder {

        /**
         * Controls whether the chain should reuse the last successful token provider in the chain. Reusing the last
         * successful token provider will typically return token faster than searching through the chain.
         *
         * <p>
         * By default, this is enabled
         */
        Builder reuseLastProviderEnabled(Boolean reuseLastProviderEnabled);

        /**
         * Configure the token providers that should be checked for token, in the order they should be checked.
         */
        Builder tokenProviders(Collection<? extends AwsTokenProvider> tokenProviders);

        /**
         * Configure the token providers that should be checked for token, in the order they should be checked.
         */
        Builder tokenProviders(AwsTokenProvider... tokenProviders);

        /**
         * Add a token provider to the chain, after the token providers that have already been configured.
         */
        Builder addTokenProvider(AwsTokenProvider tokenProviders);

        AwsTokenProviderChain build();
    }

    private static final class BuilderImpl implements Builder {
        private Boolean reuseLastProviderEnabled = true;
        private List<AwsTokenProvider> tokenProviders = new ArrayList<>();

        private BuilderImpl() {
        }

        @Override
        public Builder reuseLastProviderEnabled(Boolean reuseLastProviderEnabled) {
            this.reuseLastProviderEnabled = reuseLastProviderEnabled;
            return this;
        }

        public void setReuseLastProviderEnabled(Boolean reuseLastProviderEnabled) {
            reuseLastProviderEnabled(reuseLastProviderEnabled);
        }

        @Override
        public Builder tokenProviders(Collection<? extends AwsTokenProvider> tokenProviders) {
            this.tokenProviders = new ArrayList<>(tokenProviders);
            return this;
        }

        public void setTokenProviders(Collection<? extends AwsTokenProvider> tokenProviders) {
            tokenProviders(tokenProviders);
        }

        public Builder tokenProviders(AwsTokenProvider... tokenProviders) {
            return tokenProviders(Arrays.asList(tokenProviders));
        }

        @Override
        public Builder addTokenProvider(AwsTokenProvider tokenProviders) {
            this.tokenProviders.add(tokenProviders);
            return this;
        }

        @Override
        public AwsTokenProviderChain build() {
            return new AwsTokenProviderChain(this);
        }
    }
}
