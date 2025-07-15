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

package software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding;

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

public class ChunkedEncodedPublisherTckTest extends PublisherVerification<ByteBuffer> {
    private static final int INPUT_STREAM_ELEMENT_SIZE = 64;
    private static final int CHUNK_SIZE = 16 * 1024;

    public ChunkedEncodedPublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long l) {
        return createChunkedPublisher(l);
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 512;
    }

    private Publisher<ByteBuffer> createChunkedPublisher(long chunksToProduce) {
        // max of 8 MiB
        long totalSize = chunksToProduce * CHUNK_SIZE;

        int totalElements = (int) (totalSize / INPUT_STREAM_ELEMENT_SIZE);

        byte[] content = new byte[INPUT_STREAM_ELEMENT_SIZE];

        List<ByteBuffer> elements = new ArrayList<>();
        for (int i = 0; i < totalElements; i++) {
            elements.add(ByteBuffer.wrap(content));
        }

        Publisher<ByteBuffer> inputPublisher = Flowable.fromIterable(elements);

        return ChunkedEncodedPublisher.builder()
                                      .chunkSize(CHUNK_SIZE)
                                      .publisher(inputPublisher)
                                      .addEmptyTrailingChunk(false)
                                      .build();
    }
}
