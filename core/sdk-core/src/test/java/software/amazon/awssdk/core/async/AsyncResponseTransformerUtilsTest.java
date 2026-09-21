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

package software.amazon.awssdk.core.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.SplittingTransformerConfiguration;
import software.amazon.awssdk.core.internal.async.ConfigurableAsyncResponseTransformer;
import software.amazon.awssdk.core.internal.async.InputStreamResponseTransformer;
import software.amazon.awssdk.core.protocol.VoidSdkResponse;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.async.SimplePublisher;

class AsyncResponseTransformerUtilsTest {

    @Test
    void wrapWithEndOfStreamFuture_whenSplitCalled_delegatesToOriginalTransformer() {
        AsyncResponseTransformer<Object, Object> transformer = mock(AsyncResponseTransformer.class);
        AsyncResponseTransformer.SplitResult<Object, Object> splitResult =
            mock(AsyncResponseTransformer.SplitResult.class);
        SplittingTransformerConfiguration splitConfiguration =
            SplittingTransformerConfiguration.builder().bufferSizeInBytes(1024L).build();
        when(transformer.split(splitConfiguration)).thenReturn(splitResult);

        Pair<AsyncResponseTransformer<Object, Object>, ?> wrapped =
            AsyncResponseTransformerUtils.wrapWithEndOfStreamFuture(transformer);

        assertThat(wrapped.left()).isNotSameAs(transformer);
        assertThat(wrapped.left().split(splitConfiguration)).isSameAs(splitResult);
        verify(transformer).split(splitConfiguration);
    }

    @Test
    void configure_whenBlockingTransformerWrapped_forwardsConcatenatedGzipConfigurationToDelegate() throws IOException {
        AsyncResponseTransformer<SdkResponse, ResponseInputStream<SdkResponse>> wrapped =
            AsyncResponseTransformerUtils.wrapWithEndOfStreamFuture(new InputStreamResponseTransformer<SdkResponse>()).left();

        AsyncResponseTransformer<SdkResponse, ResponseInputStream<SdkResponse>> configured =
            ConfigurableAsyncResponseTransformer.configure(wrapped, false);

        assertThat(configured).isNotSameAs(wrapped);
        assertThat(availableAfterGzipHeader(configured)).isZero();
    }

    private static int availableAfterGzipHeader(
        AsyncResponseTransformer<SdkResponse, ResponseInputStream<SdkResponse>> transformer) throws IOException {
        SimplePublisher<ByteBuffer> body = new SimplePublisher<>();
        CompletableFuture<ResponseInputStream<SdkResponse>> future = transformer.prepare();
        transformer.onResponse(VoidSdkResponse.builder().build());
        transformer.onStream(SdkPublisher.adapt(body));
        ResponseInputStream<SdkResponse> stream = future.join();
        body.send(ByteBuffer.wrap(new byte[] {0x1f, (byte) 0x8b, 0x08}));
        stream.read();
        stream.read();
        stream.read();
        int result = stream.available();
        body.complete();
        stream.close();
        return result;
    }
}
