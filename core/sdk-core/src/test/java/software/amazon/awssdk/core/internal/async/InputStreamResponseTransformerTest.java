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

package software.amazon.awssdk.core.internal.async;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.protocol.VoidSdkResponse;
import software.amazon.awssdk.utils.async.SimplePublisher;

class InputStreamResponseTransformerTest {
    private SimplePublisher<ByteBuffer> publisher;
    private InputStreamResponseTransformer<SdkResponse> transformer;
    private SdkResponse response;
    private CompletableFuture<ResponseInputStream<SdkResponse>> resultFuture;

    @BeforeEach
    public void setup() {
        publisher = new SimplePublisher<>();
        transformer = new InputStreamResponseTransformer<>();
        resultFuture = transformer.prepare();
        response = VoidSdkResponse.builder().build();

        transformer.onResponse(response);

        assertThat(resultFuture).isNotDone();

        transformer.onStream(SdkPublisher.adapt(publisher));

        assertThat(resultFuture).isCompleted();
        assertThat(resultFuture.join().response()).isEqualTo(response);
    }

    @Test
    public void inputStreamReadsAreFromPublisher() throws IOException {
        InputStream stream = resultFuture.join();

        publisher.send(ByteBuffer.wrap(new byte[] { 0, 1, 2 }));
        publisher.complete();

        assertThat(stream.read()).isEqualTo(0);
        assertThat(stream.read()).isEqualTo(1);
        assertThat(stream.read()).isEqualTo(2);
        assertThat(stream.read()).isEqualTo(-1);
    }

    @Test
    public void inputStreamArrayReadsAreFromPublisher() throws IOException {
        InputStream stream = resultFuture.join();

        publisher.send(ByteBuffer.wrap(new byte[] { 0, 1, 2 }));
        publisher.complete();

        byte[] data = new byte[3];
        assertThat(stream.read(data)).isEqualTo(3);

        assertThat(data[0]).isEqualTo((byte) 0);
        assertThat(data[1]).isEqualTo((byte) 1);
        assertThat(data[2]).isEqualTo((byte) 2);
        assertThat(stream.read(data)).isEqualTo(-1);
    }
}