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

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

/**
 * TCK verification class for {@link AwsChunkedResponseDecodingPublisher}.
 */
public class AwsChunkedResponseDecodingPublisherTckTest extends PublisherVerification<ByteBuffer> {

    public AwsChunkedResponseDecodingPublisherTckTest() {
        super(new TestEnvironment());
    }

    /**
     * Each single-byte chunk spans two upstream buffers: a framing-only size line, then the data and CRLF, so the TCK also
     * covers the replacement request for framing-only buffers. The final terminal-chunk buffer emits nothing.
     */
    @Override
    public Publisher<ByteBuffer> createPublisher(long elements) {
        List<ByteBuffer> upstream = new ArrayList<>();
        for (long i = 0; i < elements; i++) {
            upstream.add(ascii("1\r\n"));
            upstream.add(ascii("a\r\n"));
        }
        upstream.add(ascii("0\r\ntrailer-key:abc==\r\n\r\n"));
        return new AwsChunkedResponseDecodingPublisher(Flowable.fromIterable(upstream), elements, null);
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return new AwsChunkedResponseDecodingPublisher(Flowable.error(new RuntimeException("upstream failure")), -1, null);
    }

    private static ByteBuffer ascii(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.US_ASCII));
    }
}
