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

package software.amazon.awssdk.services.s3.utils;

import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.core.internal.async.FileAsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Contains the {@link AsyncResponseTransformer} to be used in a test as well as logic on how to retrieve the body content of the
 * request for that specific transformer.
 *
 * @param <T> the type returned of the future associated with the {@link AsyncResponseTransformer}
 */
public interface AsyncResponseTransformerTestSupplier<T> {

    class ByteTestArtSupplier implements AsyncResponseTransformerTestSupplier<ResponseBytes<GetObjectResponse>> {

        @Override
        public byte[] body(ResponseBytes<GetObjectResponse> response) {
            return response.asByteArray();
        }

        @Override
        public AsyncResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>> transformer() {
            return AsyncResponseTransformer.toBytes();
        }

        @Override
        public String toString() {
            return "AsyncResponseTransformer.toBytes";
        }
    }

    class InputStreamArtSupplier implements AsyncResponseTransformerTestSupplier<ResponseInputStream<GetObjectResponse>> {

        @Override
        public byte[] body(ResponseInputStream<GetObjectResponse> response) {
            try {
                return IoUtils.toByteArray(response);
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        }

        @Override
        public AsyncResponseTransformer<GetObjectResponse, ResponseInputStream<GetObjectResponse>> transformer() {
            return AsyncResponseTransformer.toBlockingInputStream();
        }

        @Override
        public String toString() {
            return "AsyncResponseTransformer.toBlockingInputStream";
        }
    }

    class FileArtSupplier implements AsyncResponseTransformerTestSupplier<GetObjectResponse> {

        private Path path;

        @Override
        public byte[] body(GetObjectResponse response) {
            try {
                return Files.readAllBytes(path);
            } catch (IOException ioe) {
                fail("unexpected IOE during test", ioe);
                return new byte[0];
            }
        }

        @Override
        public AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> transformer() {
            FileSystem jimfs = Jimfs.newFileSystem();
            String filePath = "/tmp-file-" + UUID.randomUUID();
            this.path = jimfs.getPath(filePath);
            return AsyncResponseTransformer.toFile(this.path);
        }

        @Override
        public String toString() {
            return "AsyncResponseTransformer.toFile";
        }

        @Override
        public boolean requiresJimfs() {
            return true;
        }
    }

    class PublisherArtSupplier implements AsyncResponseTransformerTestSupplier<ResponsePublisher<GetObjectResponse>> {

        @Override
        public byte[] body(ResponsePublisher<GetObjectResponse> response) {
            List<Byte> buffer = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> error = new AtomicReference<>();
            response.subscribe(new Subscriber<ByteBuffer>() {
                Subscription s;

                @Override
                public void onSubscribe(Subscription s) {
                    this.s = s;
                    s.request(1);
                }

                @Override
                public void onNext(ByteBuffer byteBuffer) {
                    while (byteBuffer.remaining() > 0) {
                        buffer.add(byteBuffer.get());
                    }
                    s.request(1);
                }

                @Override
                public void onError(Throwable t) {
                    error.set(t);
                    latch.countDown();
                }

                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                fail("Unexpected thread interruption during test", e);
            }
            if (error.get() != null) {
                throw new RuntimeException(error.get());
            }
            return unbox(buffer.toArray(new Byte[0]));
        }

        private byte[] unbox(Byte[] arr) {
            byte[] bb = new byte[arr.length];
            int i = 0;
            for (Byte b : arr) {
                bb[i] = b;
                i++;
            }
            return bb;
        }

        @Override
        public AsyncResponseTransformer<GetObjectResponse, ResponsePublisher<GetObjectResponse>> transformer() {
            return AsyncResponseTransformer.toPublisher();
        }

        @Override
        public String toString() {
            return "AsyncResponseTransformer.toPublisher";
        }
    }

    /**
     * Call this method to retrieve the AsyncResponseTransformer required to perform the test
     *
     * @return
     */
    AsyncResponseTransformer<GetObjectResponse, T> transformer();

    /**
     * Implementation of this method whould retreive the whole body of the request made using the AsyncResponseTransformer as a
     * byte array.
     *
     * @param response the response the {@link AsyncResponseTransformerTestSupplier#transformer}
     * @return
     */
    byte[] body(T response);

    /**
     * Sonce {@link FileAsyncResponseTransformer} works with file, some test might need to initialize an in-memory
     * {@link FileSystem} with jimfs.
     *
     * @return true if the test using this class requires setup with jimfs
     */
    default boolean requiresJimfs() {
        return false;
    }
}
