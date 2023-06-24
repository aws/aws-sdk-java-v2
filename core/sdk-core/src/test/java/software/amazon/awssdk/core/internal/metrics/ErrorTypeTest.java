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

package software.amazon.awssdk.core.internal.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;

public class ErrorTypeTest {

    @ParameterizedTest
    @MethodSource("testCases")
    public void fromException_mapsToCorrectType(TestCase tc) {
        assertThat(SdkErrorType.fromException(tc.thrown)).isEqualTo(tc.expectedType);
    }

    private static Stream<? extends TestCase> testCases() {
        return Stream.of(
            tc(new IOException("I/O"), SdkErrorType.IO),

            tc(TestServiceException.builder().build(), SdkErrorType.SERVER_ERROR),
            tc(TestServiceException.builder().throttling(true).build(), SdkErrorType.THROTTLING),

            tc(ApiCallAttemptTimeoutException.builder().message("Attempt timeout").build(), SdkErrorType.CONFIGURED_TIMEOUT),
            tc(ApiCallTimeoutException.builder().message("Call timeout").build(), SdkErrorType.CONFIGURED_TIMEOUT),

            tc(SdkClientException.create("Unmarshalling error"), SdkErrorType.OTHER),

            tc(new OutOfMemoryError("OOM"), SdkErrorType.OTHER)
        );
    }

    private static TestCase tc(Throwable thrown, SdkErrorType expectedType) {
        return new TestCase(thrown, expectedType);
    }

    private static class TestCase {
        private final Throwable thrown;
        private final SdkErrorType expectedType;

        public TestCase(Throwable thrown, SdkErrorType expectedType) {
            this.thrown = thrown;
            this.expectedType = expectedType;
        }
    }

    private static class TestServiceException extends SdkServiceException {
        private final boolean throttling;

        protected TestServiceException(BuilderImpl b) {
            super(b);
            this.throttling = b.throttling;
        }

        @Override
        public boolean isThrottlingException() {
            return throttling;
        }

        public static Builder builder() {
            return new BuilderImpl();
        }

        public interface Builder extends SdkServiceException.Builder {
            Builder throttling(Boolean throttling);

            @Override
            TestServiceException build();
        }

        public static class BuilderImpl extends SdkServiceException.BuilderImpl implements Builder {
            private boolean throttling;

            @Override
            public boolean equalsBySdkFields(Object other) {
                return super.equalsBySdkFields(other);
            }

            @Override
            public Builder throttling(Boolean throttling) {
                this.throttling = throttling;
                return this;
            }

            @Override
            public TestServiceException build() {
                return new TestServiceException(this);
            }
        }
    }
}
