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

package software.amazon.awssdk.core.internal.http.pipeline.stages;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.ExceptionReportingUtils;
import software.amazon.awssdk.core.signer.NoOpSigner;
import utils.ValidSdkObjects;

public class ExceptionReportingUtilsTest {

    @Test
    public void onExecutionFailureThrowException_shouldSwallow() {
        RequestExecutionContext context = context(new ThrowErrorOnExecutionFailureInterceptor());

        assertThat(ExceptionReportingUtils.reportFailureToInterceptors(context, SdkClientException.create("test")))
            .isExactlyInstanceOf(SdkClientException.class);
    }

    @Test
    public void modifyException_shouldReturnModifiedException() {
        ApiCallTimeoutException modifiedException = ApiCallTimeoutException.create(1000);
        RequestExecutionContext context = context(new ModifyExceptionInterceptor(modifiedException));
        assertThat(ExceptionReportingUtils.reportFailureToInterceptors(context, SdkClientException.create("test")))
            .isEqualTo(modifiedException);
    }

    public RequestExecutionContext context(ExecutionInterceptor... executionInterceptors) {
        List<ExecutionInterceptor> interceptors = Arrays.asList(executionInterceptors);
        ExecutionInterceptorChain executionInterceptorChain = new ExecutionInterceptorChain(interceptors);
        return RequestExecutionContext.builder()
                                      .executionContext(ExecutionContext.builder()
                                                                        .signer(new NoOpSigner())
                                                                        .executionAttributes(new ExecutionAttributes())
                                                                        .interceptorContext(InterceptorContext.builder()
                                                                                                              .request(ValidSdkObjects.sdkRequest())
                                                                                                              .build())
                                                                        .interceptorChain(executionInterceptorChain)
                                                                        .build())
                                      .originalRequest(ValidSdkObjects.sdkRequest())
                                      .build();
    }

    private static class ThrowErrorOnExecutionFailureInterceptor implements ExecutionInterceptor {

        @Override
        public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
            throw new RuntimeException("OOPS");
        }
    }

    private static class ModifyExceptionInterceptor implements ExecutionInterceptor {

        private final Exception exceptionToThrow;

        private ModifyExceptionInterceptor(Exception exceptionToThrow) {
            this.exceptionToThrow = exceptionToThrow;
        }

        @Override
        public Throwable modifyException(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
            return exceptionToThrow;
        }
    }
}
