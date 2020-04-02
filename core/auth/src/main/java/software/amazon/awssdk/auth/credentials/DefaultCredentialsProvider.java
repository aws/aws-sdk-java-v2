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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.internal.LazyAwsCredentialsProvider;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ToString;

/**
 * AWS credentials provider chain that looks for credentials in this order:
 * <ol>
 *   <li>Java System Properties - <code>aws.accessKeyId</code> and <code>aws.secretKey</code></li>
 *   <li>Environment Variables - <code>AWS_ACCESS_KEY_ID</code> and <code>AWS_SECRET_ACCESS_KEY</code></li>
 *   <li>Credential profiles file at the default location (~/.aws/credentials) shared by all AWS SDKs and the AWS CLI</li>
 *   <li>Credentials delivered through the Amazon EC2 container service if AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" environment
 *   variable is set and security manager has permission to access the variable,</li>
 *   <li>Instance profile credentials delivered through the Amazon EC2 metadata service</li>
 * </ol>
 *
 * @see SystemPropertyCredentialsProvider
 * @see EnvironmentVariableCredentialsProvider
 * @see ProfileCredentialsProvider
 * @see WebIdentityTokenFileCredentialsProvider
 * @see ContainerCredentialsProvider
 * @see InstanceProfileCredentialsProvider
 */
@SdkPublicApi
public final class DefaultCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {

    private static final DefaultCredentialsProvider DEFAULT_CREDENTIALS_PROVIDER = new DefaultCredentialsProvider(builder());

    private final LazyAwsCredentialsProvider providerChain;

    /**
     * @see #builder()
     */
    private DefaultCredentialsProvider(Builder builder) {
        this.providerChain = createChain(builder);
    }

    /**
     * Create an create of the {@link DefaultCredentialsProvider} using the default configuration. Configuration can be
     * specified by creating an create using the {@link #builder()}.
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
            AwsCredentialsProvider[] credentialsProviders = new AwsCredentialsProvider[] {
                    SystemPropertyCredentialsProvider.create(),
                    EnvironmentVariableCredentialsProvider.create(),
                    WebIdentityTokenFileCredentialsProvider.create(),
                    ProfileCredentialsProvider.builder()
                                              .profileFile(builder.profileFile)
                                              .profileName(builder.profileName)
                                              .build(),
                    ContainerCredentialsProvider.builder()
                                                .asyncCredentialUpdateEnabled(asyncCredentialUpdateEnabled)
                                                .build(),
                    InstanceProfileCredentialsProvider.builder()
                                                      .asyncCredentialUpdateEnabled(asyncCredentialUpdateEnabled)
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

    /**
     * Configuration that defines the {@link DefaultCredentialsProvider}'s behavior.
     */
    public static final class Builder {
        private ProfileFile profileFile;
        private String profileName;
        private Boolean reuseLastProviderEnabled = true;
        private Boolean asyncCredentialUpdateEnabled = false;

        /**
         * Created with {@link #builder()}.
         */
        private Builder() {
        }

        public Builder profileFile(ProfileFile profileFile) {
            this.profileFile = profileFile;
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
        public DefaultCredentialsProvider build() {
            return new DefaultCredentialsProvider(this);
        }
    }
}
