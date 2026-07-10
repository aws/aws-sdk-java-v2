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

package software.amazon.awssdk.http.auth.aws.internal.signer.io;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

/**
 * TCK verification test for {@link InMemoryPublisher}.
 */
public class InMemoryPublisherTckTest extends PublisherVerification<ByteBuffer> {

    public InMemoryPublisherTckTest() {
        super(new TestEnvironment(1000));
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long elements) {
        List<ByteBuffer> data = new ArrayList<>();
        long totalLength = 0;
        for (long i = 0; i < elements; i++) {
            byte[] content = {(byte) (i % 127)};
            data.add(ByteBuffer.wrap(content));
            totalLength += content.length;
        }
        return new InMemoryPublisher(data, totalLength);
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024L;
    }
}
