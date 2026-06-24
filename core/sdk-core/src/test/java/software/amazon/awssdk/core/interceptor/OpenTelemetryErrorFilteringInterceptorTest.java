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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.http.SdkHttpResponse;

class OpenTelemetryErrorFilteringInterceptorTest {
    private final OpenTelemetryErrorFilteringInterceptor interceptor = new OpenTelemetryErrorFilteringInterceptor();
    private SdkRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockRequest = mock(SdkRequest.class);
        Span.clear();
    }

    @Test
    void afterTransmission_status400_setsSpanStatusToOk() {
        InterceptorContext context = InterceptorContext.builder()
                                                       .request(mockRequest)
                                                       .httpResponse(SdkHttpResponse.builder().statusCode(400).build())
                                                       .build();

        interceptor.afterTransmission(context, new ExecutionAttributes());

        assertThat(Span.getLastStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    void afterTransmission_status499_setsSpanStatusToOk() {
        InterceptorContext context = InterceptorContext.builder()
                                                       .request(mockRequest)
                                                       .httpResponse(SdkHttpResponse.builder().statusCode(499).build())
                                                       .build();

        interceptor.afterTransmission(context, new ExecutionAttributes());

        assertThat(Span.getLastStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    void afterTransmission_status200_doesNotSetSpanStatus() {
        InterceptorContext context = InterceptorContext.builder()
                                                       .request(mockRequest)
                                                       .httpResponse(SdkHttpResponse.builder().statusCode(200).build())
                                                       .build();

        interceptor.afterTransmission(context, new ExecutionAttributes());

        assertThat(Span.getLastStatus()).isNull();
    }

    @Test
    void afterTransmission_status500_doesNotSetSpanStatus() {
        InterceptorContext context = InterceptorContext.builder()
                                                       .request(mockRequest)
                                                       .httpResponse(SdkHttpResponse.builder().statusCode(500).build())
                                                       .build();

        interceptor.afterTransmission(context, new ExecutionAttributes());

        assertThat(Span.getLastStatus()).isNull();
    }
}
