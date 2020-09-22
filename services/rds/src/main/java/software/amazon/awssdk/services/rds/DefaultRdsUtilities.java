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

package software.amazon.awssdk.services.rds;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;
import software.amazon.awssdk.utils.StringUtils;

@Immutable
@SdkInternalApi
final class DefaultRdsUtilities implements RdsUtilities {
    // The time the IAM token is good for. https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/UsingWithRDS.IAMDBAuth.html
    private static final Duration EXPIRATION_DURATION = Duration.ofMinutes(15);

    private final Aws4Signer signer = Aws4Signer.create();
    private final Region region;
    private final AwsCredentialsProvider credentialsProvider;
    private final Clock clock;

    DefaultRdsUtilities(DefaultBuilder builder) {
        this(builder, Clock.systemUTC());
    }

    /**
     * Test Only
     */
    DefaultRdsUtilities(DefaultBuilder builder, Clock clock) {
        this.credentialsProvider = builder.credentialsProvider;
        this.region = builder.region;
        this.clock = clock;
    }

    /**
     * Used by RDS low-level client's utilities() method
     */
    @SdkInternalApi
    static RdsUtilities create(SdkClientConfiguration clientConfiguration) {
        return new DefaultBuilder().clientConfiguration(clientConfiguration).build();
    }

    @Override
    public String generateAuthenticationToken(GenerateAuthenticationTokenRequest request) {
        SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder()
                                            .method(SdkHttpMethod.GET)
                                            .protocol("https")
                                            .host(request.hostname())
                                            .port(request.port())
                                            .encodedPath("/")
                                            .putRawQueryParameter("DBUser", request.username())
                                            .putRawQueryParameter("Action", "connect")
                                            .build();

        Instant expirationTime = Instant.now(clock).plus(EXPIRATION_DURATION);
        Aws4PresignerParams presignRequest = Aws4PresignerParams.builder()
                                                .signingClockOverride(clock)
                                                .expirationTime(expirationTime)
                                                .awsCredentials(resolveCredentials(request).resolveCredentials())
                                                .signingName("rds-db")
                                                .signingRegion(resolveRegion(request))
                                                .build();

        SdkHttpFullRequest fullRequest = signer.presign(httpRequest, presignRequest);
        String signedUrl = fullRequest.getUri().toString();

        // Format should be: <hostname>>:<port>>/?Action=connect&DBUser=<username>>&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Expi...
        // Note: This must be the real RDS hostname, not proxy or tunnels
        return StringUtils.replacePrefixIgnoreCase(signedUrl, "https://", "");
    }

    private Region resolveRegion(GenerateAuthenticationTokenRequest request) {
        if (request.region() != null) {
            return request.region();
        }

        if (this.region != null) {
            return this.region;
        }

        throw new IllegalArgumentException("Region should be provided either in GenerateAuthenticationTokenRequest object " +
                "or RdsUtilities object");
    }

    private AwsCredentialsProvider resolveCredentials(GenerateAuthenticationTokenRequest request) {
        if (request.credentialsProvider() != null) {
            return request.credentialsProvider();
        }

        if (this.credentialsProvider != null) {
            return this.credentialsProvider;
        }

        throw new IllegalArgumentException("CredentialProvider should be provided either in GenerateAuthenticationTokenRequest " +
                "object or RdsUtilities object");
    }

    @SdkInternalApi
    static final class DefaultBuilder implements Builder {
        private Region region;
        private AwsCredentialsProvider credentialsProvider;

        DefaultBuilder() {
        }

        Builder clientConfiguration(SdkClientConfiguration clientConfiguration) {
            this.credentialsProvider = clientConfiguration.option(AwsClientOption.CREDENTIALS_PROVIDER);
            this.region = clientConfiguration.option(AwsClientOption.AWS_REGION);

            return this;
        }

        @Override
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        /**
         * Construct a {@link RdsUtilities} object.
         */
        @Override
        public RdsUtilities build() {
            return new DefaultRdsUtilities(this);
        }
    }
}