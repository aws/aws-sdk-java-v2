/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.acm.waiters;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.internal.waiters.WaiterAttribute;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.acm.AcmClient;
import software.amazon.awssdk.services.acm.model.AcmRequest;
import software.amazon.awssdk.services.acm.model.DescribeCertificateRequest;
import software.amazon.awssdk.services.acm.model.DescribeCertificateResponse;
import software.amazon.awssdk.services.acm.waiters.internal.WaitersRuntime;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.SdkAutoCloseable;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
@ThreadSafe
final class DefaultAcmWaiter implements AcmWaiter {
    private static final WaiterAttribute<SdkAutoCloseable> CLIENT_ATTRIBUTE = new WaiterAttribute<>(SdkAutoCloseable.class);

    private final AcmClient client;

    private final AttributeMap managedResources;

    private final Waiter<DescribeCertificateResponse> certificateValidatedWaiter;

    private DefaultAcmWaiter(DefaultBuilder builder) {
        AttributeMap.Builder attributeMapBuilder = AttributeMap.builder();
        if (builder.client == null) {
            this.client = AcmClient.builder().build();
            attributeMapBuilder.put(CLIENT_ATTRIBUTE, this.client);
        } else {
            this.client = builder.client;
        }
        managedResources = attributeMapBuilder.build();
        this.certificateValidatedWaiter = Waiter.builder(DescribeCertificateResponse.class)
                .acceptors(certificateValidatedWaiterAcceptors())
                .overrideConfiguration(certificateValidatedWaiterConfig(builder.overrideConfiguration)).build();
    }

    private static String errorCode(Throwable error) {
        if (error instanceof AwsServiceException) {
            return ((AwsServiceException) error).awsErrorDetails().errorCode();
        }
        return null;
    }

    @Override
    public WaiterResponse<DescribeCertificateResponse> waitUntilCertificateValidated(
            DescribeCertificateRequest describeCertificateRequest) {
        return certificateValidatedWaiter
                .run(() -> client.describeCertificate(applyWaitersUserAgent(describeCertificateRequest)));
    }

    @Override
    public WaiterResponse<DescribeCertificateResponse> waitUntilCertificateValidated(
            DescribeCertificateRequest describeCertificateRequest, WaiterOverrideConfiguration overrideConfig) {
        return certificateValidatedWaiter.run(
                () -> client.describeCertificate(applyWaitersUserAgent(describeCertificateRequest)),
                certificateValidatedWaiterConfig(overrideConfig));
    }

    private static List<WaiterAcceptor<? super DescribeCertificateResponse>> certificateValidatedWaiterAcceptors() {
        List<WaiterAcceptor<? super DescribeCertificateResponse>> result = new ArrayList<>();
        result.add(WaiterAcceptor.successOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Certificate").field("DomainValidationOptions").flatten()
                    .field("ValidationStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().allMatch(v -> Objects.equals(v, "SUCCESS"));
        }));
        result.add(WaiterAcceptor.retryOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Certificate").field("DomainValidationOptions").flatten()
                    .field("ValidationStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "PENDING_VALIDATION"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            return Objects.equals(input.field("Certificate").field("Status").value(), "FAILED");
        }));
        result.add(WaiterAcceptor.errorOnExceptionAcceptor(error -> Objects.equals(errorCode(error), "ResourceNotFoundException")));
        result.addAll(WaitersRuntime.DEFAULT_ACCEPTORS);
        return result;
    }

    private static WaiterOverrideConfiguration certificateValidatedWaiterConfig(WaiterOverrideConfiguration overrideConfig) {
        Optional<WaiterOverrideConfiguration> optionalOverrideConfig = Optional.ofNullable(overrideConfig);
        int maxAttempts = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::maxAttempts).orElse(40);
        BackoffStrategy backoffStrategy = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::backoffStrategy).orElse(
                FixedDelayBackoffStrategy.create(Duration.ofSeconds(60)));
        Duration waitTimeout = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::waitTimeout).orElse(null);
        return WaiterOverrideConfiguration.builder().maxAttempts(maxAttempts).backoffStrategy(backoffStrategy)
                .waitTimeout(waitTimeout).build();
    }

    @Override
    public void close() {
        managedResources.close();
    }

    public static AcmWaiter.Builder builder() {
        return new DefaultBuilder();
    }

    private <T extends AcmRequest> T applyWaitersUserAgent(T request) {
        Consumer<AwsRequestOverrideConfiguration.Builder> userAgentApplier = b -> b.addApiName(ApiName.builder()
                .version("waiter").name("hll").build());
        AwsRequestOverrideConfiguration overrideConfiguration = request.overrideConfiguration()
                .map(c -> c.toBuilder().applyMutation(userAgentApplier).build())
                .orElse((AwsRequestOverrideConfiguration.builder().applyMutation(userAgentApplier).build()));
        return (T) request.toBuilder().overrideConfiguration(overrideConfiguration).build();
    }

    public static final class DefaultBuilder implements AcmWaiter.Builder {
        private AcmClient client;

        private WaiterOverrideConfiguration overrideConfiguration;

        private DefaultBuilder() {
        }

        @Override
        public AcmWaiter.Builder overrideConfiguration(WaiterOverrideConfiguration overrideConfiguration) {
            this.overrideConfiguration = overrideConfiguration;
            return this;
        }

        @Override
        public AcmWaiter.Builder client(AcmClient client) {
            this.client = client;
            return this;
        }

        public AcmWaiter build() {
            return new DefaultAcmWaiter(this);
        }
    }
}
