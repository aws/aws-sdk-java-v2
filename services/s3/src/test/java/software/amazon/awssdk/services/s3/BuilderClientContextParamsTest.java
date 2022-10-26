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

package software.amazon.awssdk.services.s3;

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
import software.amazon.awssdk.services.s3.endpoints.S3ClientContextParams;
import software.amazon.awssdk.utils.AttributeMap;

public class BuilderClientContextParamsTest {
    private ExecutionInterceptor mockInterceptor;

    private static AttributeMap baseExpectedParams;

    @BeforeAll
    public static void setup() {
        // This is the set of client context params with their values the same as default values in S3Configuration
        baseExpectedParams = AttributeMap.builder()
                                         .put(S3ClientContextParams.USE_ARN_REGION, false)
                                         .put(S3ClientContextParams.DISABLE_MULTI_REGION_ACCESS_POINTS, false)
                                         .put(S3ClientContextParams.ACCELERATE, false)
                                         .put(S3ClientContextParams.FORCE_PATH_STYLE, false)
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
        S3ClientBuilder builder = S3Client.builder()
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
                S3Client s3 = builder.build();
                s3.listBuckets();
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
                    .config(S3Configuration.builder()
                                           .useArnRegionEnabled(true)
                                           .build())
                    .expectedParams(AttributeMap.builder()
                                                .put(S3ClientContextParams.USE_ARN_REGION, true)
                                                .build())
                    .build()
        );

        // UseArnRegion on builder
        testCases.add(
            TestCase.builder()
                    .builderModifier(b -> b.useArnRegion(true))
                    .expectedParams(AttributeMap.builder()
                                                .put(S3ClientContextParams.USE_ARN_REGION, true)
                                                .build())
                    .build()
        );

        // UseArnRegion on config and builder
        testCases.add(
            TestCase.builder()
                    .config(S3Configuration.builder()
                                           .useArnRegionEnabled(true)
                                           .build())
                    .builderModifier(b -> b.useArnRegion(true))
                    .exception(IllegalStateException.class)
                    .exceptionMessage("UseArnRegion")
                    .build()
        );

        // DisableMultiRegionAccessPoints on config
        testCases.add(
            TestCase.builder()
                    .config(S3Configuration.builder()
                                           .multiRegionEnabled(false)
                                           .build())
                    .expectedParams(AttributeMap.builder()
                                                .put(S3ClientContextParams.DISABLE_MULTI_REGION_ACCESS_POINTS, true)
                                                .build())
                    .build()
        );

        // DisableMultiRegionAccessPoints on builder
        testCases.add(
            TestCase.builder()
                    .builderModifier(b -> b.disableMultiRegionAccessPoints(true))
                    .expectedParams(AttributeMap.builder()
                                                .put(S3ClientContextParams.DISABLE_MULTI_REGION_ACCESS_POINTS, true)
                                                .build())
                    .build()
        );

        // DisableMultiRegionAccessPoints on config and builder
        testCases.add(
            TestCase.builder()
                    .config(S3Configuration.builder()
                                           .multiRegionEnabled(false)
                                           .build())
                    .builderModifier(b -> b.disableMultiRegionAccessPoints(true))
                    .exception(IllegalStateException.class)
                    .exceptionMessage("DisableMultiRegionAccessPoints")
                    .build()
        );

        // Accelerate on config
        testCases.add(
            TestCase.builder()
                    .config(S3Configuration.builder()
                                           .accelerateModeEnabled(true)
                                           .build())
                    .expectedParams(AttributeMap.builder()
                                                .put(S3ClientContextParams.ACCELERATE, true)
                                                .build())
                    .build()
        );

        // Accelerate on builder
        testCases.add(
            TestCase.builder()
                    .builderModifier(b -> b.accelerate(true))
                    .expectedParams(AttributeMap.builder()
                                                .put(S3ClientContextParams.ACCELERATE, true)
                                                .build())
                    .build()
        );

        // Accelerate on config and builder
        testCases.add(
            TestCase.builder()
                    .config(S3Configuration.builder()
                                           .accelerateModeEnabled(true)
                                           .build())
                    .builderModifier(b -> b.accelerate(true))
                    .exception(IllegalStateException.class)
                    .exceptionMessage("Accelerate")
                    .build()
        );

        // ForcePathStyle on config
        testCases.add(
            TestCase.builder()
                    .config(S3Configuration.builder()
                                           .pathStyleAccessEnabled(true)
                                           .build())
                    .expectedParams(AttributeMap.builder()
                                                .put(S3ClientContextParams.FORCE_PATH_STYLE, true)
                                                .build())
                    .build()
        );

        // ForcePathStyle on builder
        testCases.add(
            TestCase.builder()
                    .builderModifier(b -> b.forcePathStyle(true))
                    .expectedParams(AttributeMap.builder()
                                                .put(S3ClientContextParams.FORCE_PATH_STYLE, true)
                                                .build())
                    .build()
        );

        // ForcePathStyle on config and builder
        testCases.add(
            TestCase.builder()
                    .config(S3Configuration.builder()
                                           .pathStyleAccessEnabled(true)
                                           .build())
                    .builderModifier(b -> b.forcePathStyle(true))
                    .exception(IllegalStateException.class)
                    .exceptionMessage("ForcePathStyle")
                    .build()
        );

        return testCases;
    }

    public static class TestCase {
        private final S3Configuration config;
        private final Consumer<S3ClientBuilder> builderModifier;

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

        public S3Configuration config() {
            return config;
        }

        public Consumer<S3ClientBuilder> builderModifier() {
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
            private S3Configuration config;
            private Consumer<S3ClientBuilder> builderModifier;

            private Class<?> exception;
            private String exceptionMessage;

            private AttributeMap expectedParams;

            public Builder config(S3Configuration config) {
                this.config = config;
                return this;
            }

            public Builder builderModifier(Consumer<S3ClientBuilder> builderModifier) {
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
