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

package software.amazon.awssdk.core.internal.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.jimfs.Jimfs;
import io.reactivex.Flowable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.FileTransformerConfiguration;
import software.amazon.awssdk.core.FileTransformerConfiguration.FileWriteOption;
import software.amazon.awssdk.core.async.SdkPublisher;

/**
 * Tests for {@link FileAsyncResponseTransformer}.
 */
class FileAsyncResponseTransformerTest {
    private FileSystem testFs;

    @BeforeEach
    public void setup() {
        testFs = Jimfs.newFileSystem();
    }

    @AfterEach
    public void teardown() throws IOException {
        testFs.close();
    }

    @Test
    public void errorInStream_completesFuture() {
        Path testPath = testFs.getPath("test_file.txt");
        FileAsyncResponseTransformer xformer = new FileAsyncResponseTransformer(testPath);

        CompletableFuture prepareFuture = xformer.prepare();

        xformer.onResponse(new Object());
        xformer.onStream(subscriber -> {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long l) {
                }

                @Override
                public void cancel() {
                }
            });

            subscriber.onError(new RuntimeException("Something went wrong"));
        });

        assertThat(prepareFuture.isCompletedExceptionally()).isTrue();
    }

    @Test
    public void synchronousPublisher_shouldNotHang() throws Exception {
        List<CompletableFuture> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Path testPath = testFs.getPath(i + "test_file.txt");
            FileAsyncResponseTransformer transformer = new FileAsyncResponseTransformer(testPath);

            CompletableFuture prepareFuture = transformer.prepare();

            transformer.onResponse(new Object());

            transformer.onStream(testPublisher(RandomStringUtils.randomAlphanumeric(30000)));
            futures.add(prepareFuture);
        }

        CompletableFuture<Void> future = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        future.get(10, TimeUnit.SECONDS);
        assertThat(future.isCompletedExceptionally()).isFalse();
    }

    @Test
    void noConfiguration_fileAlreadyExists_shouldThrowException() throws Exception {
        Path testPath = testFs.getPath("test_file.txt");
        Files.write(testPath, RandomStringUtils.random(1000).getBytes(StandardCharsets.UTF_8));
        assertThat(testPath).exists();

        String content = RandomStringUtils.randomAlphanumeric(30000);
        FileAsyncResponseTransformer<String> transformer = new FileAsyncResponseTransformer<>(testPath);

        CompletableFuture<String> future = transformer.prepare();
        transformer.onResponse("foobar");
        assertThatThrownBy(() -> transformer.onStream(testPublisher(content))).hasRootCauseInstanceOf(FileAlreadyExistsException.class);
    }

    @Test
    void createOrReplaceExisting_fileDoesNotExist_shouldCreateNewFile() throws Exception {
        Path testPath = testFs.getPath("test_file.txt");
        assertThat(testPath).doesNotExist();
        String newContent = RandomStringUtils.randomAlphanumeric(2000);
        FileAsyncResponseTransformer<String> transformer = new FileAsyncResponseTransformer<>(testPath,
                                                                                              FileTransformerConfiguration.builder()
                                                                                                                          .fileWriteOption(FileWriteOption.CREATE_OR_REPLACE_EXISTING)
                                                                                                                          .build());

        stubSuccessfulStreaming(newContent, transformer);
        assertThat(testPath).hasContent(newContent);
    }

    @Test
    void createOrReplaceExisting_fileAlreadyExists_shouldReplaceExisting() throws Exception {
        Path testPath = testFs.getPath("test_file.txt");

        int existingBytesLength = 20;
        String existingContent = RandomStringUtils.randomAlphanumeric(existingBytesLength);
        byte[] existingContentBytes = existingContent.getBytes(StandardCharsets.UTF_8);
        Files.write(testPath, existingContentBytes);

        int newBytesLength = 10;
        String newContent = RandomStringUtils.randomAlphanumeric(newBytesLength);
        FileAsyncResponseTransformer<String> transformer = new FileAsyncResponseTransformer<>(testPath,
                                                                                              FileTransformerConfiguration.builder()
                                                                                                                          .fileWriteOption(FileWriteOption.CREATE_OR_REPLACE_EXISTING)
                                                                                                                          .build());
        stubSuccessfulStreaming(newContent, transformer);
        assertThat(testPath).hasContent(newContent);
    }

    @Test
    void createOrAppendExisting_fileDoesNotExist_shouldCreateNewFile() throws Exception {
        Path testPath = testFs.getPath("test_file.txt");
        assertThat(testPath).doesNotExist();
        String newContent = RandomStringUtils.randomAlphanumeric(500);
        FileAsyncResponseTransformer<String> transformer = new FileAsyncResponseTransformer<>(testPath,
                                                                                              FileTransformerConfiguration.builder()
                                                                                                                          .fileWriteOption(FileWriteOption.CREATE_OR_APPEND_EXISTING)
                                                                                                                          .build());
        stubSuccessfulStreaming(newContent, transformer);
        assertThat(testPath).hasContent(newContent);
    }

    @Test
    void createOrAppendExisting_fileExists_shouldAppend() throws Exception {
        Path testPath = testFs.getPath("test_file.txt");
        String existingString = RandomStringUtils.randomAlphanumeric(10);
        byte[] existingBytes = existingString.getBytes(StandardCharsets.UTF_8);
        Files.write(testPath, existingBytes);
        String content = RandomStringUtils.randomAlphanumeric(20);
        FileAsyncResponseTransformer<String> transformer = new FileAsyncResponseTransformer<>(testPath,
                                                                                              FileTransformerConfiguration.builder()
                                                                                                                          .fileWriteOption(FileWriteOption.CREATE_OR_APPEND_EXISTING)
                                                                                                                          .build());
        stubSuccessfulStreaming(content, transformer);
        assertThat(testPath).hasContent(existingString + content);
    }

    @ParameterizedTest
    @MethodSource("configurations")
    void exceptionOccurred_deleteFileBehavior(FileTransformerConfiguration configuration) throws Exception {
        Path testPath = testFs.getPath("test_file.txt");
        FileAsyncResponseTransformer<String> transformer = new FileAsyncResponseTransformer<>(testPath,
                                                                                              configuration);
        stubException(RandomStringUtils.random(200), transformer);
        if (configuration.deleteOnFailure().filter(i -> !i).isPresent()) {
            assertThat(testPath).exists();
        } else {
            assertThat(testPath).doesNotExist();
        }
    }

    private static List<FileTransformerConfiguration> configurations() {
        return Arrays.asList(
            FileTransformerConfiguration.builder().build(),
            FileTransformerConfiguration.builder()
                                        .fileWriteOption(FileWriteOption.CREATE_NEW)
                                        .deleteOnFailure(true).build(),
            FileTransformerConfiguration.builder()
                                        .fileWriteOption(FileWriteOption.CREATE_NEW)
                                        .deleteOnFailure(false).build(),
            FileTransformerConfiguration.builder()
                                        .fileWriteOption(FileWriteOption.CREATE_OR_APPEND_EXISTING)
                                        .deleteOnFailure(true).build(),
            FileTransformerConfiguration.builder()
                                        .fileWriteOption(FileWriteOption.CREATE_OR_APPEND_EXISTING)
                                        .deleteOnFailure(false).build(),
            FileTransformerConfiguration.builder()
                                        .fileWriteOption(FileWriteOption.CREATE_OR_REPLACE_EXISTING)
                                        .deleteOnFailure(true).build(),
            FileTransformerConfiguration.builder()
                                        .fileWriteOption(FileWriteOption.CREATE_OR_REPLACE_EXISTING)
                                        .deleteOnFailure(false).build());
    }

    private static void stubSuccessfulStreaming(String newContent, FileAsyncResponseTransformer<String> transformer) throws Exception {
        CompletableFuture<String> future = transformer.prepare();
        transformer.onResponse("foobar");

        transformer.onStream(testPublisher(newContent));

        future.get(10, TimeUnit.SECONDS);
        assertThat(future.isCompletedExceptionally()).isFalse();
    }

    private static void stubException(String newContent, FileAsyncResponseTransformer<String> transformer) throws Exception {
        CompletableFuture<String> future = transformer.prepare();
        transformer.onResponse("foobar");

        RuntimeException runtimeException = new RuntimeException("oops");
        ByteBuffer content = ByteBuffer.wrap(newContent.getBytes(StandardCharsets.UTF_8));
        transformer.onStream(SdkPublisher.adapt(Flowable.just(content, content)));
        transformer.exceptionOccurred(runtimeException);

        assertThatThrownBy(() -> future.get(10, TimeUnit.SECONDS))
            .hasRootCause(runtimeException);
        assertThat(future.isCompletedExceptionally()).isTrue();
    }

    private static SdkPublisher<ByteBuffer> testPublisher(String content) {
        return SdkPublisher.adapt(Flowable.just(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8))));
    }
}