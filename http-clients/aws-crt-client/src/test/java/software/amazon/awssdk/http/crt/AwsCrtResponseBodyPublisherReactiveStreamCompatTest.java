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

package software.amazon.awssdk.http.crt;

import static org.mockito.Mockito.mock;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.crt.http.HttpClientConnection;
import software.amazon.awssdk.crt.http.HttpStream;
import software.amazon.awssdk.http.crt.internal.AwsCrtResponseBodyPublisher;
import software.amazon.awssdk.utils.Logger;

public class AwsCrtResponseBodyPublisherReactiveStreamCompatTest extends PublisherVerification<ByteBuffer> {
    private static final Logger log = Logger.loggerFor(AwsCrtResponseBodyPublisherReactiveStreamCompatTest.class);

    public AwsCrtResponseBodyPublisherReactiveStreamCompatTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long elements) {
        HttpClientConnection connection = mock(HttpClientConnection.class);
        HttpStream stream = mock(HttpStream.class);
        AwsCrtResponseBodyPublisher bodyPublisher = new AwsCrtResponseBodyPublisher(connection, stream, new CompletableFuture<>(), Integer.MAX_VALUE);

        for (long i = 0; i < elements; i++) {
            bodyPublisher.queueBuffer(UUID.randomUUID().toString().getBytes());
        }

        bodyPublisher.setQueueComplete();
        return bodyPublisher;
    }

    // Some tests try to create INT_MAX elements, which causes OutOfMemory Exceptions. Lower the max allowed number of
    // queued buffers to 1024.
    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return null;
    }
}
