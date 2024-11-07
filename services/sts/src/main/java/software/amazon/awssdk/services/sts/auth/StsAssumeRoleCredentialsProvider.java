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

package software.amazon.awssdk.services.sts.auth;

import static software.amazon.awssdk.services.sts.internal.StsAuthUtils.accountIdFromArn;
import static software.amazon.awssdk.services.sts.internal.StsAuthUtils.fromStsCredentials;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An {@link IdentityProvider}{@code <}{@link AwsCredentialsIdentity}{@code >} implementation that loads credentials by
 * assuming a role from STS using {@link StsClient#assumeRole(AssumeRoleRequest)}.
 *
 * <p>
 * This credential provider caches the credentials, and will only invoke STS periodically
 * to keep the credentials "fresh". As a result, it is recommended that you create a single credentials provider of this type
 * and reuse it throughout your application. You may notice small latency increases on requests that refresh the cached
 * credentials. To avoid this latency increase, you can enable async refreshing with
 * {@link Builder#asyncCredentialUpdateEnabled(Boolean)}. If you enable this setting, you must {@link #close()} the credentials
 * provider if you are done using it, to disable the background refreshing task. If you fail to do this, your application could
 * run out of resources.
 *
 * <p>
 * This credentials provider is used by the {@link ProfileCredentialsProvider} if the {@code role_arn} profile
 * property is configured. The {@code ProfileCredentialsProvider} is included in the {@link DefaultCredentialsProvider}.
 *
 * <p>
 * Create using {@link #builder()}:
 * {@snippet :
 * StsClient stsClient = StsClient.create();
 *
 * AssumeRoleRequest assumeRoleRequest =
 *     AssumeRoleRequest.builder()
 *                      .roleArn("arn:aws:iam::012345678901:role/custom-role-to-assume")
 *                      .roleSessionName("some-session-name")
 *                      .build();
 *
 * StsAssumeRoleCredentialsProvider credentialsProvider =
 *     StsAssumeRoleCredentialsProvider.builder() // @link substring="builder" target="#builder()"
 *                                     .stsClient(stsClient)
 *                                     .refreshRequest(assumeRoleRequest)
 *                                     .build();
 *
 * S3Client s3 = S3Client.builder()
 *                       .credentialsProvider(credentialsProvider)
 *                       .build();
 *}
 */
@SdkPublicApi
@ThreadSafe
public final class StsAssumeRoleCredentialsProvider
    extends StsCredentialsProvider
    implements ToCopyableBuilder<StsAssumeRoleCredentialsProvider.Builder, StsAssumeRoleCredentialsProvider> {
    private static final String PROVIDER_NAME = "StsAssumeRoleCredentialsProvider";
    private final Supplier<AssumeRoleRequest> assumeRoleRequestSupplier;

    private StsAssumeRoleCredentialsProvider(Builder builder) {
        super(builder, "sts-assume-role-credentials-provider");
        Validate.notNull(builder.assumeRoleRequestSupplier, "Assume role request must not be null.");

        this.assumeRoleRequestSupplier = builder.assumeRoleRequestSupplier;
    }

    /**
     * Get a new builder for creating a {@link StsAssumeRoleCredentialsProvider}.
     * <p>
     * {@snippet :
     * StsClient stsClient = StsClient.create();
     *
     * AssumeRoleRequest assumeRoleRequest =
     *     AssumeRoleRequest.builder()
     *                      .roleArn("arn:aws:iam::012345678901:role/custom-role-to-assume")
     *                      .roleSessionName("some-session-name")
     *                      .build();
     *
     * StsAssumeRoleCredentialsProvider credentialsProvider =
     *     StsAssumeRoleCredentialsProvider.builder()
     *                                     .stsClient(stsClient)
     *                                     .refreshRequest(assumeRoleRequest)
     *                                     .build();
     * }
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected AwsSessionCredentials getUpdatedCredentials(StsClient stsClient) {
        AssumeRoleRequest assumeRoleRequest = assumeRoleRequestSupplier.get();
        Validate.notNull(assumeRoleRequest, "Assume role request must not be null.");
        AssumeRoleResponse assumeRoleResponse = stsClient.assumeRole(assumeRoleRequest);
        return fromStsCredentials(assumeRoleResponse.credentials(),
                                  PROVIDER_NAME,
                                  accountIdFromArn(assumeRoleResponse.assumedRoleUser()));
    }

    @Override
    public String toString() {
        return ToString.builder(PROVIDER_NAME)
                       .add("refreshRequest", assumeRoleRequestSupplier)
                       .build();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    String providerName() {
        return PROVIDER_NAME;
    }

    /**
     * See {@link StsAssumeRoleCredentialsProvider} for detailed documentation.
     */
    @NotThreadSafe
    public static final class Builder extends BaseBuilder<Builder, StsAssumeRoleCredentialsProvider> {
        private Supplier<AssumeRoleRequest> assumeRoleRequestSupplier;

        private Builder() {
            super(StsAssumeRoleCredentialsProvider::new);
        }

        private Builder(StsAssumeRoleCredentialsProvider provider) {
            super(StsAssumeRoleCredentialsProvider::new, provider);
            this.assumeRoleRequestSupplier = provider.assumeRoleRequestSupplier;
        }

        /**
         * Specify an {@link StsClient} to use when assuming the IAM role.
         *
         * <p>
         * The provided client will not be closed if this credentials provider is {@link #close()}d.
         *
         * <p>
         * If not specified, credential provider creation will fail.
         *
         * <p>
         * {@snippet :
         * StsClient stsClient = StsClient.create();
         *
         * StsAssumeRoleCredentialsProvider credentialsProvider =
         *     StsAssumeRoleCredentialsProvider.builder()
         *                                     .stsClient(stsClient)
         *                                     .refreshRequest(...)
         *                                     .build();
         *}
         */
        @Override
        public Builder stsClient(StsClient stsClient) {
            return super.stsClient(stsClient);
        }

        /**
         * Specify the {@link AssumeRoleRequest} that should be used when assuming the IAM role.
         *
         * <p>
         * The provided request will be passed to {@link StsClient#assumeRole(AssumeRoleRequest)}.
         *
         * <p>
         * If not specified, credential provider creation will fail.
         *
         * <p>
         * {@snippet :
         * AssumeRoleRequest assumeRoleRequest =
         *     AssumeRoleRequest.builder()
         *                      .roleArn("arn:aws:iam::012345678901:role/custom-role-to-assume")
         *                      .roleSessionName("some-session-name")
         *                      .build();
         *
         * StsAssumeRoleCredentialsProvider credentialsProvider =
         *     StsAssumeRoleCredentialsProvider.builder()
         *                                     .stsClient(...)
         *                                     .refreshRequest(assumeRoleRequest)
         *                                     .build();
         *}
         */
        public Builder refreshRequest(AssumeRoleRequest assumeRoleRequest) {
            return refreshRequest(() -> assumeRoleRequest);
        }

        /**
         * Specify the {@link AssumeRoleRequest} that should be used when assuming the IAM role.
         *
         * <p>
         * Similar to {@link #refreshRequest(AssumeRoleRequest)}, but takes a lambda to configure a new
         * {@link AssumeRoleRequest.Builder}. This removes the need to called {@link AssumeRoleRequest#builder()} and
         * {@link AssumeRoleRequest.Builder#build()}. The consumer is only invoked once, when the method is called.
         *
         * <p>
         * If not specified, credential provider creation will fail.
         *
         * <p>
         * {@snippet :
         * String roleArn = "arn:aws:iam::012345678901:role/custom-role-to-assume";
         * StsAssumeRoleCredentialsProvider credentialsProvider =
         *     StsAssumeRoleCredentialsProvider.builder()
         *                                     .stsClient(...)
         *                                     .refreshRequest(r -> r.roleArn(roleArn)
         *                                                           .roleSessionName("some-session-name"))
         *                                     .build();
         *}
         */
        public Builder refreshRequest(Consumer<AssumeRoleRequest.Builder> assumeRoleRequest) {
            return refreshRequest(AssumeRoleRequest.builder().applyMutation(assumeRoleRequest).build());
        }

        /**
         * Specify a supplier for a {@link AssumeRoleRequest} that should be used when assuming the IAM role.
         *
         * <p>
         * The supplier will be called each time credentials need to be refreshed, which allows the assume role request to be
         * changed. The returned request will be passed as-is to {@link StsClient#assumeRole(AssumeRoleRequest)}.
         *
         * <p>
         * If not specified, credential provider creation will fail.
         *
         * <p>
         * {@snippet :
         * AtomicInteger roleNumber = new AtomicInteger(0);
         * Supplier<AssumeRoleRequest> assumeRoleRequestSupplier = () ->
         *     AssumeRoleRequest.builder()
         *                      .roleArn("arn:aws:iam::012345678901:role/custom-role-to-assume")
         *                      .roleSessionName("some-session-name-" + roleNumber.incrementAndGet())
         *                      .build();
         *
         * StsAssumeRoleCredentialsProvider credentialsProvider =
         *     StsAssumeRoleCredentialsProvider.builder()
         *                                     .stsClient(...)
         *                                     .refreshRequest(assumeRoleRequestSupplier)
         *                                     .build();
         *}
         */
        public Builder refreshRequest(Supplier<AssumeRoleRequest> assumeRoleRequestSupplier) {
            this.assumeRoleRequestSupplier = assumeRoleRequestSupplier;
            return this;
        }

        /**
         * Configure the amount of time between when the credentials expire and when the credential provider starts to pre-fetch
         * updated credentials.
         *
         * <p>
         * When the pre-fetch threshold is encountered, the SDK will block a single calling thread to refresh the credentials.
         * Other threads will continue to use the existing credentials. This prevents all SDK caller's latency from increasing
         * when the credential gets close to expiration, but you may still see a single call with increased latency as that
         * thread refreshes the credentials. To avoid this single-thread latency increase, you can enable async refreshing with
         * {@link #asyncCredentialUpdateEnabled(Boolean)}. When async refreshing is enabled, the pre-fetch threshold is
         * used to determine when the async refreshing thread should "run" to update the credentials.
         *
         * <p>
         * This value should be less than the {@link AssumeRoleRequest#durationSeconds()}, and greater than the
         * {@link #staleTime(Duration)} ({@code assumeRoleRequestDuration > prefetchTime > staleTime}).
         *
         * <p>
         * If not specified, {@code Duration.ofMinutes(5)} is used. (55 minutes after the default session duration, and 4
         * minutes before the default stale time).
         *
         * <p>
         * {@snippet :
         * StsAssumeRoleCredentialsProvider.builder()
         *                                 .stsClient(...)
         *                                 .refreshRequest(...)
         *                                 .prefetchTime(Duration.ofMinutes(5))
         *                                 .build();
         * }
         */
        @Override
        public Builder prefetchTime(Duration prefetchTime) {
            return super.prefetchTime(prefetchTime);
        }

        /**
         * Configure the amount of time between when the credentials actually expire and when the credential provider treats
         * those credentials as expired.
         *
         * <p>
         * If the SDK treated the credentials as expired exactly when the service reported they will expire (a stale time of 0
         * seconds), SDK calls could fail close to that expiration time. As a result, the SDK treats credentials as expired
         * 1 minute before the service reported that those credentials will expire.
         *
         * <p>
         * The failures that could occur without this threshold are caused by two primary factors:
         * <ul>
         *     <li>Request latency: There is latency between when the credentials are loaded and when the service processes
         *     the request. The SDK has to sign the request, transmit to the service, and the service has to validate the
         *     signature.</li>
         *     <li>Clock skew: The client and service may not have the exact same measure of time, so an expiration time for
         *     the service may be off from the expiration time for the client.</li>
         * </ul>
         *
         * <p>
         * When the stale threshold is encountered, the SDK will block all calling threads until a successful refresh is achieved.
         * (Note: while all threads are blocked, only one thread will actually make the service call to refresh the
         * credentials). Because this increase in latency for all threads is undesirable, you should ensure that the
         * {@link #prefetchTime(Duration)} is greater than the {@code staleTime}. When configured correctly, the stale time is
         * only encountered when the prefetch calls did not succeed (e.g. due to an outage).
         *
         * <p>
         * This value should be less than the {@link #prefetchTime(Duration)} ({@code assumeRoleRequestDuration > prefetchTime >
         * staleTime}).
         *
         * <p>
         * If not specified, {@code Duration.ofMinutes(1)} is used. (4 minutes after the default
         * {@link #prefetchTime(Duration)}, and 59 minutes after the default session duration).
         *
         * <p>
         * {@snippet :
         * StsAssumeRoleCredentialsProvider.builder()
         *                                 .stsClient(...)
         *                                 .refreshRequest(...)
         *                                 .staleTime(Duration.ofMinutes(1))
         *                                 .build();
         * }
         */
        @Override
        public Builder staleTime(Duration staleTime) {
            return super.staleTime(staleTime);
        }

        /**
         * Configure whether this provider should fetch credentials asynchronously in the background. If this is {@code true},
         * threads are less likely to block when credentials are loaded, but additional resources are used to maintain
         * the provider.
         *
         * <p>
         * If not specified, this is {@code false}.
         *
         * <p>
         * {@snippet :
         * StsAssumeRoleCredentialsProvider.builder()
         *                                 .stsClient(...)
         *                                 .refreshRequest(...)
         *                                 .asyncCredentialUpdateEnabled(false)
         *                                 .build();
         * }
         */
        @Override
        public Builder asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled) {
            return super.asyncCredentialUpdateEnabled(asyncCredentialUpdateEnabled);
        }

        /**
         * Build the {@link StsAssumeRoleCredentialsProvider}.
         *
         * <p>
         * {@snippet :
         * StsClient stsClient = StsClient.create();
         *
         * AssumeRoleRequest assumeRoleRequest =
         *     AssumeRoleRequest.builder()
         *                      .roleArn("arn:aws:iam::012345678901:role/custom-role-to-assume")
         *                      .roleSessionName("some-session-name")
         *                      .build();
         *
         * StsAssumeRoleCredentialsProvider credentialsProvider =
         *     StsAssumeRoleCredentialsProvider.builder()
         *                                     .stsClient(stsClient)
         *                                     .refreshRequest(assumeRoleRequest)
         *                                     .build();
         * }
         */
        @Override
        public StsAssumeRoleCredentialsProvider build() {
            return super.build();
        }
    }
}
