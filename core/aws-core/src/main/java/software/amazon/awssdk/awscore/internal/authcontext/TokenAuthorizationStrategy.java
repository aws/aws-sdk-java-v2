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

package software.amazon.awssdk.awscore.internal.authcontext;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.auth.token.signer.SdkTokenExecutionAttribute;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.Validate;

/**
 * An authorization strategy for tokens that can resolve a compatible signer as
 * well as provide a resolved token as an execution attribute.
 */
@SdkInternalApi
public final class TokenAuthorizationStrategy implements AuthorizationStrategy {

    private final SdkRequest request;
    private final Signer defaultSigner;
    private final SdkTokenProvider defaultTokenProvider;
    private final MetricCollector metricCollector;

    public TokenAuthorizationStrategy(Builder builder) {
        this.request = builder.request();
        this.defaultSigner = builder.defaultSigner();
        this.defaultTokenProvider = builder.defaultTokenProvider();
        this.metricCollector = builder.metricCollector();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Request override signers take precedence over the default alternative, for instance what is specified in the
     * client. Request override signers can also be modified by modifyRequest interceptors.
     *
     * @return The signer that will be used by the SDK to sign the request
     */
    @Override
    public Signer resolveSigner() {
        return request.overrideConfiguration()
                      .flatMap(RequestOverrideConfiguration::signer)
                      .orElse(defaultSigner);
    }

    /**
     * Add credentials to be used by the signer in later stages.
     */
    @Override
    public void addCredentialsToExecutionAttributes(ExecutionAttributes executionAttributes) {
        SdkToken credentials = resolveToken(defaultTokenProvider, metricCollector);
        executionAttributes.putAttribute(SdkTokenExecutionAttribute.SDK_TOKEN, credentials);
    }

    private static SdkToken resolveToken(SdkTokenProvider tokenProvider, MetricCollector metricCollector) {
        Validate.notNull(tokenProvider, "No token provider exists to resolve a token from.");

        Pair<SdkToken, Duration> measured = MetricUtils.measureDuration(tokenProvider::resolveToken);
        metricCollector.reportMetric(CoreMetric.TOKEN_FETCH_DURATION, measured.right());
        SdkToken credentials = measured.left();

        Validate.validState(credentials != null, "Token providers must never return null.");
        return credentials;
    }

    public static final class Builder {
        private SdkRequest request;
        private Signer defaultSigner;
        private SdkTokenProvider defaultTokenProvider;
        private MetricCollector metricCollector;

        private Builder() {
        }

        public SdkRequest request() {
            return this.request;
        }

        public Builder request(SdkRequest request) {
            this.request = request;
            return this;
        }

        public Signer defaultSigner() {
            return this.defaultSigner;
        }

        public Builder defaultSigner(Signer defaultSigner) {
            this.defaultSigner = defaultSigner;
            return this;
        }

        public SdkTokenProvider defaultTokenProvider() {
            return this.defaultTokenProvider;
        }

        public Builder defaultTokenProvider(SdkTokenProvider defaultTokenProvider) {
            this.defaultTokenProvider = defaultTokenProvider;
            return this;
        }

        public MetricCollector metricCollector() {
            return this.metricCollector;
        }

        public Builder metricCollector(MetricCollector metricCollector) {
            this.metricCollector = metricCollector;
            return this;
        }

        public TokenAuthorizationStrategy build() {
            return new TokenAuthorizationStrategy(this);
        }
    }
}
