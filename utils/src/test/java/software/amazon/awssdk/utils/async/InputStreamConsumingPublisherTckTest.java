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

package software.amazon.awssdk.utils.async;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

public class InputStreamConsumingPublisherTckTest extends PublisherVerification<ByteBuffer> {
    private final ExecutorService executor =
        Executors.newCachedThreadPool(new ThreadFactoryBuilder().daemonThreads(true).build());

    public InputStreamConsumingPublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long elements) {
        InputStreamConsumingPublisher publisher = new InputStreamConsumingPublisher();
        executor.submit(() -> {
            publisher.doBlockingWrite(new InputStream() {
                int i = 0;

                @Override
                public int read() throws IOException {
                    throw new IOException();
                }

                @Override
                public int read(byte[] b) throws IOException {
                    if (i >= elements) {
                        return -1;
                    }
                    ++i;
                    assert b.length > 0;
                    return 1;
                }
            });
        });
        return publisher;
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        InputStreamConsumingPublisher publisher = new InputStreamConsumingPublisher();
        executor.submit(publisher::cancel);
        return publisher;
    }
}