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

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.core.internal.async.CompressionAsyncRequestBody;
import software.amazon.awssdk.core.internal.compression.Compressor;
import software.amazon.awssdk.core.internal.compression.GzipCompressor;

public class CompressionAsyncRequestBodyTckTest extends PublisherVerification<ByteBuffer> {

    private static final int MAX_ELEMENTS = 1000;
    private static final int CHUNK_SIZE = 128 * 1024;
    private static final Compressor compressor = new GzipCompressor();

    public CompressionAsyncRequestBodyTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public long maxElementsFromPublisher() {
        return MAX_ELEMENTS;
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long n) {
        return CompressionAsyncRequestBody.builder()
                                          .asyncRequestBody(customAsyncRequestBodyWithoutContentLength(createCompressibleStringOfNChunks(n).getBytes()))
                                          .compressor(compressor)
                                          .build();
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return null;
    }

    private static String createCompressibleStringOfNChunks(long nChunks) {
        int size = Math.toIntExact(nChunks * CHUNK_SIZE);
        ByteBuffer data = ByteBuffer.allocate(size);

        byte[] a = new byte[size / 4];
        byte[] b = new byte[size / 4];
        Arrays.fill(a, (byte) 'a');
        Arrays.fill(b, (byte) 'b');

        data.put(a);
        data.put(b);
        data.put(a);
        data.put(b);

        return new String(data.array());
    }

    private static AsyncRequestBody customAsyncRequestBodyWithoutContentLength(byte[] content) {
        return new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.empty();
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                Flowable.fromPublisher(AsyncRequestBody.fromBytes(content))
                        .subscribe(s);
            }
        };
    }
}
