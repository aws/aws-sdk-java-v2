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
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.CredentialUtils;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.Validate;

/**
 * An authorization strategy for AWS Credentials that can resolve a compatible signer as
 * well as provide resolved AWS credentials as an execution attribute.
 */
@SdkInternalApi
public final class AwsCredentialsAuthorizationStrategy implements AuthorizationStrategy {

    private final SdkRequest request;
    private final Signer defaultSigner;
    private final IdentityProvider<? extends AwsCredentialsIdentity> defaultCredentialsProvider;
    private final MetricCollector metricCollector;

    public AwsCredentialsAuthorizationStrategy(Builder builder) {
        this.request = builder.request();
        this.defaultSigner = builder.defaultSigner();
        this.defaultCredentialsProvider = builder.defaultCredentialsProvider();
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
     *
     */
    @Override
    public Signer resolveSigner() {
        return request.overrideConfiguration()
                      .flatMap(RequestOverrideConfiguration::signer)
                      .orElse(defaultSigner);
    }

    /**
     * Add credentials to be used by the signer in later stages.
     *
     * @return
     */
    @Override
    public CompletableFuture<Void> addCredentialsToExecutionAttributes(ExecutionAttributes executionAttributes) {
        IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider =
            resolveCredentialsProvider(request, defaultCredentialsProvider);
        Validate.notNull(credentialsProvider, "No credentials provider exists to resolve credentials from.");

        long start = System.nanoTime();
        // TODO: CompletableFutureUtils.forwardExceptionTo() here too?
        return credentialsProvider.resolveIdentity().thenAccept(credentialsIdentity -> {
            metricCollector.reportMetric(CoreMetric.CREDENTIALS_FETCH_DURATION, Duration.ofNanos(System.nanoTime() - start));

            Validate.validState(credentialsIdentity != null, "Credential providers must never return null.");
            // TODO: Should the signer be changed to use AwsCredentialsIdentity? Maybe with Signer SRA work, not now.
            executionAttributes.putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS,
                                             CredentialUtils.toCredentials(credentialsIdentity));
        });
    }

    /**
     * Resolves the credentials provider, with the request override configuration taking precedence over the
     * provided default.
     *
     * @return The credentials provider that will be used by the SDK to resolve credentials
     */
    private static IdentityProvider<? extends AwsCredentialsIdentity> resolveCredentialsProvider(
            SdkRequest originalRequest,
            IdentityProvider<? extends AwsCredentialsIdentity> defaultProvider) {
        return originalRequest.overrideConfiguration()
                              .filter(c -> c instanceof AwsRequestOverrideConfiguration)
                              .map(c -> (AwsRequestOverrideConfiguration) c)
                              .flatMap(AwsRequestOverrideConfiguration::credentialsIdentityProvider)
                              .orElse(defaultProvider);
    }

    public static final class Builder {
        private SdkRequest request;
        private Signer defaultSigner;
        private IdentityProvider<? extends AwsCredentialsIdentity> defaultCredentialsProvider;
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

        public IdentityProvider<? extends AwsCredentialsIdentity> defaultCredentialsProvider() {
            return this.defaultCredentialsProvider;
        }

        public Builder defaultCredentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> defaultCredentialsProvider) {
            this.defaultCredentialsProvider = defaultCredentialsProvider;
            return this;
        }

        public MetricCollector metricCollector() {
            return this.metricCollector;
        }

        public Builder metricCollector(MetricCollector metricCollector) {
            this.metricCollector = metricCollector;
            return this;
        }

        public AwsCredentialsAuthorizationStrategy build() {
            return new AwsCredentialsAuthorizationStrategy(this);
        }
    }
}
