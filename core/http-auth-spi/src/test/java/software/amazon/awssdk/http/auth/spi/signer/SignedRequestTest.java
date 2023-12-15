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

package software.amazon.awssdk.http.auth.spi.signer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.StringInputStream;

public class SignedRequestTest {

    @Test
    public void createSignedRequest_missingRequest_throwsException() {
        assertThatThrownBy(() -> SignedRequest.builder().build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("request must not be null");
    }

    @Test
    public void createSignedRequest_minimalBuild_works() {
        SdkHttpRequest request = mock(SdkHttpRequest.class);
        SignedRequest signedRequest = SignedRequest.builder()
                                                   .request(request)
                                                   .build();

        assertNotNull(signedRequest);
        assertThat(signedRequest.request()).isEqualTo(request);
    }

    @Test
    public void createSignedRequest_maximalBuild_works() {
        SdkHttpRequest request = mock(SdkHttpRequest.class);
        SignedRequest signedRequest = SignedRequest.builder()
                                                   .request(request)
                                                   .payload(() -> new StringInputStream("test"))
                                                   .build();

        assertNotNull(signedRequest);
        assertThat(signedRequest.request()).isEqualTo(request);
        assertThat(signedRequest.payload()).isPresent();
    }

    @Test
    public void createSignedRequest_toBuilder_works() {
        SdkHttpRequest request = mock(SdkHttpRequest.class);
        SignedRequest signedRequest = SignedRequest.builder()
                                                   .request(request)
                                                   .payload(() -> new StringInputStream("test"))
                                                   .build();

        SignedRequest copy = signedRequest.toBuilder().build();

        assertNotNull(copy);
        assertThat(copy.request()).isEqualTo(request);
        assertThat(copy.payload()).isPresent();
    }

    @Test
    public void createSignedRequest_copyNoChange_works() {
        SdkHttpRequest request = mock(SdkHttpRequest.class);
        SignedRequest signedRequest = SignedRequest.builder()
                                                   .request(request)
                                                   .payload(() -> new StringInputStream("test"))
                                                   .build();

        SignedRequest copy = signedRequest.copy(r -> {});

        assertNotNull(copy);
        assertThat(copy.request()).isEqualTo(request);
        assertThat(copy.payload()).isPresent();
    }

    @Test
    public void createSignedRequest_copyWithChange_works() {
        SdkHttpRequest firstRequest = mock(SdkHttpRequest.class);
        SdkHttpRequest secondRequest = mock(SdkHttpRequest.class);
        SignedRequest signedRequest = SignedRequest.builder()
                                                   .request(firstRequest)
                                                   .payload(() -> new StringInputStream("test"))
                                                   .build();

        SignedRequest copy = signedRequest.copy(r -> r.request(secondRequest));

        assertNotNull(copy);
        assertThat(copy.request()).isEqualTo(secondRequest);
        assertThat(copy.payload()).isPresent();
    }
}
