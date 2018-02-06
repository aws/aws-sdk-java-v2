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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

public class FileAsyncRequestPublisherTckTest extends org.reactivestreams.tck.PublisherVerification<ByteBuffer> {

    // same as `FileAsyncRequestProvider.DEFAULT_CHUNK_SIZE`:
    final int DEFAULT_CHUNK_SIZE = 16 * 1024;
    final int ELEMENTS = 1000;

    // mock file system:
    final FileSystem fs = Jimfs.newFileSystem(Configuration.unix());

    final Path testFile;
    final Path doestNotExist;

    public FileAsyncRequestPublisherTckTest() throws IOException {
        super(new TestEnvironment());
        testFile = Files.createFile(fs.getPath("/test-file.tmp"));

        doestNotExist = new File("does-not-exist").toPath();

        final BufferedWriter writer = Files.newBufferedWriter(testFile);

        final char[] chars = new char[DEFAULT_CHUNK_SIZE];
        Arrays.fill(chars, 'A');

        for (int i = 0; i < ELEMENTS; i++) {
            writer.write(chars); // write one chunk
        }
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long elements) {
        if (elements < ELEMENTS) return AsyncRequestProvider.fromFile(testFile);
        else return null; // we don't support more elements
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        // tests properly failing on non existing files:
        return AsyncRequestProvider.fromFile(doestNotExist);
    }
}
