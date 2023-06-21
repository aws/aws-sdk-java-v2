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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
import software.amazon.awssdk.core.waiters.AsyncWaiter;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.acm.AcmAsyncClient;
import software.amazon.awssdk.services.acm.model.AcmRequest;
import software.amazon.awssdk.services.acm.model.DescribeCertificateRequest;
import software.amazon.awssdk.services.acm.model.DescribeCertificateResponse;
import software.amazon.awssdk.services.acm.waiters.internal.WaitersRuntime;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
@ThreadSafe
final class DefaultAcmAsyncWaiter implements AcmAsyncWaiter {
    private static final WaiterAttribute<SdkAutoCloseable> CLIENT_ATTRIBUTE = new WaiterAttribute<>(SdkAutoCloseable.class);

    private static final WaiterAttribute<ScheduledExecutorService> SCHEDULED_EXECUTOR_SERVICE_ATTRIBUTE = new WaiterAttribute<>(
            ScheduledExecutorService.class);

    private final AcmAsyncClient client;

    private final AttributeMap managedResources;

    private final AsyncWaiter<DescribeCertificateResponse> certificateValidatedWaiter;

    private final ScheduledExecutorService executorService;

    private DefaultAcmAsyncWaiter(DefaultBuilder builder) {
        AttributeMap.Builder attributeMapBuilder = AttributeMap.builder();
        if (builder.client == null) {
            this.client = AcmAsyncClient.builder().build();
            attributeMapBuilder.put(CLIENT_ATTRIBUTE, this.client);
        } else {
            this.client = builder.client;
        }
        if (builder.executorService == null) {
            this.executorService = Executors.newScheduledThreadPool(1,
                    new ThreadFactoryBuilder().threadNamePrefix("waiters-ScheduledExecutor").build());
            attributeMapBuilder.put(SCHEDULED_EXECUTOR_SERVICE_ATTRIBUTE, this.executorService);
        } else {
            this.executorService = builder.executorService;
        }
        managedResources = attributeMapBuilder.build();
        this.certificateValidatedWaiter = AsyncWaiter.builder(DescribeCertificateResponse.class)
                .acceptors(certificateValidatedWaiterAcceptors())
                .overrideConfiguration(certificateValidatedWaiterConfig(builder.overrideConfiguration))
                .scheduledExecutorService(executorService).build();
    }

    private static String errorCode(Throwable error) {
        if (error instanceof AwsServiceException) {
            return ((AwsServiceException) error).awsErrorDetails().errorCode();
        }
        return null;
    }

    @Override
    public CompletableFuture<WaiterResponse<DescribeCertificateResponse>> waitUntilCertificateValidated(
            DescribeCertificateRequest describeCertificateRequest) {
        return certificateValidatedWaiter.runAsync(() -> client
                .describeCertificate(applyWaitersUserAgent(describeCertificateRequest)));
    }

    @Override
    public CompletableFuture<WaiterResponse<DescribeCertificateResponse>> waitUntilCertificateValidated(
            DescribeCertificateRequest describeCertificateRequest, WaiterOverrideConfiguration overrideConfig) {
        return certificateValidatedWaiter.runAsync(
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

    public static AcmAsyncWaiter.Builder builder() {
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

    public static final class DefaultBuilder implements AcmAsyncWaiter.Builder {
        private AcmAsyncClient client;

        private WaiterOverrideConfiguration overrideConfiguration;

        private ScheduledExecutorService executorService;

        private DefaultBuilder() {
        }

        @Override
        public AcmAsyncWaiter.Builder scheduledExecutorService(ScheduledExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        @Override
        public AcmAsyncWaiter.Builder overrideConfiguration(WaiterOverrideConfiguration overrideConfiguration) {
            this.overrideConfiguration = overrideConfiguration;
            return this;
        }

        @Override
        public AcmAsyncWaiter.Builder client(AcmAsyncClient client) {
            this.client = client;
            return this;
        }

        public AcmAsyncWaiter build() {
            return new DefaultAcmAsyncWaiter(this);
        }
    }
}
