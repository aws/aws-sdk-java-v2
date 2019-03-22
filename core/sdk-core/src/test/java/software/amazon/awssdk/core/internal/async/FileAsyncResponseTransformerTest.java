/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.internal.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;

/**
 * Tests for {@link FileAsyncResponseTransformer}.
 */
public class FileAsyncResponseTransformerTest {
    private FileSystem testFs;

    @Before
    public void setup() {
        testFs = Jimfs.newFileSystem(Configuration.forCurrentPlatform());
    }

    @After
    public void teardown() throws IOException {
        testFs.close();
    }

    @Test
    public void defaultCreatesNewWritableFile() {
        byte[] content = "test".getBytes(StandardCharsets.UTF_8);
        Path testFile = testFs.getPath("testFile");
        AsyncResponseTransformer<String, String> transformer = AsyncResponseTransformer.toFile(testFile);
        CompletableFuture<String> transformFuture = transformer.prepare();
        transformer.onResponse("some response");
        transformer.onStream(AsyncRequestBody.fromBytes(content));
        transformFuture.join();

        assertFileContentsEquals(testFile, "test");
    }

    @Test
    public void honorsPosition() throws IOException {
        byte[] content = "test".getBytes(StandardCharsets.UTF_8);
        Path testFile = testFs.getPath("testFile");
        AsyncResponseTransformer<String, String> transformer = AsyncResponseTransformer.toFile(testFile, content.length, true, true);
        CompletableFuture<String> transformFuture = transformer.prepare();
        transformer.onResponse("some response");
        transformer.onStream(AsyncRequestBody.fromBytes(content));
        transformFuture.join();

        assertThat(Files.size(testFile)).isEqualTo(content.length * 2);
    }

    @Test
    public void honorsNewFileFlags_False() throws IOException {
        Path exists = testFs.getPath("exists");
        createFileWithContents(exists, "Hello".getBytes(StandardCharsets.UTF_8));

        honorsNewFileFlagTest(exists, 5, false, "Test", "HelloTest");
    }

    @Test
    public void honorsNewFileFlag_True_FileNotExists() {
        Path notExists = testFs.getPath("notExists");
        honorsNewFileFlagTest(notExists, 0, true, "Test", "Test");
    }

    @Test
    public void honorsNewFileFlag_True_FileExists() throws IOException {
        Path exists = testFs.getPath("exists");
        createFileWithContents(exists, "Hello".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> honorsNewFileFlagTest(exists, 5, true, "Test", null))
                .hasCauseInstanceOf(FileAlreadyExistsException.class);
    }

    @Test
    public void honorsDeleteOnFailure_True_NoExistingFile() {
        Path notExists = testFs.getPath("notExists");
        honorsDeleteOnFailureTest(notExists, true, true);
    }

    @Test
    public void honorsDeleteOnFailure_True_ExistingFile() throws IOException {
        Path exists = testFs.getPath("exists");
        createFileWithContents(exists, "Hello".getBytes(StandardCharsets.UTF_8));
        honorsDeleteOnFailureTest(exists, false, true);
    }

    @Test
    public void honorsDeleteOnFailure_False_NonExistingFile() {
        Path notExists = testFs.getPath("notExists");
        honorsDeleteOnFailureTest(notExists, true, false);
    }

    @Test
    public void honorsDeleteOnFailure_False_ExistingFile() throws IOException {
        Path exists = testFs.getPath("exists");
        createFileWithContents(exists, "Hello".getBytes(StandardCharsets.UTF_8));
        honorsDeleteOnFailureTest(exists, false, false);
    }

    private void honorsNewFileFlagTest(Path file, long position, boolean isNewFile, String streamContents, String expectedContents) {
        AsyncResponseTransformer<String, String> transformer = AsyncResponseTransformer.toFile(file, position, isNewFile, true);
        CompletableFuture<String> transformFuture = transformer.prepare();
        transformer.onResponse("some response");
        transformer.onStream(AsyncRequestBody.fromString(streamContents));
        transformFuture.join();

        if (expectedContents != null) {
            assertFileContentsEquals(file, expectedContents);
        }
    }

    private void honorsDeleteOnFailureTest(Path file, boolean isNewFile, boolean deleteOnFailure) {
        AsyncResponseTransformer<String, String> transformer = AsyncResponseTransformer.toFile(file, 0, isNewFile, deleteOnFailure);
        CompletableFuture<String> transformFuture = transformer.prepare();
        IOException error = new IOException("Something went wrong");
        transformer.onResponse("some response");
        transformer.onStream(new ErrorPublisher<>(error));
        transformer.exceptionOccurred(error);
        assertThatThrownBy(transformFuture::join).hasCause(error);
        if (deleteOnFailure) {
            assertThat(Files.exists(file)).isFalse();
        } else {
            assertThat(Files.exists(file)).isTrue();
        }
    }

    private static void createFileWithContents(Path file, byte[] contents) throws IOException {
        OutputStream os = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        os.write(contents);
        os.close();
    }

    private static void assertFileContentsEquals(Path file, String expected) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file)));
            String s;
            while ((s = reader.readLine()) != null) {
                sb.append(s);
            }
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        assertThat(sb.toString()).isEqualTo(expected);
    }

    private static final class ErrorPublisher<T> implements SdkPublisher<T> {
        private final Throwable error;

        private ErrorPublisher(Throwable error) {
            this.error = error;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            subscriber.onSubscribe(new ErrorSubscription(subscriber, error));
        }
    }

    private static final class ErrorSubscription implements Subscription {
        private final Subscriber<?> subscriber;
        private final Throwable error;

        public ErrorSubscription(Subscriber<?> subscriber, Throwable error) {
            this.subscriber = subscriber;
            this.error = error;
        }

        @Override
        public void request(long l) {
            subscriber.onError(error);
        }

        @Override
        public void cancel() {

        }
    }
}
