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

package software.amazon.awssdk.auth.token.credentials.aws;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.token.credentials.ProfileTokenProvider;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProviderChain;
import software.amazon.awssdk.auth.token.internal.LazyTokenProvider;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSupplier;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ToString;

/**
 * An {@link IdentityProvider}{@code <}{@link TokenIdentity}{@code >} that is used by default in the AWS SDK for Java
 * for clients that accept bearer tokens. If such a client is not configured with a token provider, this one is used.
 * <p>
 * This provider looks for tokens in this order:
 * <ol>
 *   <li>Profile Tokens: Uses a token from the {@code ~/.aws/config} and {@code ~/.aws/credentials}. See
 *   {@link ProfileTokenProvider} for more information.</li>
 * </ol>
 *
 * <p>
 * Some token providers in this chain will make service calls to retrieve a token. These providers will cache the
 * token result, and will only invoke the service periodically to keep the token "fresh". As a result, it is
 * recommended that you create a single token provider of this type and reuse it throughout your application. You may
 * notice small latency increases on requests that refresh the cached token.
 *
 * <p>
 * You should {@link #close()} this token provider if you are done using it, because some configurations can cause the
 * creation of resources that cannot be garbage collected.
 *
 * <p>
 * This can be created using {@link #create()} or {@link #builder()}:
 * {@snippet :
 * DefaultAwsTokenProvider tokenProvider =
 *     DefaultAwsTokenProvider.create(); // @link substring="create" target="#create()"
 *
 * // or
 *
 * DefaultAwsTokenProvider tokenProvider =
 *     DefaultAwsTokenProvider.builder() // @link substring="builder" target="#builder()"
 *                            .profileName("custom-profile-name")
 *                            .build();
 *
 * ServiceClient s3 = ServiceClient.builder()
 *                                 .tokenProvider(tokenProvider)
 *                                 .build();
 * }
 */
@SdkPublicApi
public final class DefaultAwsTokenProvider implements SdkTokenProvider, SdkAutoCloseable {

    private static final DefaultAwsTokenProvider DEFAULT_TOKEN_PROVIDER = new DefaultAwsTokenProvider(builder());
    private final LazyTokenProvider providerChain;

    private DefaultAwsTokenProvider(Builder builder) {
        this.providerChain = createChain(builder);
    }

    /**
     * Retrieve the default {@link DefaultAwsTokenProvider} instance.
     * <p>
     * {@snippet :
     * DefaultAwsTokenProvider tokenProvider = DefaultAwsTokenProvider.create();
     * }
     * <p>
     * If a new instance of this class is desired, it can be created using a {@link #builder()}.
     */
    public static DefaultAwsTokenProvider create() {
        return DEFAULT_TOKEN_PROVIDER;
    }

    /**
     * Get a new builder for creating a {@link DefaultAwsTokenProvider}.
     * <p>
     * {@snippet :
     * DefaultAwsTokenProvider tokenProvider =
     *     DefaultAwsTokenProvider.builder()
     *                            .profileName("non-default-profile")
     *                            .build();
     * }
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create the default token provider chain using the configuration in the provided builder.
     */
    private static LazyTokenProvider createChain(Builder builder) {
        return LazyTokenProvider.create(
            () -> SdkTokenProviderChain.of(ProfileTokenProvider.builder()
                                                               .profileFile(builder.profileFile)
                                                               .profileName(builder.profileName)
                                                               .build()));
    }

    @Override
    public SdkToken resolveToken() {
        return providerChain.resolveToken();
    }

    /**
     * Release resources held by this token provider. This should be called when you're done using the token
     * provider, because some delegate providers hold resources (e.g. clients) that must be released.
     */
    @Override
    public void close() {
        providerChain.close();
    }

    @Override
    public String toString() {
        return ToString.builder("DefaultAwsTokenProvider")
                       .add("providerChain", providerChain)
                       .build();
    }

    /**
     * See {@link DefaultAwsTokenProvider} for detailed documentation.
     */
    public static final class Builder {
        private Supplier<ProfileFile> profileFile;
        private String profileName;

        private Builder() {
        }

        /**
         * Define the {@link ProfileFile} that should be used by delegate token providers that rely on the profile file.
         *
         * <p>
         * The profile file is only read when the {@link ProfileFile} object is created, so the token provider will not
         * reflect any changes made in the provided file. To automatically adjust to changes in the file, see
         * {@link #profileFile(Supplier)}.
         *
         * <p>
         * If not specified, the {@link ProfileFile#defaultProfileFile()} will be used.
         *
         * <p>
         * {@snippet :
         * DefaultAwsTokenProvider.builder()
         *                        .profileFile(ProfileFile.builder()
         *                                                .type(ProfileFile.Type.CONFIGURATION)
         *                                                .content(Paths.get("~/.aws/config"))
         *                                                .build())
         *                        .build()
         *}
         *
         * @see ProfileFile
         */
        public Builder profileFile(ProfileFile profileFile) {
            this.profileFile = () -> profileFile;
            return this;
        }

        /**
         * Define a {@link ProfileFileSupplier} that should be used by delegate token providers that rely on the profile file.
         *
         * <p>
         * The profile file supplier is called each time the {@link ProfileFile} is read, so the token provider can
         * "pick up" changes made in the provided file.
         *
         * <p>
         * If not specified, the (fixed) {@link ProfileFile#defaultProfileFile()} will be used.
         *
         * <p>
         * {@snippet :
         * DefaultAwsTokenProvider.builder()
         *                        .profileFile(ProfileFileSupplier.defaultSupplier())
         *                        .build()
         *}
         *
         * @see ProfileFileSupplier
         */
        public Builder profileFile(Supplier<ProfileFile> profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        /**
         * Define the name of the profile that should be used by delegate token providers that rely on the profile file.
         *
         * <p>
         * If this profile does not exist in the {@link ProfileFile}, token resolution in delegate providers that use the
         * profile file will fail.
         *
         * <p>
         * If not specified, the {@code aws.profile} system property or {@code AWS_PROFILE} environment variable's value will
         * be used. If these are not set, then {@code default} will be used.
         *
         * <p>
         * {@snippet :
         * DefaultAwsTokenProvider.builder()
         *                        .profileName("custom-profile-name")
         *                        .build()
         *}
         */
        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        /**
         * Build the {@link DefaultAwsTokenProvider}.
         *
         * <p>
         * {@snippet :
         * DefaultAwsTokenProvider tokenProvider =
         *     DefaultAwsTokenProvider.builder()
         *                            .profileName("custom-profile-name")
         *                            .build();
         * }
         */
        public DefaultAwsTokenProvider build() {
            return new DefaultAwsTokenProvider(this);
        }
    }

}
