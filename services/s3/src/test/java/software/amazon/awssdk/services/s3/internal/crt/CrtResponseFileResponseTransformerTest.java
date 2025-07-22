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

package software.amazon.awssdk.services.s3.internal.crt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.SdkPublisher;

public class CrtResponseFileResponseTransformerTest {
    private CrtResponseFileResponseTransformer<SdkResponse> transformer;
    private SdkResponse response;
    private SdkPublisher<ByteBuffer> publisher;

    @BeforeEach
    public void setUp() throws Exception {
        transformer = new CrtResponseFileResponseTransformer<>();
        response = Mockito.mock(SdkResponse.class);
        publisher = AsyncRequestBody.fromString("");
    }

    @Test
    void successfulResponseAndStream_returnsResponsePublisher() throws Exception {
        CompletableFuture<SdkResponse> responseFuture = transformer.prepare();
        transformer.onResponse(response);
        assertThat(responseFuture.isDone()).isFalse();
        transformer.onStream(publisher);
        assertThat(responseFuture.isDone()).isTrue();
        SdkResponse returnedResponse = responseFuture.get();
        assertThat(returnedResponse).isEqualTo(response);
    }

    @Test
    void failedResponse_completesExceptionally() {
        CompletableFuture<SdkResponse> responseFuture = transformer.prepare();
        assertThat(responseFuture.isDone()).isFalse();
        transformer.exceptionOccurred(new RuntimeException("Intentional exception for testing purposes - before response."));
        assertThat(responseFuture.isDone()).isTrue();
        assertThatThrownBy(responseFuture::get)
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void failedStream_completesExceptionally() {
        CompletableFuture<SdkResponse> responseFuture = transformer.prepare();
        transformer.onResponse(response);
        assertThat(responseFuture.isDone()).isFalse();
        transformer.exceptionOccurred(new RuntimeException("Intentional exception for testing purposes - after response."));
        assertThat(responseFuture.isDone()).isTrue();
        assertThatThrownBy(responseFuture::get)
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(RuntimeException.class);
    }

}
