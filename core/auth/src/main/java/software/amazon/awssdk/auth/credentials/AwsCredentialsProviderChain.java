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

package software.amazon.awssdk.auth.credentials;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An {@link IdentityProvider}{@code <}{@link AwsCredentialsIdentity}{@code >} that chains together multiple other
 * identity providers. This is useful when the same application can be configured using different credential sources,
 * depending on the environment the application is deployed to.
 * <p>
 * When a caller first requests credentials from this provider, it calls each provider in the chain, in the original order
 * specified, until one can provide credentials, and then returns those credentials. If every credential provider in the
 * chain has been called, and none of them can provide credentials, then this class will throw an exception indicating that no
 * credentials are available.
 * <p>
 * By default, this class will remember the first credentials provider in the chain that was able to provide credentials, and
 * will continue to use that provider when credentials are requested in the future, instead of traversing the chain each time.
 * This behavior can be controlled through the {@link Builder#reuseLastProviderEnabled(Boolean)} method.
 * <p>
 * This chain implements {@link AutoCloseable}. When closed, it will call the {@link AutoCloseable#close()} on any credential
 * providers in the chain that need to be closed.
 * <p>
 * Create using {@link AwsCredentialsProviderChain#builder()} or {@link AwsCredentialsProviderChain#of}:
 * {@snippet :
 * AwsCredentialsProviderChain credentialsProvider =
 *     AwsCredentialsProviderChain.builder()
 *                                .addCredentialsProvider(SystemPropertyCredentialsProvider.create())
 *                                .addCredentialsProvider(EnvironmentVariableCredentialsProvider.create())
 *                                .build();
 *
 * // or
 *
 * AwsCredentialsProviderChain credentialsProvider =
 *     AwsCredentialsProviderChain.of(SystemPropertyCredentialsProvider.create(),
 *                                    EnvironmentVariableCredentialsProvider.create());
 *
 * S3Client s3 = S3Client.builder()
 *                       .credentialsProvider(credentialsProvider)
 *                       .build();
 * }
 */
@SdkPublicApi
public final class AwsCredentialsProviderChain
    implements AwsCredentialsProvider,
               SdkAutoCloseable,
               ToCopyableBuilder<AwsCredentialsProviderChain.Builder, AwsCredentialsProviderChain> {
    private static final Logger log = Logger.loggerFor(AwsCredentialsProviderChain.class);

    private final List<IdentityProvider<? extends AwsCredentialsIdentity>> credentialsProviders;

    private final boolean reuseLastProviderEnabled;

    private volatile IdentityProvider<? extends AwsCredentialsIdentity> lastUsedProvider;

    /**
     * @see #builder()
     */
    private AwsCredentialsProviderChain(BuilderImpl builder) {
        Validate.notEmpty(builder.credentialsProviders, "No credential providers were specified.");
        this.reuseLastProviderEnabled = builder.reuseLastProviderEnabled;
        this.credentialsProviders = Collections.unmodifiableList(builder.credentialsProviders);
    }

    /**
     * Get a new builder for creating a {@link AwsCredentialsProviderChain}.
     */
    public static Builder builder() {
        return new BuilderImpl();
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

    /**
     * Create an AWS credentials provider chain with default configuration that checks the given credential providers.
     * @param awsCredentialsProviders The credentials providers that should be checked for credentials, in the order they should
     *                                be checked.
     * @return A credential provider chain that checks the provided credential providers in order.
     */
    public static AwsCredentialsProviderChain of(IdentityProvider<? extends AwsCredentialsIdentity>... awsCredentialsProviders) {
        return builder().credentialsProviders(awsCredentialsProviders).build();
    }

    @Override
    public AwsCredentials resolveCredentials() {
        if (reuseLastProviderEnabled && lastUsedProvider != null) {
            return CredentialUtils.toCredentials(CompletableFutureUtils.joinLikeSync(lastUsedProvider.resolveIdentity()));
        }

        List<String> exceptionMessages = null;
        for (IdentityProvider<? extends AwsCredentialsIdentity> provider : credentialsProviders) {
            try {
                AwsCredentialsIdentity credentials = CompletableFutureUtils.joinLikeSync(provider.resolveIdentity());

                log.debug(() -> "Loading credentials from " + provider);

                lastUsedProvider = provider;
                return CredentialUtils.toCredentials(credentials);
            } catch (RuntimeException e) {
                // Ignore any exceptions and move onto the next provider
                String message = provider + ": " + e.getMessage();
                log.debug(() -> "Unable to load credentials from " + message , e);

                if (exceptionMessages == null) {
                    exceptionMessages = new ArrayList<>();
                }
                exceptionMessages.add(message);
            }
        }

        throw SdkClientException.builder()
                                .message("Unable to load credentials from any of the providers in the chain " +
                                         this + " : " + exceptionMessages)
                                .build();
    }

    @Override
    public void close() {
        credentialsProviders.forEach(c -> IoUtils.closeIfCloseable(c, null));
    }

    @Override
    public String toString() {
        return ToString.builder("AwsCredentialsProviderChain")
                       .add("credentialsProviders", credentialsProviders)
                       .build();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /**
     * A builder for a {@link AwsCredentialsProviderChain} that allows controlling its behavior.
     */
    public interface Builder extends CopyableBuilder<Builder, AwsCredentialsProviderChain> {

        /**
         * Controls whether the chain should reuse the last successful credentials provider in the chain. Reusing the last
         * successful credentials provider will typically return credentials faster than searching through the chain.
         *
         * <p>
         * By default, this is enabled
         */
        Builder reuseLastProviderEnabled(Boolean reuseLastProviderEnabled);

        /**
         * Configure the credentials providers that should be checked for credentials, in the order they should be checked.
         */
        Builder credentialsProviders(Collection<? extends AwsCredentialsProvider> credentialsProviders);

        /**
         * Configure the credentials providers that should be checked for credentials, in the order they should be checked.
         */
        Builder credentialsIdentityProviders(
            Collection<? extends IdentityProvider<? extends AwsCredentialsIdentity>> credentialsProviders);

        /**
         * Configure the credentials providers that should be checked for credentials, in the order they should be checked.
         */
        default Builder credentialsProviders(AwsCredentialsProvider... credentialsProviders) {
            return credentialsProviders((IdentityProvider<? extends AwsCredentialsIdentity>[]) credentialsProviders);
        }

        /**
         * Configure the credentials providers that should be checked for credentials, in the order they should be checked.
         */
        default Builder credentialsProviders(IdentityProvider<? extends AwsCredentialsIdentity>... credentialsProviders) {
            throw new UnsupportedOperationException();
        }

        /**
         * Add a credential provider to the chain, after the credential providers that have already been configured.
         */
        default Builder addCredentialsProvider(AwsCredentialsProvider credentialsProvider) {
            return addCredentialsProvider((IdentityProvider<? extends AwsCredentialsIdentity>) credentialsProvider);
        }

        /**
         * Add a credential provider to the chain, after the credential providers that have already been configured.
         */
        default Builder addCredentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
            throw new UnsupportedOperationException();
        }

        AwsCredentialsProviderChain build();
    }

    private static final class BuilderImpl implements Builder {
        private Boolean reuseLastProviderEnabled = true;
        private List<IdentityProvider<? extends AwsCredentialsIdentity>> credentialsProviders = new ArrayList<>();

        private BuilderImpl() {
        }

        private BuilderImpl(AwsCredentialsProviderChain provider) {
            this.reuseLastProviderEnabled = provider.reuseLastProviderEnabled;
            this.credentialsProviders = provider.credentialsProviders;
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
        public Builder credentialsProviders(Collection<? extends AwsCredentialsProvider> credentialsProviders) {
            this.credentialsProviders = new ArrayList<>(credentialsProviders);
            return this;
        }

        public void setCredentialsProviders(Collection<? extends AwsCredentialsProvider> credentialsProviders) {
            credentialsProviders(credentialsProviders);
        }

        @Override
        public Builder credentialsIdentityProviders(
                Collection<? extends IdentityProvider<? extends AwsCredentialsIdentity>> credentialsProviders) {
            this.credentialsProviders = new ArrayList<>(credentialsProviders);
            return this;
        }

        public void setCredentialsIdentityProviders(
                Collection<? extends IdentityProvider<? extends AwsCredentialsIdentity>> credentialsProviders) {
            credentialsIdentityProviders(credentialsProviders);
        }

        @Override
        public Builder credentialsProviders(IdentityProvider<? extends AwsCredentialsIdentity>... credentialsProviders) {
            return credentialsIdentityProviders(Arrays.asList(credentialsProviders));
        }

        @Override
        public Builder addCredentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
            this.credentialsProviders.add(credentialsProvider);
            return this;
        }

        @Override
        public AwsCredentialsProviderChain build() {
            return new AwsCredentialsProviderChain(this);
        }
    }
}
