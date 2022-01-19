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

package software.amazon.awssdk.services.ssooidc.internal;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.token.AwsToken;
import software.amazon.awssdk.auth.token.AwsTokenProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.RefreshResult;

/**
 * Implementation of {@link AwsTokenProvider} that is capable of loading and
 * storing SSO tokens to {@code ~/.aws/sso/cache}. This is also capable of
 * refreshing the cached token via the SSO-OIDC service.
 */
@SdkPublicApi
@ThreadSafe
public final class SsoTokenProvider implements AwsTokenProvider {
    private final String startUrl;
    private final TokenManager onDiskTokenManager;
    private final Clock clock;

    private final CachedSupplier<SsoToken> tokenSupplier = createTokenSupplier();

    @SdkTestInternalApi
    SsoTokenProvider(String startUrl, TokenManager onDiskTokenLoader, Clock clock) {
        this.startUrl = startUrl;
        this.onDiskTokenManager = onDiskTokenLoader;
        this.clock = clock;
    }

    private SsoTokenProvider(BuilderImpl builder) {
        this.startUrl = builder.startUrl;
        this.onDiskTokenManager = OnDiskTokenManager.create(startUrl);
        this.clock = Clock.systemUTC();
    }

    @Override
    public AwsToken resolveToken() {
        return tokenSupplier.get();
    }

    @SdkTestInternalApi
    String startUrl() {
        return startUrl;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder {
        /**
         * The startUrl used to retrieve the SSO token.
         */
        Builder startUrl(String startUrl);

        SsoTokenProvider build();
    }

    private CachedSupplier<SsoToken> createTokenSupplier() {
        return CachedSupplier.builder(this::refreshToken).build();
    }

    /**
     * Note, this method is <b>not</b> threadsafe. External synchronization is required, such as through CachedSupplier so that
     * this class maintains its threadsafe guarantee.
     */
    private RefreshResult<SsoToken> refreshToken() {
        Optional<SsoToken> tokenOptional = onDiskTokenManager.loadToken();

        if (!tokenOptional.isPresent()) {
            throw SdkClientException.create("Unable to load SSO token");
        }

        SsoToken token = validateToken(tokenOptional.get());

        // TODO: implement refresh using SSO-OIDC client
        if (isExpired(token)) {
            throw SdkClientException.create("Token is expired");
        }

        return asRefreshResult(tokenOptional.get());
    }

    private boolean isExpired(SsoToken token) {
        Instant expiration = token.expirationTime();
        Instant now = clock.instant();

        return now.isAfter(expiration);
    }

    private SsoToken validateToken(SsoToken token) {
        Validate.notNull(token.token(), "token cannot be null");
        Validate.notNull(token.expirationTime(), "expirationTime cannot be null");
        return token;
    }

    private static RefreshResult<SsoToken> asRefreshResult(SsoToken token) {
        return RefreshResult.builder(token)
                            .staleTime(token.expirationTime())
                            .build();
    }

    private static class BuilderImpl implements Builder {
        private String startUrl;

        @Override
        public Builder startUrl(String startUrl) {
            this.startUrl = startUrl;
            return this;
        }

        @Override
        public SsoTokenProvider build() {
            return new SsoTokenProvider(this);
        }
    }
}
