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

package software.amazon.awssdk.auth.token.credentials;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.TokenUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * An {@link IdentityProvider}{@code <}{@link TokenIdentity}{@code >} that chains together multiple other
 * identity providers. This is useful when the same application can be configured using different token sources,
 * depending on the environment the application is deployed to.
 * <p>
 * When a caller first requests tokens from this provider, it calls each provider in the chain, in the original order
 * specified, until one can provide a token, and then returns that token. If every token provider in the
 * chain has been called, and none of them can provide a token, then this class will throw an exception indicating that no
 * tokens are available.
 * <p>
 * By default, this class will remember the first token provider in the chain that was able to provide a token, and
 * will continue to use that provider when tokens are requested in the future, instead of traversing the chain each time.
 * This behavior can be controlled through the {@link Builder#reuseLastProviderEnabled(Boolean)} method.
 * <p>
 * This chain implements {@link AutoCloseable}. When closed, it will call the {@link AutoCloseable#close()} on any token
 * providers in the chain that need to be closed.
 * <p>
 * Create using {@link #builder()} or {@link #of}:
 * {@snippet :
 * SdkTokenProviderChain tokenProvider =
 *     SdkTokenProviderChain.builder() // @link substring="builder" target="#builder()"
 *                          .addTokenProvider(ProfileTokenProvider.create())
 *                          .addTokenProvider(IdentityProvider.staticToken("bearer-token"))
 *                          .build();
 *
 * // or
 *
 * SdkTokenProviderChain tokenProvider =
 *     SdkTokenProviderChain.of(ProfileTokenProvider.create(), // @link regex="\bof\b" target="#of"
 *                              IdentityProvider.staticToken("bearer-token"));
 *
 * ServiceClient service = ServiceClient.builder()
 *                                      .tokenProvider(tokenProvider)
 *                                      .build();
 * }
 */
@SdkPublicApi
public final class SdkTokenProviderChain implements SdkTokenProvider, SdkAutoCloseable {
    private static final Logger log = Logger.loggerFor(SdkTokenProviderChain.class);

    private final List<IdentityProvider<? extends TokenIdentity>> sdkTokenProviders;

    private final boolean reuseLastProviderEnabled;

    private volatile IdentityProvider<? extends TokenIdentity> lastUsedProvider;

    private SdkTokenProviderChain(BuilderImpl builder) {
        Validate.notEmpty(builder.tokenProviders, "No token providers were specified.");
        this.reuseLastProviderEnabled = builder.reuseLastProviderEnabled;
        this.sdkTokenProviders = Collections.unmodifiableList(builder.tokenProviders);
    }

    /**
     * Get a new builder for creating a {@link SdkTokenProviderChain}.
     * <p>
     * {@snippet :
     * SdkTokenProviderChain tokenProvider =
     *     SdkTokenProviderChain.builder()
     *                          .addTokenProvider(ProfileTokenProvider.create())
     *                          .addTokenProvider(IdentityProvider.staticToken("bearer-token"))
     *                          .build();
     * }
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * Create a token provider chain that checks the given token providers.
     * <p>
     * {@snippet :
     * SdkTokenProviderChain tokenProvider =
     *     SdkTokenProviderChain.of(ProfileTokenProvider.create(),
     *                              IdentityProvider.staticToken("bearer-token"));
     * }
     */
    public static SdkTokenProviderChain of(SdkTokenProvider... sdkTokenProviders) {
        return builder().tokenProviders(sdkTokenProviders).build();
    }

    /**
     * Create a token provider chain that checks the given identity providers.
     * <p>
     * {@snippet :
     * SdkTokenProviderChain tokenProvider =
     *     SdkTokenProviderChain.of(ProfileTokenProvider.create(),
     *                              IdentityProvider.staticToken("bearer-token"));
     * }
     */
    public static SdkTokenProviderChain of(IdentityProvider<? extends TokenIdentity>... sdkTokenProviders) {
        return builder().tokenProviders(sdkTokenProviders).build();
    }

    @Override
    public SdkToken resolveToken() {
        if (reuseLastProviderEnabled && lastUsedProvider != null) {
            TokenIdentity tokenIdentity = CompletableFutureUtils.joinLikeSync(lastUsedProvider.resolveIdentity());
            return TokenUtils.toSdkToken(tokenIdentity);
        }

        List<String> exceptionMessages = null;
        for (IdentityProvider<? extends TokenIdentity> provider : sdkTokenProviders) {
            try {
                TokenIdentity token = CompletableFutureUtils.joinLikeSync(provider.resolveIdentity());

                log.debug(() -> "Loading token from " + provider);

                lastUsedProvider = provider;
                return TokenUtils.toSdkToken(token);
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

    /**
     * Close every token provider in this chain that implements {@link AutoCloseable}.
     */
    @Override
    public void close() {
        sdkTokenProviders.forEach(c -> IoUtils.closeIfCloseable(c, null));
    }

    @Override
    public String toString() {
        return ToString.builder("SdkTokenProviderChain")
                       .add("tokenProviders", sdkTokenProviders)
                       .build();
    }

    /**
     * See {@link SdkTokenProviderChain} for detailed documentation.
     */
    public interface Builder {
        /**
         * Controls whether the chain should reuse the last successful token provider in the chain. Reusing the last
         * successful token provider will typically return a token faster than searching through the chain.
         *
         * <p>
         * If not specified, this is {@code true}.
         *
         * <p>
         * {@snippet :
         * SdkTokenProviderChain.builder()
         *                      .addTokenProvider(ProfileTokenProvider.create())
         *                      .addTokenProvider(IdentityProvider.staticToken("bearer-token"))
         *                      .reuseLastProviderEnabled(true)
         *                      .build()
         * }
         */
        Builder reuseLastProviderEnabled(Boolean reuseLastProviderEnabled);

        /**
         * Configure which token providers should be checked by this chain, in the order they should be checked.
         *
         * <p>
         * This will replace any token or identity providers already added to this chain. At least one provider
         * must be added in this chain before it is built.
         */
        Builder tokenProviders(Collection<? extends SdkTokenProvider> tokenProviders);

        /**
         * Configure which identity providers should be checked by this chain, in the order they should be checked.
         *
         * <p>
         * This will replace any token or identity providers already added to this chain. At least one provider
         * must be added in this chain before it is built.
         *
         * <p>
         * {@snippet :
         * SdkTokenProviderChain.builder()
         *                      .tokenIdentityProviders(Arrays.asList(ProfileTokenProvider.create(),
         *                                                            IdentityProvider.staticToken("bearer-token")))
         *                      .build()
         * }
         */
        Builder tokenIdentityProviders(Collection<? extends IdentityProvider<? extends TokenIdentity>> tokenProviders);

        /**
         * Configure which token providers should be checked by this chain, in the order they should be checked.
         *
         * <p>
         * This will replace any token or identity providers already added to this chain. At least one provider
         * must be added in this chain before it is built.
         */
        default Builder tokenProviders(SdkTokenProvider... tokenProviders) {
            return tokenProviders((IdentityProvider<? extends TokenIdentity>[]) tokenProviders);
        }

        /**
         * Configure which identity providers should be checked by this chain, in the order they should be checked.
         *
         * <p>
         * This will replace any token or identity providers already added to this chain. At least one provider
         * must be added in this chain before it is built.
         *
         * <p>
         * {@snippet :
         * SdkTokenProviderChain.builder()
         *                      .tokenProviders(ProfileTokenProvider.create(),
         *                                      IdentityProvider.staticToken("bearer-token"))
         *                      .build()
         * }
         */
        default Builder tokenProviders(IdentityProvider<? extends TokenIdentity>... tokenProviders) {
            throw new UnsupportedOperationException();
        }

        /**
         * Add a token provider that should be checked to this chain, added after any providers already configured in this
         * chain.
         *
         * <p>
         * At least one provider must be added in this chain before it is built.
         *
         * <p>
         * {@snippet :
         * SdkTokenProviderChain.builder()
         *                      .addTokenProvider(ProfileTokenProvider.create())
         *                      .addTokenProvider(IdentityProvider.staticToken("bearer-token"))
         *                      .build()
         * }
         */
        default Builder addTokenProvider(SdkTokenProvider tokenProvider) {
            return addTokenProvider((IdentityProvider<? extends TokenIdentity>) tokenProvider);
        }

        /**
         * Add an identity provider that should be checked to this chain, added after any providers already configured in this
         * chain.
         *
         * <p>
         * At least one provider must be added in this chain before it is built.
         *
         * <p>
         * {@snippet :
         * SdkTokenProviderChain.builder()
         *                      .addTokenProvider(ProfileTokenProvider.create())
         *                      .addTokenProvider(IdentityProvider.staticToken("bearer-token"))
         *                      .build()
         * }
         */
        default Builder addTokenProvider(IdentityProvider<? extends TokenIdentity> tokenProvider) {
            throw new UnsupportedOperationException();
        }

        /**
         * Build the {@link SdkTokenProviderChain}.
         *
         * <p>
         * {@snippet :
         * SdkTokenProviderChain tokenProvider =
         *     SdkTokenProviderChain.builder()
         *                          .addTokenProvider(ProfileTokenProvider.create())
         *                          .addTokenProvider(IdentityProvider.staticToken("bearer-token"))
         *                          .build();
         * }
         */
        SdkTokenProviderChain build();
    }

    private static final class BuilderImpl implements Builder {
        private Boolean reuseLastProviderEnabled = true;
        private List<IdentityProvider<? extends TokenIdentity>> tokenProviders = new ArrayList<>();

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
        public Builder tokenProviders(Collection<? extends SdkTokenProvider> tokenProviders) {
            this.tokenProviders = new ArrayList<>(tokenProviders);
            return this;
        }

        public void setTokenProviders(Collection<? extends SdkTokenProvider> tokenProviders) {
            tokenProviders(tokenProviders);
        }

        @Override
        public Builder tokenIdentityProviders(Collection<? extends IdentityProvider<? extends TokenIdentity>> tokenProviders) {
            this.tokenProviders = new ArrayList<>(tokenProviders);
            return this;
        }

        public void setTokenIdentityProviders(Collection<? extends IdentityProvider<? extends TokenIdentity>> tokenProviders) {
            tokenIdentityProviders(tokenProviders);
        }

        public Builder tokenProviders(IdentityProvider<? extends TokenIdentity>... tokenProvider) {
            return tokenIdentityProviders(Arrays.asList(tokenProvider));
        }

        @Override
        public Builder addTokenProvider(IdentityProvider<? extends TokenIdentity> tokenProvider) {
            this.tokenProviders.add(tokenProvider);
            return this;
        }

        @Override
        public SdkTokenProviderChain build() {
            return new SdkTokenProviderChain(this);
        }
    }
}
