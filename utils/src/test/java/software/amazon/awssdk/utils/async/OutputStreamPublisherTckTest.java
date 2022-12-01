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

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

public class OutputStreamPublisherTckTest extends PublisherVerification<ByteBuffer> {
    private final ExecutorService executor =
        Executors.newCachedThreadPool(new ThreadFactoryBuilder().daemonThreads(true).build());

    public OutputStreamPublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long elements) {
        OutputStreamPublisher publisher = new OutputStreamPublisher();
        executor.submit(() -> {
            for (int i = 0; i < elements; i++) {
                publisher.write(new byte[1]);
            }
            publisher.close();
        });
        return publisher;
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        OutputStreamPublisher publisher = new OutputStreamPublisher();
        executor.submit(publisher::cancel);
        return publisher;
    }
}