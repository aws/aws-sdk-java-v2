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

package software.amazon.awssdk.core.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.internal.interceptor.DefaultFailedExecutionContext;

class OpenTelemetryErrorFilteringInterceptorTest {
    private final OpenTelemetryErrorFilteringInterceptor interceptor = new OpenTelemetryErrorFilteringInterceptor();
    private SdkRequest mockRequest;
    private InterceptorContext mockInterceptorContext;

    @BeforeEach
    void setUp() {
        mockRequest = mock(SdkRequest.class);
        mockInterceptorContext = InterceptorContext.builder().request(mockRequest).build();
        Span.clear();
        OpenTelemetryErrorFilteringInterceptor.clearIgnoredExceptions();
        System.clearProperty("aws.otel.ignoredExceptions");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("aws.otel.ignoredExceptions");
    }

    @Test
    void modifyException_ignoredException_setsSpanStatusToOk() {
        OpenTelemetryErrorFilteringInterceptor.addIgnoredExceptions(IOException.class);

        Context.FailedExecution context = DefaultFailedExecutionContext.builder()
                                                                       .interceptorContext(mockInterceptorContext)
                                                                       .exception(new IOException("Expected error"))
                                                                       .build();

        interceptor.modifyException(context, new ExecutionAttributes());

        assertThat(Span.getLastStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    void modifyException_nonIgnoredException_doesNotSetSpanStatus() {
        OpenTelemetryErrorFilteringInterceptor.addIgnoredExceptions(IOException.class);

        Context.FailedExecution context = DefaultFailedExecutionContext.builder()
                                                                       .interceptorContext(mockInterceptorContext)
                                                                       .exception(new RuntimeException("Unexpected error"))
                                                                       .build();

        interceptor.modifyException(context, new ExecutionAttributes());

        assertThat(Span.getLastStatus()).isNull();
    }

    @Test
    void modifyException_ignoredExceptionInChain_setsSpanStatusToOk() {
        OpenTelemetryErrorFilteringInterceptor.addIgnoredExceptions(IOException.class);

        RuntimeException wrappingException = new RuntimeException("Wrapped", new IOException("Root cause"));
        Context.FailedExecution context = DefaultFailedExecutionContext.builder()
                                                                       .interceptorContext(mockInterceptorContext)
                                                                       .exception(wrappingException)
                                                                       .build();

        interceptor.modifyException(context, new ExecutionAttributes());

        assertThat(Span.getLastStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    void modifyException_systemPropertyConfiguration_loadsExceptionsAndSetsSpanStatusToOk() {
        System.setProperty("aws.otel.ignoredExceptions", "java.io.IOException, java.lang.IllegalArgumentException");
        OpenTelemetryErrorFilteringInterceptor.initializeFromSystemSettings();

        Context.FailedExecution context = DefaultFailedExecutionContext.builder()
                                                                       .interceptorContext(mockInterceptorContext)
                                                                       .exception(new IllegalArgumentException("Invalid argument"))
                                                                       .build();

        interceptor.modifyException(context, new ExecutionAttributes());

        assertThat(Span.getLastStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    void modifyException_emptyIgnoredExceptions_doesNotSetSpanStatus() {
        Context.FailedExecution context = DefaultFailedExecutionContext.builder()
                                                                       .interceptorContext(mockInterceptorContext)
                                                                       .exception(new IOException("Expected error"))
                                                                       .build();

        interceptor.modifyException(context, new ExecutionAttributes());

        assertThat(Span.getLastStatus()).isNull();
    }
}
