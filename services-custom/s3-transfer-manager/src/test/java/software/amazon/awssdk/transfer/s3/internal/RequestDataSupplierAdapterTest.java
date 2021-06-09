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

package software.amazon.awssdk.transfer.s3.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.reactivex.Flowable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.async.AsyncRequestBody;

public class RequestDataSupplierAdapterTest {

    @Test
    public void getRequestData_fillsInputBuffer_publisherBuffersAreSmaller() {
        int inputBufferSize = 16;

        List<ByteBuffer> data = Stream.generate(() -> (byte) 42)
                .limit(inputBufferSize)
                .map(b -> {
                    ByteBuffer bb = ByteBuffer.allocate(1);
                    bb.put(b);
                    bb.flip();
                    return bb;
                })
                .collect(Collectors.toList());

        AsyncRequestBody requestBody = AsyncRequestBody.fromPublisher(Flowable.fromIterable(data));

        RequestDataSupplierAdapter adapter = new RequestDataSupplierAdapter(requestBody);

        ByteBuffer inputBuffer = ByteBuffer.allocate(inputBufferSize);
        adapter.getRequestBytes(inputBuffer);

        assertThat(inputBuffer.remaining()).isEqualTo(0);
    }

    @Test
    public void getRequestData_fillsInputBuffer_publisherBuffersAreLarger() {
        int bodySize = 16;

        ByteBuffer data = ByteBuffer.allocate(bodySize);
        data.put(new byte[bodySize]);
        data.flip();

        AsyncRequestBody requestBody = AsyncRequestBody.fromPublisher(Flowable.just(data));

        RequestDataSupplierAdapter adapter = new RequestDataSupplierAdapter(requestBody);

        ByteBuffer inputBuffer = ByteBuffer.allocate(1);

        for (int i = 0; i < bodySize; ++i) {
            adapter.getRequestBytes(inputBuffer);
            assertThat(inputBuffer.remaining()).isEqualTo(0);
            inputBuffer.flip();
        }
    }

    @Test
    public void getRequestData_publisherThrows_surfacesException() {
        Publisher<ByteBuffer> errorPublisher = Flowable.error(new RuntimeException("Something wrong happened"));

        AsyncRequestBody requestBody = AsyncRequestBody.fromPublisher(errorPublisher);
        RequestDataSupplierAdapter adapter = new RequestDataSupplierAdapter(requestBody);

        assertThatThrownBy(() -> adapter.getRequestBytes(ByteBuffer.allocate(16)))
                  .isInstanceOf(RuntimeException.class)
                  .hasMessageContaining("Something wrong happened");
    }

    @Test
    public void getRequestData_publisherThrows_wrapsExceptionIfNotRuntimeException() {
        Publisher<ByteBuffer> errorPublisher = Flowable.error(new IOException("Some I/O error happened"));

        AsyncRequestBody requestBody = AsyncRequestBody.fromPublisher(errorPublisher);
        RequestDataSupplierAdapter adapter = new RequestDataSupplierAdapter(requestBody);

        assertThatThrownBy(() -> adapter.getRequestBytes(ByteBuffer.allocate(16)))
                  .isInstanceOf(RuntimeException.class)
                  .hasCauseInstanceOf(IOException.class);
    }

    @Test
    public void resetMidStream_discardsBufferedData() {
        long requestSize = RequestDataSupplierAdapter.DEFAULT_REQUEST_SIZE;
        int inputBufferSize = 16;

        Publisher<ByteBuffer> requestBody = new Publisher<ByteBuffer>() {
            private byte value = 0;

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
                byte byteVal = value++;

                List<ByteBuffer> dataList = Stream.generate(() -> {
                    byte[] data = new byte[inputBufferSize];
                    Arrays.fill(data, byteVal);
                    return ByteBuffer.wrap(data);
                })
                        .limit(requestSize)
                        .collect(Collectors.toList());

                Flowable<ByteBuffer> realPublisher = Flowable.fromIterable(dataList);

                realPublisher.subscribe(subscriber);
            }
        };

        RequestDataSupplierAdapter adapter = new RequestDataSupplierAdapter(requestBody);

        long resetAfter = requestSize / 2;

        ByteBuffer inputBuffer = ByteBuffer.allocate(inputBufferSize);

        for (long l = 0; l < resetAfter; ++l) {
            adapter.getRequestBytes(inputBuffer);
            inputBuffer.flip();
        }

        adapter.resetPosition();

        byte[] expectedBufferContent = new byte[inputBufferSize];
        Arrays.fill(expectedBufferContent, (byte) 1);

        byte[] readBuffer = new byte[inputBufferSize];
        for (int l = 0; l < requestSize; ++l) {
            adapter.getRequestBytes(inputBuffer);
            // flip for reading
            inputBuffer.flip();
            inputBuffer.get(readBuffer);

            // flip for writing
            inputBuffer.flip();

            assertThat(readBuffer).isEqualTo(expectedBufferContent);
        }
    }
}
