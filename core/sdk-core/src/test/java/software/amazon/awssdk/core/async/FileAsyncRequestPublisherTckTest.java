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

package software.amazon.awssdk.core.async;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.core.internal.async.FileAsyncRequestBody;

/**
 * TCK verification test for {@link FileAsyncRequestBody}.
 */
public class FileAsyncRequestPublisherTckTest extends org.reactivestreams.tck.PublisherVerification<ByteBuffer> {

    // same as `FileAsyncRequestProvider.DEFAULT_CHUNK_SIZE`:
    private static final int CHUNK_SIZE = 16 * 1024;
    private static final int MAX_ELEMENTS = 1000;

    private final FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
    private final Path rootDir = fs.getRootDirectories().iterator().next();
    private final byte[] chunkData = new byte[CHUNK_SIZE];

    public FileAsyncRequestPublisherTckTest() throws IOException {
        super(new TestEnvironment());
    }

    // prevent some tests from trying to create publishers with more elements
    // than this since it would be impractical. For example, one test attempts
    // to create a publisher with Long.MAX_VALUE elements
    @Override
    public long maxElementsFromPublisher() {
        return MAX_ELEMENTS;
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long elements) {
        return FileAsyncRequestBody.builder()
                .chunkSizeInBytes(CHUNK_SIZE)
                .path(fileOfNChunks(elements))
                .build();
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        // tests properly failing on non existing files:
        return FileAsyncRequestBody.builder()
                .chunkSizeInBytes(CHUNK_SIZE)
                .path(rootDir.resolve("does-not-exist"))
                .build();
    }

    private Path fileOfNChunks(long nChunks) {
        String name = String.format("%d-chunks-file.dat", nChunks);
        Path p = rootDir.resolve(name);
        if (!Files.exists(p)) {
            try (OutputStream os = Files.newOutputStream(p)) {
                for (int i = 0; i < nChunks; ++i) {
                    os.write(chunkData);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return p;
    }
}
