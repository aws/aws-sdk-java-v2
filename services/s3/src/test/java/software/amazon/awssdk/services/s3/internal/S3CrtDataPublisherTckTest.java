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

package software.amazon.awssdk.services.s3.internal;

import java.nio.ByteBuffer;
import org.apache.commons.lang3.RandomUtils;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.core.internal.async.FileAsyncRequestBody;
import software.amazon.awssdk.services.s3.internal.s3crt.S3CrtDataPublisher;

/**
 * TCK verification test for {@link FileAsyncRequestBody}.
 */
public class S3CrtDataPublisherTckTest extends org.reactivestreams.tck.PublisherVerification<ByteBuffer> {

    public S3CrtDataPublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long elements) {
        S3CrtDataPublisher s3CrtDataPublisher = new S3CrtDataPublisher();

        for (long i = 0; i < elements; i++) {
            s3CrtDataPublisher.deliverData(ByteBuffer.wrap(RandomUtils.nextBytes(20)));
        }

        s3CrtDataPublisher.notifyStreamingFinished();

        return s3CrtDataPublisher;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        S3CrtDataPublisher s3CrtDataPublisher = new S3CrtDataPublisher();

        s3CrtDataPublisher.deliverData(ByteBuffer.wrap(RandomUtils.nextBytes(20)));

        s3CrtDataPublisher.notifyError(new RuntimeException("error"));

        return s3CrtDataPublisher;
    }
}
