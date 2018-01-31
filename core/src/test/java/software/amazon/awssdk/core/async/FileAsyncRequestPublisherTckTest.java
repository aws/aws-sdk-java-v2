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

import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.http.async.SimpleSubscriber;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class FileAsyncRequestPublisherTckTest extends org.reactivestreams.tck.PublisherVerification<ByteBuffer> {

    final File example = File.createTempFile("example", ".tmp");
    final File fileDoesNotExist = new File(example.getPath() + "-does-not-exist");

    public FileAsyncRequestPublisherTckTest() throws IOException {
        super(new TestEnvironment());

        BufferedWriter writer = new BufferedWriter(new FileWriter(example));
        writer.write("Hello world\n");
        writer.write("Hello world\n");
        writer.write("Hello world\n");
        writer.write("Hello world\n");
        writer.write("Hello world\n");
        writer.flush();
        writer.close();
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
