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

import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.internal.LazyAwsCredentialsProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSupplier;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * {@link IdentityProvider}{@code <}{@link AwsCredentialsIdentity}{@code >} that is used by default by AWS SDK for Java
 * clients. If a client is not configured with a credential provider, this one is used.
 * <p>
 * This provider looks for credentials in this order:
 * <ol>
 *   <li>Java System Properties: Uses AWS credentials configured using Java system properties.
 *       See {@link SystemPropertyCredentialsProvider} for more information.</li>
 *   <li>Environment Variables: Uses AWS credentials configured using environment variables.
 *       See {@link EnvironmentVariableCredentialsProvider} for more information.</li>
 *   <li>Web Identity Token File: Uses AWS credentials retrieved from
 *       <a href="https://docs.aws.amazon.com/STS/latest/APIReference/API_AssumeRoleWithWebIdentity.html">AWS STS's
 *       {@code AssumeRoleWithWebIdentity} operation</a>.
 *       See {@link WebIdentityTokenFileCredentialsProvider} for more information.</li>
 *   <li>Profile File Credentials: Uses credentials from the {@code ~/.aws/config} and {@code ~/.aws/credentials}.
 *       See {@link ProfileCredentialsProvider} for more information.</li>
 *   <li>Container Credentials: Uses credentials from your Amazon ECS or EKS configuration.
 *       See {@link ContainerCredentialsProvider} for more information.</li>
 *   <li>Instance Profile Credentials: Uses credentials from your Amazon EC2 configuration.
 *       See {@link InstanceProfileCredentialsProvider} for more information.</li>
 * </ol>
 * See our <a href="https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html">default
 * credentials provider chain documentation</a> for more information.
 *
 * <p>
 * Some credential providers in this chain will make service calls to retrieve credentials. These providers will cache the
 * credential result, and will only invoke the metadata service periodically to keep the credential "fresh". As a result, it is
 * recommended that you create a single credentials provider of this type and reuse it throughout your application. You may
 * notice small latency increases on requests that refresh the cached credentials. To avoid this latency increase, you can
 * enable async refreshing with {@link Builder#asyncCredentialUpdateEnabled(Boolean)}. If you enable this setting, you must
 * {@link #close()} this credential provider if you are done using it, to disable the background refreshing task. If you fail
 * to do this, your application could run out of resources.
 *
 * <p>
 * This can be created using {@link DefaultCredentialsProvider#create()} or {@link DefaultCredentialsProvider#builder()}:
 * {@snippet :
 * DefaultCredentialsProvider credentialsProvider =
 *     DefaultCredentialsProvider.create();
 *
 * // or
 *
 * DefaultCredentialsProvider credentialsProvider =
 *     DefaultCredentialsProvider.builder()
 *                               .profileName("non-default-profile")
 *                               .build();
 *
 * S3Client s3 = S3Client.builder()
 *                       .credentialsProvider(credentialsProvider)
 *                       .build();
 * }
 */
@SdkPublicApi
public final class DefaultCredentialsProvider
    implements AwsCredentialsProvider, SdkAutoCloseable,
               ToCopyableBuilder<DefaultCredentialsProvider.Builder, DefaultCredentialsProvider> {

    private static final DefaultCredentialsProvider DEFAULT_CREDENTIALS_PROVIDER = new DefaultCredentialsProvider(builder());

    private final LazyAwsCredentialsProvider providerChain;

    private final Supplier<ProfileFile> profileFile;

    private final String profileName;

    private final Boolean reuseLastProviderEnabled;

    private final Boolean asyncCredentialUpdateEnabled;

    /**
     * @see #builder()
     */
    private DefaultCredentialsProvider(Builder builder) {
        this.profileFile = builder.profileFile;
        this.profileName = builder.profileName;
        this.reuseLastProviderEnabled = builder.reuseLastProviderEnabled;
        this.asyncCredentialUpdateEnabled = builder.asyncCredentialUpdateEnabled;
        this.providerChain = createChain(builder);
    }

    /**
     * Returns the singleton instance of the {@link DefaultCredentialsProvider} using the default configuration. 
     * Configuration can be specified by creating an instance using the {@link #builder()}. If you want to 
     * create a new instance, use {@link #builder()} instead.
     */
    public static DefaultCredentialsProvider create() {
        return DEFAULT_CREDENTIALS_PROVIDER;
    }

    /**
     * Create the default credential chain using the configuration in the provided builder.
     */
    private static LazyAwsCredentialsProvider createChain(Builder builder) {
        boolean asyncCredentialUpdateEnabled = builder.asyncCredentialUpdateEnabled;
        boolean reuseLastProviderEnabled = builder.reuseLastProviderEnabled;

        return LazyAwsCredentialsProvider.create(() -> {
            AwsCredentialsProvider[] credentialsProviders = {
                SystemPropertyCredentialsProvider.create(),
                EnvironmentVariableCredentialsProvider.create(),
                WebIdentityTokenFileCredentialsProvider.builder()
                                                       .asyncCredentialUpdateEnabled(asyncCredentialUpdateEnabled)
                                                       .build(),
                ProfileCredentialsProvider.builder()
                                          .profileFile(builder.profileFile)
                                          .profileName(builder.profileName)
                                          .build(),
                ContainerCredentialsProvider.builder()
                                            .asyncCredentialUpdateEnabled(asyncCredentialUpdateEnabled)
                                            .build(),
                InstanceProfileCredentialsProvider.builder()
                                                  .asyncCredentialUpdateEnabled(asyncCredentialUpdateEnabled)
                                                  .profileFile(builder.profileFile)
                                                  .profileName(builder.profileName)
                                                  .build()
            };

            return AwsCredentialsProviderChain.builder()
                                              .reuseLastProviderEnabled(reuseLastProviderEnabled)
                                              .credentialsProviders(credentialsProviders)
                                              .build();
        });
    }

    /**
     * Get a builder for defining a {@link DefaultCredentialsProvider} with custom configuration.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public AwsCredentials resolveCredentials() {
        return providerChain.resolveCredentials();
    }

    @Override
    public void close() {
        providerChain.close();
    }

    @Override
    public String toString() {
        return ToString.builder("DefaultCredentialsProvider")
                       .add("providerChain", providerChain)
                       .build();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Configuration that defines the {@link DefaultCredentialsProvider}'s behavior.
     */
    public static final class Builder implements CopyableBuilder<Builder, DefaultCredentialsProvider> {
        private Supplier<ProfileFile> profileFile;
        private String profileName;
        private Boolean reuseLastProviderEnabled = true;
        private Boolean asyncCredentialUpdateEnabled = false;

        /**
         * Created with {@link #builder()}.
         */
        private Builder() {
        }

        private Builder(DefaultCredentialsProvider credentialsProvider) {
            this.profileFile = credentialsProvider.profileFile;
            this.profileName = credentialsProvider.profileName;
            this.reuseLastProviderEnabled = credentialsProvider.reuseLastProviderEnabled;
            this.asyncCredentialUpdateEnabled = credentialsProvider.asyncCredentialUpdateEnabled;
        }

        public Builder profileFile(ProfileFile profileFile) {
            return profileFile(Optional.ofNullable(profileFile)
                                       .map(ProfileFileSupplier::fixedProfileFile)
                                       .orElse(null));
        }

        public Builder profileFile(Supplier<ProfileFile> profileFileSupplier) {
            this.profileFile = profileFileSupplier;
            return this;
        }

        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        /**
         * Controls whether the provider should reuse the last successful credentials provider in the chain. Reusing the last
         * successful credentials provider will typically return credentials faster than searching through the chain.
         *
         * <p>By default, this is enabled.</p>
         */
        public Builder reuseLastProviderEnabled(Boolean reuseLastProviderEnabled) {
            this.reuseLastProviderEnabled = reuseLastProviderEnabled;
            return this;
        }

        /**
         * Configure whether this provider should fetch credentials asynchronously in the background. If this is true, threads are
         * less likely to block when {@link #resolveCredentials()} is called, but additional resources are used to maintain the
         * provider.
         *
         * <p>By default, this is disabled.</p>
         */
        public Builder asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled) {
            this.asyncCredentialUpdateEnabled = asyncCredentialUpdateEnabled;
            return this;
        }

        /**
         * Create a {@link DefaultCredentialsProvider} using the configuration defined in this builder.
         */
        @Override
        public DefaultCredentialsProvider build() {
            return new DefaultCredentialsProvider(this);
        }
    }
}
