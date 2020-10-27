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

package software.amazon.awssdk.services.waiters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.waiters.WaiterState;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.restjsonwithwaiters.waiters.internal.WaitersRuntime.ResponseStatusAcceptor;

/**
 * Verify the accuracy of {@link ResponseStatusAcceptor}.
 */
public class WaitersRuntimeResponseStatusAcceptorTest {
    @Test
    public void usesStatus() {
        assertThat(new ResponseStatusAcceptor(200, WaiterState.RETRY).waiterState()).isEqualTo(WaiterState.RETRY);
        assertThat(new ResponseStatusAcceptor(200, WaiterState.FAILURE).waiterState()).isEqualTo(WaiterState.FAILURE);
        assertThat(new ResponseStatusAcceptor(200, WaiterState.SUCCESS).waiterState()).isEqualTo(WaiterState.SUCCESS);
    }

    @Test
    public void checksStatusOnResponse() {
        SdkHttpFullResponse http200 = SdkHttpResponse.builder().statusCode(200).build();
        SdkHttpFullResponse http500 = SdkHttpResponse.builder().statusCode(500).build();

        assertThat(new ResponseStatusAcceptor(200, WaiterState.SUCCESS).matches(new ExampleSdkResponse(http200))).isTrue();
        assertThat(new ResponseStatusAcceptor(200, WaiterState.SUCCESS).matches(new ExampleSdkResponse(http500))).isFalse();
        assertThat(new ResponseStatusAcceptor(500, WaiterState.SUCCESS).matches(new ExampleSdkResponse(http500))).isTrue();
        assertThat(new ResponseStatusAcceptor(500, WaiterState.SUCCESS).matches(new ExampleSdkResponse(http200))).isFalse();
    }

    @Test
    public void checksStatusOnException() {
        assertThat(new ResponseStatusAcceptor(200, WaiterState.SUCCESS).matches((Throwable) null)).isFalse();
        assertThat(new ResponseStatusAcceptor(200, WaiterState.SUCCESS).matches(new Throwable())).isFalse();
        assertThat(new ResponseStatusAcceptor(200, WaiterState.SUCCESS).matches(SdkException.create("", null))).isFalse();
        assertThat(new ResponseStatusAcceptor(200, WaiterState.SUCCESS).matches(SdkServiceException.create("", null))).isFalse();
        assertThat(new ResponseStatusAcceptor(200, WaiterState.SUCCESS).matches(SdkServiceException.builder()
                                                                                                   .message("")
                                                                                                   .statusCode(500)
                                                                                                   .build()))
            .isFalse();
        assertThat(new ResponseStatusAcceptor(200, WaiterState.SUCCESS).matches(SdkServiceException.builder()
                                                                                                   .message("")
                                                                                                   .statusCode(200)
                                                                                                   .build()))
            .isTrue();
    }

    private static class ExampleSdkResponse extends SdkResponse {
        protected ExampleSdkResponse(SdkHttpResponse httpResponse) {
            super(new Builder() {
                @Override
                public Builder sdkHttpResponse(SdkHttpResponse sdkHttpResponse) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public SdkHttpResponse sdkHttpResponse() {
                    return httpResponse;
                }

                @Override
                public SdkResponse build() {
                    throw new UnsupportedOperationException();
                }
            });
        }

        @Override
        public Builder toBuilder() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            throw new UnsupportedOperationException();
        }
    }
}
