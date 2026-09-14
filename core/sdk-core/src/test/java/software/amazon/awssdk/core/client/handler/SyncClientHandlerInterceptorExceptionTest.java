/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package software.amazon.awssdk.core.client.handler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.protocol.VoidSdkResponse;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import utils.HttpTestUtils;
import utils.ValidSdkObjects;

@RunWith(Parameterized.class)
public class SyncClientHandlerInterceptorExceptionTest {
    private final SdkRequest request = mock(SdkRequest.class);
    private final SdkHttpClient httpClient = mock(SdkHttpClient.class);
    private final ExecutableHttpRequest httpClientCall = mock(ExecutableHttpRequest.class);
    private final Marshaller<SdkRequest> marshaller = mock(Marshaller.class);
    private final HttpResponseHandler<SdkResponse> responseHandler = mock(HttpResponseHandler.class);
    private final HttpResponseHandler<SdkServiceException> errorResponseHandler = mock(HttpResponseHandler.class);
    private final InputStream responseBody = mock(InputStream.class);

    private final Hook hook;
    private SdkSyncClientHandler clientHandler;

    @Parameterized.Parameters(name = "Interceptor Hook: {0}")
    public static Collection<Object[]> data() {
        return Arrays.stream(Hook.values())
                     .map(hook -> new Object[] {hook})
                     .collect(Collectors.toList());
    }

    public SyncClientHandlerInterceptorExceptionTest(Hook hook) {
        this.hook = hook;
    }

    @Before
    public void setUp() throws Exception {
        clientHandler = new SdkSyncClientHandler(clientConfiguration());

        when(request.overrideConfiguration()).thenReturn(Optional.empty());
        when(marshaller.marshall(request)).thenReturn(ValidSdkObjects.sdkHttpFullRequest().build());
        when(httpClient.prepareRequest(any())).thenReturn(httpClientCall);
        when(httpClientCall.call()).thenReturn(HttpExecuteResponse.builder()
                                                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                                                  .responseBody(AbortableInputStream.create(responseBody))
                                                                  .build());
        when(responseHandler.handle(any(), any())).thenReturn(VoidSdkResponse.builder().build());
    }

    @Test
    public void responseInterceptorFailureClosesResponseBody() throws Exception {
        assertThatThrownBy(() -> clientHandler.execute(clientExecutionParams()))
            .hasMessage(hook.name());

        verify(responseBody).close();
    }

    private SdkClientConfiguration clientConfiguration() {
        return HttpTestUtils.testClientConfiguration().toBuilder()
                            .option(SdkClientOption.EXECUTION_INTERCEPTORS, Collections.singletonList(hook.interceptor()))
                            .option(SdkClientOption.SYNC_HTTP_CLIENT, httpClient)
                            .option(SdkClientOption.RETRY_STRATEGY, DefaultRetryStrategy.doNotRetry())
                            .build();
    }

    private ClientExecutionParams<SdkRequest, SdkResponse> clientExecutionParams() {
        return new ClientExecutionParams<SdkRequest, SdkResponse>()
            .withInput(request)
            .withMarshaller(marshaller)
            .withResponseHandler(responseHandler)
            .withErrorResponseHandler(errorResponseHandler);
    }

    private enum Hook {
        AFTER_TRANSMISSION(new ExecutionInterceptor() {
            @Override
            public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {
                throw new RuntimeException(AFTER_TRANSMISSION.name());
            }
        }),

        MODIFY_HTTP_RESPONSE(new ExecutionInterceptor() {
            @Override
            public SdkHttpResponse modifyHttpResponse(Context.ModifyHttpResponse context,
                                                       ExecutionAttributes executionAttributes) {
                throw new RuntimeException(MODIFY_HTTP_RESPONSE.name());
            }
        }),

        MODIFY_HTTP_RESPONSE_CONTENT(new ExecutionInterceptor() {
            @Override
            public Optional<InputStream> modifyHttpResponseContent(Context.ModifyHttpResponse context,
                                                                     ExecutionAttributes executionAttributes) {
                throw new RuntimeException(MODIFY_HTTP_RESPONSE_CONTENT.name());
            }
        }),

        BEFORE_UNMARSHALLING(new ExecutionInterceptor() {
            @Override
            public void beforeUnmarshalling(Context.BeforeUnmarshalling context,
                                            ExecutionAttributes executionAttributes) {
                throw new RuntimeException(BEFORE_UNMARSHALLING.name());
            }
        });

        private final ExecutionInterceptor interceptor;

        Hook(ExecutionInterceptor interceptor) {
            this.interceptor = interceptor;
        }

        private ExecutionInterceptor interceptor() {
            return interceptor;
        }
    }
}
