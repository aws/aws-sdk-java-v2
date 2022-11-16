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

package software.amazon.awssdk.services.protocolquery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.IoUtils;

public class MoveQueryParamsToBodyTest {
    private static final AwsCredentialsProvider CREDENTIALS = StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));

    private SdkHttpClient mockHttpClient;

    private ProtocolQueryClient client;

    @BeforeEach
    public void setup() throws IOException {
        mockHttpClient = mock(SdkHttpClient.class);
        ExecutableHttpRequest mockRequest = mock(ExecutableHttpRequest.class);
        when(mockRequest.call()).thenThrow(new IOException("IO error!"));
        when(mockHttpClient.prepareRequest(any())).thenReturn(mockRequest);
    }

    @AfterEach
    public void teardown() {
        if (client != null) {
            client.close();
        }
        client = null;
    }

    @Test
    public void customInterceptor_additionalQueryParamsAdded_paramsAlsoMovedToBody() throws IOException {
        client = ProtocolQueryClient.builder()
                                    .overrideConfiguration(o -> o.addExecutionInterceptor(new AdditionalQueryParamInterceptor()))
                                    .region(Region.US_WEST_2)
                                    .credentialsProvider(CREDENTIALS)
                                    .httpClient(mockHttpClient)
                                    .build();

        ArgumentCaptor<HttpExecuteRequest> requestCaptor = ArgumentCaptor.forClass(HttpExecuteRequest.class);

        assertThatThrownBy(() -> client.membersInQueryParams(r -> r.stringQueryParam("hello")))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("IO");

        verify(mockHttpClient, atLeast(1)).prepareRequest(requestCaptor.capture());

        ContentStreamProvider requestContent = requestCaptor.getValue().contentStreamProvider().get();

        String contentString = IoUtils.toUtf8String(requestContent.newStream());

        assertThat(contentString).contains("CustomParamName=CustomParamValue");
    }

    private static class AdditionalQueryParamInterceptor implements ExecutionInterceptor {
        @Override
        public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
            return context.httpRequest().toBuilder()
                                        .putRawQueryParameter("CustomParamName", "CustomParamValue")
                                        .build();
        }
    }
}
