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

package software.amazon.awssdk.services.dsql;

import java.time.Clock;
import java.time.Instant;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.CredentialUtils;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dsql.model.GenerateAuthTokenRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;

@Immutable
@SdkInternalApi
public final class DefaultDsqlUtilities implements DsqlUtilities {
    private static final Logger log = Logger.loggerFor(DsqlUtilities.class);
    private final Aws4Signer signer = Aws4Signer.create();
    private final Region region;
    private final IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider;
    private final Clock clock;

    public DefaultDsqlUtilities(DefaultBuilder builder) {
        this(builder, Clock.systemUTC());
    }

    /**
     * For testing purposes only
     */
    @SdkTestInternalApi
    public DefaultDsqlUtilities(DefaultBuilder builder, Clock clock) {
        this.credentialsProvider = builder.credentialsProvider;
        this.region = builder.region;
        this.clock = clock;
    }

    /**
     * Used by DSQL low-level client's utilities() method
     */
    @SdkInternalApi
    static DsqlUtilities create(SdkClientConfiguration clientConfiguration) {
        return new DefaultBuilder().clientConfiguration(clientConfiguration).build();
    }

    @Override
    public String generateDbConnectAuthToken(GenerateAuthTokenRequest request) {
        return generateAuthToken(request, false);
    }

    @Override
    public String generateDbConnectAdminAuthToken(GenerateAuthTokenRequest request) {
        return generateAuthToken(request, true);
    }

    private String generateAuthToken(GenerateAuthTokenRequest request, boolean isAdmin) {
        String action = isAdmin ? "DbConnectAdmin" : "DbConnect";

        SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder()
                                                           .method(SdkHttpMethod.GET)
                                                           .protocol("https")
                                                           .host(request.hostname())
                                                           .encodedPath("/")
                                                           .putRawQueryParameter("Action", action)
                                                           .build();

        Instant expirationTime = Instant.now(clock).plus(request.expiresIn());

        Aws4PresignerParams presignRequest = Aws4PresignerParams.builder()
                                                                .signingClockOverride(clock)
                                                                .expirationTime(expirationTime)
                                                                .awsCredentials(resolveCredentials(request))
                                                                .signingName("dsql")
                                                                .signingRegion(resolveRegion(request))
                                                                .build();

        SdkHttpFullRequest fullRequest = signer.presign(httpRequest, presignRequest);
        String signedUrl = fullRequest.getUri().toString();

        log.debug(() -> "Generated DSQL authentication token with expiration of " + expirationTime);
        return StringUtils.replacePrefixIgnoreCase(signedUrl, "https://", "");
    }

    private Region resolveRegion(GenerateAuthTokenRequest request) {
        if (request.region() != null) {
            return request.region();
        }

        if (this.region != null) {
            return this.region;
        }

        throw new IllegalArgumentException("Region must be provided in GenerateAuthTokenRequest or DsqlUtilities");
    }

    // TODO: update this to use AwsCredentialsIdentity when we migrate Signers to accept the new type.
    private AwsCredentials resolveCredentials(GenerateAuthTokenRequest request) {
        if (request.credentialsIdentityProvider() != null) {
            return CredentialUtils.toCredentials(
                CompletableFutureUtils.joinLikeSync(request.credentialsIdentityProvider().resolveIdentity()));
        }

        if (this.credentialsProvider != null) {
            return CredentialUtils.toCredentials(CompletableFutureUtils.joinLikeSync(this.credentialsProvider.resolveIdentity()));
        }

        throw new IllegalArgumentException("CredentialProvider must be provided in GenerateAuthTokenRequest or DsqlUtilities");
    }

    @SdkInternalApi
    public static final class DefaultBuilder implements DsqlUtilities.Builder {
        private Region region;
        private IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider;

        Builder clientConfiguration(SdkClientConfiguration clientConfiguration) {
            this.credentialsProvider = clientConfiguration.option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER);
            this.region = clientConfiguration.option(AwsClientOption.AWS_REGION);

            return this;
        }

        @Override
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public Builder credentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        /**
         * Construct a {@link DsqlUtilities} object.
         */
        @Override
        public DsqlUtilities build() {
            return new DefaultDsqlUtilities(this);
        }
    }
}