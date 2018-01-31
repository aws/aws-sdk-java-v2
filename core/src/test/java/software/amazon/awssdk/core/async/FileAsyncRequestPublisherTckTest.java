/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.TestEnvironment;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class FileAsyncRequestPublisherTckTest extends org.reactivestreams.tck.PublisherVerification<ByteBuffer> {

    final File example = File.createTempFile("example", ".tmp");
    final File fileDoesNotExist = new File(example.getPath() + "-does-not-exist");

    public FileAsyncRequestPublisherTckTest() throws IOException {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long l) {
        return AsyncRequestProvider.fromFile(example.toPath());
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return AsyncRequestProvider.fromFile(fileDoesNotExist.toPath());
    }
}
