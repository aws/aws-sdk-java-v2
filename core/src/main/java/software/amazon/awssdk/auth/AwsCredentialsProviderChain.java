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

package software.amazon.awssdk.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.Validate;

/**
 * {@link AwsCredentialsProvider} implementation that chains together multiple credentials providers.
 *
 * <p>When a caller first requests credentials from this provider, it calls all the providers in the chain, in the original order
 * specified, until one can provide credentials, and then returns those credentials. If all of the credential providers in the
 * chain have been called, and none of them can provide credentials, then this class will throw an exception indicated that no
 * credentials are available.</p>
 *
 * <p>By default, this class will remember the first credentials provider in the chain that was able to provide credentials, and
 * will continue to use that provider when credentials are requested in the future, instead of traversing the chain each time.
 * This behavior can be controlled through the {@link Builder#reuseLastProviderEnabled(Boolean)} method.</p>
 *
 * <p>This chain implements {@link AutoCloseable}. When closed, it will call the {@link AutoCloseable#close()} on any credential
 * providers in the chain that need to be closed.</p>
 */
public final class AwsCredentialsProviderChain implements AwsCredentialsProvider, SdkAutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(AwsCredentialsProviderChain.class);

    private final List<AwsCredentialsProvider> credentialsProviders;

    private final boolean reuseLastProviderEnabled;

    private volatile AwsCredentialsProvider lastUsedProvider;

    /**
     * @see #builder()
     */
    private AwsCredentialsProviderChain(Builder builder) {
        this.reuseLastProviderEnabled = builder.reuseLastProviderEnabled;
        this.credentialsProviders = Collections.unmodifiableList(
                Validate.notEmpty(builder.credentialsProviders, "No credential providers were specified."));
    }

    /**
     * Get a new builder for creating a {@link AwsCredentialsProviderChain}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create an AWS credentials provider chain with default configuration that checks the given credential providers.
     * @param awsCredentialsProviders The credentials providers that should be checked for credentials, in the order they should
     *                                be checked.
     * @return A credential provider chain that checks the provided credential providers in order.
     */
    public static AwsCredentialsProviderChain of(AwsCredentialsProvider... awsCredentialsProviders) {
        return builder().credentialsProviders(awsCredentialsProviders).build();
    }

    @Override
    public AwsCredentials getCredentials() {
        if (reuseLastProviderEnabled && lastUsedProvider != null) {
            return lastUsedProvider.getCredentials();
        }

        for (AwsCredentialsProvider provider : credentialsProviders) {
            try {
                AwsCredentials credentials = provider.getCredentials();

                log.debug("Loading credentials from {}", provider.toString());

                lastUsedProvider = provider;
                return credentials;
            } catch (RuntimeException e) {
                // Ignore any exceptions and move onto the next provider
                log.debug("Unable to load credentials from {}:{}", provider.toString(), e.getMessage(), e);
            }
        }

        throw new SdkClientException("Unable to load credentials from any of the providers in the chain: " + this);
    }

    @Override
    public void close() {
        credentialsProviders.forEach(c -> IoUtils.closeIfCloseable(c, null));
    }

    @Override
    public String toString() {
        String credentialProviders = credentialsProviders.stream().map(Object::toString).collect(Collectors.joining(", "));
        return getClass().getSimpleName() + "(" + credentialProviders + ")";
    }

    /**
     * A builder for a {@link AwsCredentialsProviderChain} that allows controlling its behavior.
     */
    public static class Builder {
        private Boolean reuseLastProviderEnabled = true;
        private List<AwsCredentialsProvider> credentialsProviders = new ArrayList<>();

        /**
         * Created with {@link #builder()}.
         */
        private Builder() {}

        /**
         * Controls whether the chain should reuse the last successful credentials provider in the chain. Reusing the last
         * successful credentials provider will typically return credentials faster than searching through the chain.
         *
         * <p>By default, this is enabled.</p>
         */
        public Builder reuseLastProviderEnabled(Boolean reuseLastProviderEnabled) {
            this.reuseLastProviderEnabled = reuseLastProviderEnabled;
            return this;
        }

        /**
         * Configure the credentials providers that should be checked for credentials, in the order they should be checked.
         */
        public Builder credentialsProviders(Collection<? extends AwsCredentialsProvider> credentialsProviders) {
            this.credentialsProviders = new ArrayList<>(credentialsProviders);
            return this;
        }

        /**
         * Configure the credentials providers that should be checked for credentials, in the order they should be checked.
         */
        public Builder credentialsProviders(AwsCredentialsProvider... credentialsProviders) {
            return credentialsProviders(Arrays.asList(credentialsProviders));
        }

        /**
         * Add a credential provider to the chain, after the credential providers that have already been configured.
         */
        public Builder addCredentialsProvider(AwsCredentialsProvider credentialsProviders) {
            this.credentialsProviders.add(credentialsProviders);
            return this;
        }

        /**
         * Constructs a new AWSCredentialsProviderChain with the specified credential providers. When credentials are requested
         * from this provider, it will call each of these credential providers in the same order specified here until one of them
         * returns AWS security credentials.
         */
        public AwsCredentialsProviderChain build() {
            return new AwsCredentialsProviderChain(this);
        }
    }
}
