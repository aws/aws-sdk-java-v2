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

package software.amazon.awssdk.services.apigateway.internal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AcceptJsonInterceptor}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AcceptJsonInterceptorTest {
    private static final AcceptJsonInterceptor interceptor = new AcceptJsonInterceptor();

    @Mock
    private Context.ModifyHttpRequest ctx;

    @Test
    public void doesNotClobberExistingValue() {
        SdkHttpRequest request = newRequest("some-value");
        Mockito.when(ctx.httpRequest()).thenReturn(request);
        request = interceptor.modifyHttpRequest(ctx, new ExecutionAttributes());
        assertThat(request.headers().get("Accept")).containsOnly("some-value");
    }

    @Test
    public void addsStandardAcceptHeaderIfMissing() {
        SdkHttpRequest request = newRequest(null);
        Mockito.when(ctx.httpRequest()).thenReturn(request);
        request = interceptor.modifyHttpRequest(ctx, new ExecutionAttributes());
        assertThat(request.headers().get("Accept")).containsOnly("application/json");
    }

    private SdkHttpFullRequest newRequest(String accept) {
        SdkHttpFullRequest.Builder builder = SdkHttpFullRequest.builder()
                .uri(URI.create("https://amazonaws.com"))
                .method(SdkHttpMethod.GET);
        if (accept != null) {
            builder.appendHeader("Accept", accept);
        }
        return builder.build();
    }
}
