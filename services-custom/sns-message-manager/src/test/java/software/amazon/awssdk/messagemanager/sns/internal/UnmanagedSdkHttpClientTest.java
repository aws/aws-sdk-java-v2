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

package software.amazon.awssdk.messagemanager.sns.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;

public class UnmanagedSdkHttpClientTest {
    private SdkHttpClient mockHttpClient;

    @BeforeEach
    void setup() {
        mockHttpClient = mock(SdkHttpClient.class);
    }

    @Test
    void close_doesNotCloseDelegate() {
        UnmanagedSdkHttpClient unmanaged = new UnmanagedSdkHttpClient(mockHttpClient);
        unmanaged.close();
        verifyNoInteractions(mockHttpClient);
    }

    @Test
    void prepareRequest_delegatesToRealClient() {
        UnmanagedSdkHttpClient unmanaged = new UnmanagedSdkHttpClient(mockHttpClient);
        HttpExecuteRequest request = HttpExecuteRequest.builder().build();

        unmanaged.prepareRequest(request);

        ArgumentCaptor<HttpExecuteRequest> requestCaptor = ArgumentCaptor.forClass(HttpExecuteRequest.class);

        verify(mockHttpClient).prepareRequest(requestCaptor.capture());
        assertThat(requestCaptor.getValue()).isSameAs(request);
    }
}
