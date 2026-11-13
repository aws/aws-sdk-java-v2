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

package software.amazon.awssdk.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.http.ConnectionPoolShutdownException;
import software.amazon.awssdk.http.ConnectionPoolShutdownException.Reason;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;

/**
 * Pins what {@link ConnectionPoolShutdownException}'s Javadoc promises: a synchronous service client neither retries it
 * nor wraps it, so callers receive the instance the HTTP client threw.
 */
public class ConnectionPoolShutdownExceptionPropagationTest {

    private SdkHttpClient httpClient;
    private ExecutableHttpRequest call;
    private ProtocolRestJsonClient client;

    @BeforeEach
    public void setup() {
        httpClient = mock(SdkHttpClient.class);
        call = mock(ExecutableHttpRequest.class);
        when(httpClient.prepareRequest(any())).thenReturn(call);
        client = ProtocolRestJsonClient.builder()
                                       .httpClient(httpClient)
                                       .region(Region.US_EAST_1)
                                       .credentialsProvider(AnonymousCredentialsProvider.create())
                                       .overrideConfiguration(o -> o.retryStrategy(RetryMode.STANDARD))
                                       .build();
    }

    @AfterEach
    public void teardown() {
        client.close();
    }

    @ParameterizedTest
    @EnumSource(Reason.class)
    public void syncCallService_httpClientThrowsConnectionPoolShutdown_propagatesSameInstanceWithoutRetry(Reason reason)
            throws Exception {
        ConnectionPoolShutdownException thrown = ConnectionPoolShutdownException.create(reason, null);
        when(call.call()).thenThrow(thrown);

        assertThat(catchThrowable(() -> client.allTypes(r -> { }))).isSameAs(thrown);
        verify(call, times(1)).call();
    }

    /** Shows that this setup can observe retries, so the single call above is meaningful. */
    @Test
    public void syncCallService_httpClientThrowsIOException_isRetried() throws Exception {
        when(call.call()).thenThrow(new IOException("connection reset"));

        assertThat(catchThrowable(() -> client.allTypes(r -> { }))).isInstanceOf(SdkClientException.class);
        verify(call, times(3)).call();
    }
}
