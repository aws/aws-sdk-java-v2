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
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ToString;

/**
 * A token provider chain that looks for providers in this order:
 * <ol>
 *   <li>A profile based provider that can initialize token providers based on profile configurations</li>
 * </ol>
 *
 * @see ProfileTokenProvider
 */
@SdkPublicApi
public final class DefaultAwsTokenProvider implements SdkTokenProvider, SdkAutoCloseable {

    private static final DefaultAwsTokenProvider DEFAULT_TOKEN_PROVIDER = new DefaultAwsTokenProvider(builder());
    private final LazyTokenProvider providerChain;

    private DefaultAwsTokenProvider(Builder builder) {
        this.providerChain = createChain(builder);
    }

    public static DefaultAwsTokenProvider create() {
        return DEFAULT_TOKEN_PROVIDER;
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

    /**
     * Get a builder for defining a {@link DefaultAwsTokenProvider} with custom configuration.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public SdkToken resolveToken() {
        return providerChain.resolveToken();
    }

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
     * Configuration that defines the {@link DefaultAwsTokenProvider}'s behavior.
     */
    public static final class Builder {
        private Supplier<ProfileFile> profileFile;
        private String profileName;

        /**
         * Created with {@link #builder()}.
         */
        private Builder() {
        }

        public Builder profileFile(Supplier<ProfileFile> profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        /**
         * Create a {@link DefaultAwsTokenProvider} using the configuration defined in this builder.
         */
        public DefaultAwsTokenProvider build() {
            return new DefaultAwsTokenProvider(this);
        }
    }

}
