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

package software.amazon.awssdk.s3.mockserver.testarchive.helpers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.s3.mockserver.invariants.ObservationSink;
import software.amazon.awssdk.s3.mockserver.invariants.SerialSignalSubscriber;
import software.amazon.awssdk.utils.async.SequentialSubscriber;

public final class Downloads {

    private Downloads() {
    }

    public static byte[] drain(SdkPublisher<ByteBuffer> publisher) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Consumer<ByteBuffer> collect = buffer -> {
            byte[] chunk = new byte[buffer.remaining()];
            buffer.get(chunk);
            out.write(chunk, 0, chunk.length);
        };
        CompletableFuture<Void> done = new CompletableFuture<>();
        publisher.subscribe(SerialSignalSubscriber.wrap(new SequentialSubscriber<>(collect, done),
                "the subscriber drained from " + publisher.getClass().getSimpleName()));
        done.join();
        return out.toByteArray();
    }

    public static Path freshFile(String prefix) {
        try {
            Path file = Files.createTempFile(prefix, ".dat");
            Files.delete(file);
            ObservationSink.current().destination(file);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("could not reserve a destination path", e);
        }
    }

    public static Path freshDirectory(String prefix) {
        try {
            Path directory = Files.createTempDirectory(prefix);
            ObservationSink.current().destination(directory);
            return directory;
        } catch (IOException e) {
            throw new UncheckedIOException("could not create a destination directory", e);
        }
    }

    public static Path destination(Path path) {
        ObservationSink.current().destination(path);
        return path;
    }
}
