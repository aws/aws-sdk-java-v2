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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber;
import software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber.TransferResult;

class InputStreamWithExecutorAsyncRequestBodyTest {

    private static ExecutorService customerExecutor;

    @BeforeAll
    static void setUp() {
        customerExecutor = Executors.newSingleThreadExecutor();
    }

    @AfterAll
    static void tearDown() {
        customerExecutor.shutdownNow();
    }

    static Stream<Arguments> requestBodyFactories() {
        return Stream.of(
            Arguments.of("customerProvidedExecutor",
                         (BiFunction<InputStream, Long, AsyncRequestBody>) (is, len) ->
                             AsyncRequestBody.fromInputStream(is, len, customerExecutor)),
            Arguments.of("sdkManagedExecutor_twoArgOverload",
                         (BiFunction<InputStream, Long, AsyncRequestBody>) AsyncRequestBody::fromInputStream),
            Arguments.of("sdkManagedExecutor_builderWithoutExecutor",
                         (BiFunction<InputStream, Long, AsyncRequestBody>) (is, len) ->
                             AsyncRequestBody.fromInputStream(b -> b.inputStream(is).contentLength(len)))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("requestBodyFactories")
    @Timeout(10)
    public void dataFromInputStreamIsCopied(String name,
                                            BiFunction<InputStream, Long, AsyncRequestBody> factory) throws Exception {
        PipedOutputStream os = new PipedOutputStream();
        PipedInputStream is = new PipedInputStream(os);

        InputStreamWithExecutorAsyncRequestBody asyncRequestBody =
            (InputStreamWithExecutorAsyncRequestBody) factory.apply(is, 4L);

        ByteBufferStoringSubscriber subscriber = new ByteBufferStoringSubscriber(8);
        asyncRequestBody.subscribe(subscriber);

        os.write(0);
        os.write(1);
        os.write(2);
        os.write(3);
        os.close();

        asyncRequestBody.activeWriteFuture().get();

        ByteBuffer output = ByteBuffer.allocate(8);
        assertThat(subscriber.transferTo(output)).isEqualTo(TransferResult.END_OF_STREAM);
        output.flip();

        assertThat(output.remaining()).isEqualTo(4);
        assertThat(output.get()).isEqualTo((byte) 0);
        assertThat(output.get()).isEqualTo((byte) 1);
        assertThat(output.get()).isEqualTo((byte) 2);
        assertThat(output.get()).isEqualTo((byte) 3);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("requestBodyFactories")
    @Timeout(10)
    public void errorsReadingInputStreamAreForwardedToSubscriber(String name,
                                                                  BiFunction<InputStream, Long, AsyncRequestBody> factory) throws Exception {
        PipedOutputStream os = new PipedOutputStream();
        PipedInputStream is = new PipedInputStream(os);

        is.close();

        InputStreamWithExecutorAsyncRequestBody asyncRequestBody =
            (InputStreamWithExecutorAsyncRequestBody) factory.apply(is, 4L);

        ByteBufferStoringSubscriber subscriber = new ByteBufferStoringSubscriber(8);
        asyncRequestBody.subscribe(subscriber);
        assertThatThrownBy(() -> asyncRequestBody.activeWriteFuture().get()).hasRootCauseInstanceOf(IOException.class);
        assertThatThrownBy(() -> subscriber.transferTo(ByteBuffer.allocate(8))).hasRootCauseInstanceOf(IOException.class);
    }
}
