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

package software.amazon.awssdk.core.internal.http.timers;

import java.util.Collections;

import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.metrics.MetricCollector;

/**
 * Useful utilities for verifying behavior of the client execution timeout and request timeout features.
 */
public class ClientExecutionAndRequestTimerTestUtils {

    public static ExecutionContext executionContext(SdkHttpFullRequest request) {
        InterceptorContext incerceptorContext =
                InterceptorContext.builder()
                                  .request(NoopTestRequest.builder().build())
                                  .httpRequest(request)
                                  .build();
        return ExecutionContext.builder()
                               .signer(new NoOpSigner())
                               .interceptorChain(new ExecutionInterceptorChain(Collections.emptyList()))
                               .executionAttributes(new ExecutionAttributes())
                               .interceptorContext(incerceptorContext)
                               .metricCollector(MetricCollector.create("ApiCall"))
                               .build();
    }
}
