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

package software.amazon.awssdk.services.s3control;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.services.s3control.endpoints.S3ControlClientContextParams;
import software.amazon.awssdk.services.s3control.model.ListJobsRequest;
import software.amazon.awssdk.utils.AttributeMap;

public class BuilderClientContextParamsTest {
    private ExecutionInterceptor mockInterceptor;

    private static AttributeMap baseExpectedParams;

    @BeforeAll
    public static void setup() {
        // This is the set of client context params with their values the same as default values in S3ControlConfiguration
        baseExpectedParams = AttributeMap.builder()
                                         .put(S3ControlClientContextParams.USE_ARN_REGION, false)
                                         .build();
    }

    @BeforeEach
    public void methodSetup() {
        mockInterceptor = mock(ExecutionInterceptor.class);
        when(mockInterceptor.modifyRequest(any(), any())).thenThrow(new RuntimeException("Oops"));
    }

    @MethodSource("testCases")
    @ParameterizedTest
    public void verifyParams(TestCase tc) {
        S3ControlClientBuilder builder = S3ControlClient.builder()
                                          .overrideConfiguration(o -> o.addExecutionInterceptor(mockInterceptor));

        if (tc.config() != null) {
            builder.serviceConfiguration(tc.config());
        }

        if (tc.builderModifier() != null) {
            tc.builderModifier().accept(builder);
        }

        if (tc.exception != null) {
            assertThatThrownBy(builder::build)
                .isInstanceOf(tc.exception)
                .hasMessageContaining(tc.exceptionMessage);
        } else {
            assertThatThrownBy(() -> {
                S3ControlClient s3control = builder.build();
                s3control.listJobs(ListJobsRequest.builder().accountId("1234").build());
            }).hasMessageContaining("Oops");

            ArgumentCaptor<ExecutionAttributes> executionAttrsCaptor = ArgumentCaptor.forClass(ExecutionAttributes.class);
            verify(mockInterceptor).modifyRequest(any(), executionAttrsCaptor.capture());

            AttributeMap clientContextParams =
                executionAttrsCaptor.getValue().getAttribute(SdkInternalExecutionAttribute.CLIENT_CONTEXT_PARAMS);

            assertThat(clientContextParams).isEqualTo(tc.expectedParams().merge(baseExpectedParams));
        }
    }

    public static List<TestCase> testCases() {
        List<TestCase> testCases = new ArrayList<>();

        // UseArnRegion on config
        testCases.add(
            TestCase.builder()
                    .config(S3ControlConfiguration.builder()
                                           .useArnRegionEnabled(true)
                                           .build())
                    .expectedParams(AttributeMap.builder()
                                                .put(S3ControlClientContextParams.USE_ARN_REGION, true)
                                                .build())
                    .build()
        );

        // UseArnRegion on builder
        testCases.add(
            TestCase.builder()
                    .builderModifier(b -> b.useArnRegion(true))
                    .expectedParams(AttributeMap.builder()
                                                .put(S3ControlClientContextParams.USE_ARN_REGION, true)
                                                .build())
                    .build()
        );

        // UseArnRegion on config and builder
        testCases.add(
            TestCase.builder()
                    .config(S3ControlConfiguration.builder()
                                           .useArnRegionEnabled(true)
                                           .build())
                    .builderModifier(b -> b.useArnRegion(true))
                    .exception(IllegalStateException.class)
                    .exceptionMessage("UseArnRegion")
                    .build()
        );

        return testCases;
    }

    public static class TestCase {
        private final S3ControlConfiguration config;
        private final Consumer<S3ControlClientBuilder> builderModifier;

        private final Class<?> exception;
        private final String exceptionMessage;

        private final AttributeMap expectedParams;

        private TestCase(Builder b) {
            this.config = b.config;
            this.builderModifier = b.builderModifier;
            this.exception = b.exception;
            this.exceptionMessage = b.exceptionMessage;
            this.expectedParams = b.expectedParams;
        }

        public S3ControlConfiguration config() {
            return config;
        }

        public Consumer<S3ControlClientBuilder> builderModifier() {
            return builderModifier;
        }

        public Class<?> exception() {
            return exception;
        }

        public String exceptionMessage() {
            return exceptionMessage;
        }

        public AttributeMap expectedParams() {
            return expectedParams;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private S3ControlConfiguration config;
            private Consumer<S3ControlClientBuilder> builderModifier;

            private Class<?> exception;
            private String exceptionMessage;

            private AttributeMap expectedParams;

            public Builder config(S3ControlConfiguration config) {
                this.config = config;
                return this;
            }

            public Builder builderModifier(Consumer<S3ControlClientBuilder> builderModifier) {
                this.builderModifier = builderModifier;
                return this;
            }

            public Builder exception(Class<?> exception) {
                this.exception = exception;
                return this;
            }

            public Builder exceptionMessage(String exceptionMessage) {
                this.exceptionMessage = exceptionMessage;
                return this;
            }

            private Builder expectedParams(AttributeMap expectedParams) {
                this.expectedParams = expectedParams;
                return this;
            }

            public TestCase build() {
                return new TestCase(this);
            }
        }
    }
}