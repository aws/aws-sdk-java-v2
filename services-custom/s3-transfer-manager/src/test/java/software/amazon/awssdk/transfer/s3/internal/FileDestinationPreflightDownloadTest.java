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

package software.amazon.awssdk.transfer.s3.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.multipart.MultipartS3AsyncClient;
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.model.Download;
import software.amazon.awssdk.transfer.s3.model.DownloadRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

/**
 * {@code download} splits the response transformer as a capability probe and discards the result, so the rejection a caller sees
 * comes from the split the underlying client performs. It has to arrive through the returned {@link Download} rather than as a
 * synchronous throw, and the progress listener has to be told exactly once.
 */
public class FileDestinationPreflightDownloadTest {

    private static final String PRE_EXISTING = "pre-existing contents";

    @TempDir
    Path tempDir;

    private Path destination;
    private S3AsyncClient delegate;
    private GenericS3TransferManager tm;

    @BeforeEach
    void init() throws IOException {
        destination = tempDir.resolve("destination.bin");
        Files.write(destination, PRE_EXISTING.getBytes(StandardCharsets.UTF_8));
        // A real multipart client over a mock delegate, so download() takes the branch that probes parallelSplitSupported().
        delegate = mock(S3AsyncClient.class);
        S3AsyncClient multipartClient =
            MultipartS3AsyncClient.create(delegate, MultipartConfiguration.builder().build(), false);
        tm = new GenericS3TransferManager(multipartClient,
                                          mock(UploadDirectoryHelper.class),
                                          mock(TransferManagerConfiguration.class),
                                          mock(DownloadDirectoryHelper.class));
    }

    @Test
    void download_existingDestination_failsCompletionFutureAndNotifiesListener() {
        AtomicReference<Throwable> listenerFailure = new AtomicReference<>();
        AtomicInteger failedCallbacks = new AtomicInteger();
        TransferListener listener = new TransferListener() {
            @Override
            public void transferFailed(Context.TransferFailed context) {
                failedCallbacks.incrementAndGet();
                listenerFailure.set(context.exception());
            }
        };

        Download<GetObjectResponse> download =
            tm.download(DownloadRequest.builder()
                                       .getObjectRequest(GetObjectRequest.builder().bucket("bucket").key("key").build())
                                       .responseTransformer(AsyncResponseTransformer.toFile(destination))
                                       .addTransferListener(listener)
                                       .build());

        assertThat(catchThrowable(() -> download.completionFuture().join()))
            .hasRootCauseInstanceOf(FileAlreadyExistsException.class);
        // Exactly once: the failed future and the error publisher both carry the rejection.
        assertThat(failedCallbacks).hasValue(1);
        assertThat(listenerFailure.get()).isNotNull();
        assertThat(contentsOf(destination)).isEqualTo(PRE_EXISTING);
        verify(delegate, never()).getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class));
    }

    private static String contentsOf(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "<unreadable: " + e + ">";
        }
    }
}
