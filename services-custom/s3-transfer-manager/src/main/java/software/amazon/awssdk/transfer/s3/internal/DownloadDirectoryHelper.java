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

import static software.amazon.awssdk.transfer.s3.internal.TransferConfigurationOption.DEFAULT_DELIMITER;
import static software.amazon.awssdk.transfer.s3.internal.TransferConfigurationOption.DEFAULT_PREFIX;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultDirectoryDownload;
import software.amazon.awssdk.transfer.s3.internal.utils.LogSanitizer;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.DirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.DownloadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FailedFileDownload;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * An internal helper class that sends {@link DownloadFileRequest}s while it retrieves the objects to download from S3
 * recursively
 */
@SdkInternalApi
public class DownloadDirectoryHelper {
    private static final Logger log = Logger.loggerFor(S3TransferManager.class);
    private static final String TEMP_FILE_INFIX = ".s3tmp.";
    private static final int MAX_FILE_NAME_BYTES = 255;
    private final TransferManagerConfiguration transferConfiguration;
    private final Function<DownloadFileRequest, FileDownload> downloadFileFunction;
    private final ListObjectsHelper listObjectsHelper;

    public DownloadDirectoryHelper(TransferManagerConfiguration transferConfiguration,
                                   ListObjectsHelper listObjectsHelper,
                                   Function<DownloadFileRequest, FileDownload> downloadFileFunction) {

        this.transferConfiguration = transferConfiguration;
        this.downloadFileFunction = downloadFileFunction;
        this.listObjectsHelper = listObjectsHelper;
    }

    public DirectoryDownload downloadDirectory(DownloadDirectoryRequest downloadDirectoryRequest) {

        CompletableFuture<CompletedDirectoryDownload> returnFuture = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> doDownloadDirectory(returnFuture, downloadDirectoryRequest),
                                   transferConfiguration.option(TransferConfigurationOption.EXECUTOR))
                         .whenComplete((r, t) -> {
                             if (t != null) {
                                 returnFuture.completeExceptionally(t);
                             }
                         });

        return new DefaultDirectoryDownload(returnFuture);
    }

    private static void validateDirectoryIfExists(Path directory) {
        if (Files.exists(directory)) {
            Validate.isTrue(Files.isDirectory(directory), "The destination directory provided (%s) is not a "
                                                          + "directory", directory);
        }
    }

    private void doDownloadDirectory(CompletableFuture<CompletedDirectoryDownload> returnFuture,
                                     DownloadDirectoryRequest downloadDirectoryRequest) {
        validateDirectoryIfExists(downloadDirectoryRequest.destination());
        String bucket = downloadDirectoryRequest.bucket();

        // Delimiter is null by default. See https://github.com/aws/aws-sdk-java/issues/1215
        ListObjectsV2Request request =
            ListObjectsV2Request.builder()
                                .bucket(bucket)
                                .prefix(DEFAULT_PREFIX)
                                .applyMutation(downloadDirectoryRequest.listObjectsRequestTransformer())
                                .build();

        Queue<FailedFileDownload> failedFileDownloads = new ConcurrentLinkedQueue<>();

        CompletableFuture<Void> allOfFutures = new CompletableFuture<>();
        AsyncBufferingSubscriber<S3Object> asyncBufferingSubscriber =
            new AsyncBufferingSubscriber<>(downloadSingleFile(returnFuture, downloadDirectoryRequest, request,
                                                              failedFileDownloads),
                                           allOfFutures,
                                           transferConfiguration.option(
                                               TransferConfigurationOption.DIRECTORY_TRANSFER_MAX_CONCURRENCY
                                           ));
        listObjectsHelper.listS3ObjectsRecursively(request)
                         .filter(downloadDirectoryRequest.filter())
                         .subscribe(asyncBufferingSubscriber);
        CompletableFutureUtils.forwardExceptionTo(returnFuture, allOfFutures);

        allOfFutures.whenComplete((r, t) -> {
            if (t != null) {
                returnFuture.completeExceptionally(SdkClientException.create("Failed to send request", t));
            } else {
                returnFuture.complete(CompletedDirectoryDownload.builder()
                                                                .failedTransfers(failedFileDownloads)
                                                                .build());
            }
        });
    }

    private Function<S3Object, CompletableFuture<?>> downloadSingleFile(
        CompletableFuture<CompletedDirectoryDownload> returnFuture,
        DownloadDirectoryRequest downloadDirectoryRequest,
        ListObjectsV2Request listRequest,
        Queue<FailedFileDownload> failedFileDownloads) {

        return s3Object -> doDownloadSingleFile(returnFuture,
                                            downloadDirectoryRequest,
                                            failedFileDownloads,
                                            listRequest,
                                            s3Object);
    }

    private Path determineDestinationPath(DownloadDirectoryRequest downloadDirectoryRequest,
                                          ListObjectsV2Request listRequest,
                                          S3Object s3Object) {
        FileSystem fileSystem = downloadDirectoryRequest.destination().getFileSystem();
        String delimiter = listRequest.delimiter() == null ? DEFAULT_DELIMITER : listRequest.delimiter();
        String key = DirectoryHelperUtils.normalizeKey(listRequest.prefix(), s3Object.key(), delimiter);
        String relativePath = getRelativePath(fileSystem, delimiter, key);
        Path destinationPath = downloadDirectoryRequest.destination().resolve(relativePath);
        validatePath(downloadDirectoryRequest.destination(), destinationPath, s3Object.key());
        return destinationPath;
    }

    private void validatePath(Path destinationDirectory, Path targetPath, String key) {
        if (!targetPath.toAbsolutePath().normalize().startsWith(destinationDirectory.toAbsolutePath().normalize())) {
            throw SdkClientException.create("Cannot download key " + key +
                                            ", its relative path resolves outside the parent directory.");
        }
    }

    /**
     * Downloads the object to a temporary file next to its destination and moves it into place once it is complete. Keys such
     * as "a/b" and "a//b", or "A" and "a" on a case-insensitive file system, resolve to the same file, and writing to it
     * directly would let their concurrent downloads mix bytes or delete each other's file.
     */
    private CompletableFuture<CompletedFileDownload> doDownloadSingleFile(
        CompletableFuture<CompletedDirectoryDownload> returnFuture,
        DownloadDirectoryRequest downloadDirectoryRequest,
        Collection<FailedFileDownload> failedFileDownloads,
        ListObjectsV2Request listRequest,
        S3Object s3Object) {

        if (returnFuture.isCompletedExceptionally()) {
            return CompletableFutureUtils.failedFuture(
                SdkClientException.create("Download was cancelled before file could be started"));
        }

        Path destinationPath;
        try {
            destinationPath = determineDestinationPath(downloadDirectoryRequest, listRequest, s3Object);
        } catch (SdkClientException | InvalidPathException e) {
            return refuseKey(downloadDirectoryRequest, failedFileDownloads, s3Object, e);
        }

        DownloadFileRequest downloadFileRequest = downloadFileRequest(downloadDirectoryRequest, s3Object, destinationPath);
        Path destination = downloadFileRequest.destination();
        Path tempFile = tempFileNextTo(destination);
        DownloadFileRequest tempFileRequest = downloadFileRequest.copy(b -> b.destination(tempFile));

        try {
            log.debug(() -> LogSanitizer.escapeControlCharacters("Sending download request " + downloadFileRequest));
            createParentDirectoriesIfNeeded(destinationPath);

            CompletableFuture<CompletedFileDownload> executionFuture =
                downloadFileFunction.apply(tempFileRequest).completionFuture();
            CompletableFuture<CompletedFileDownload> future = new CompletableFuture<>();
            // Attached to executionFuture rather than to the returned future, so the temporary file is also removed when the
            // directory download cancels the returned future.
            executionFuture.whenComplete((r, t) -> {
                Throwable failure = t;
                try {
                    if (failure == null) {
                        failure = moveIntoPlace(returnFuture, tempFile, destination);
                    } else {
                        deleteQuietly(tempFile);
                    }
                } catch (Throwable unexpected) {
                    deleteQuietly(tempFile);
                    failure = unexpected;
                }

                if (failure == null) {
                    future.complete(r);
                    return;
                }
                failedFileDownloads.add(FailedFileDownload.builder()
                                                          .exception(failure instanceof CompletionException
                                                                     ? failure.getCause() : failure)
                                                          .request(downloadFileRequest)
                                                          .build());
                future.completeExceptionally(failure);
            });
            CompletableFutureUtils.forwardExceptionTo(future, executionFuture);
            return future;

        } catch (Throwable throwable) {
            deleteQuietly(tempFile);
            failedFileDownloads.add(FailedFileDownload.builder()
                                                      .exception(throwable)
                                                      .request(downloadFileRequest)
                                                      .build());
            return CompletableFutureUtils.failedFuture(throwable);
        }
    }

    /**
     * Returns {@code <name>.s3tmp.<uuid>} next to the destination, shortening the name so the whole file name fits in 255 UTF-8
     * bytes, the file name limit on common file systems.
     */
    private static Path tempFileNextTo(Path destination) {
        String suffix = TEMP_FILE_INFIX + UUID.randomUUID();
        String name = truncateToUtf8Bytes(destination.getFileName().toString(), MAX_FILE_NAME_BYTES - suffix.length());
        return destination.resolveSibling(name + suffix);
    }

    private static String truncateToUtf8Bytes(String value, int maxBytes) {
        int bytes = 0;
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            int length = codePoint < 0x80 ? 1 : codePoint < 0x800 ? 2 : codePoint < 0x10000 ? 3 : 4;
            if (bytes + length > maxBytes) {
                return value.substring(0, i);
            }
            bytes += length;
            i += Character.charCount(codePoint);
        }
        return value;
    }

    /**
     * Moves a completed download onto its destination, replacing any file already there. Returns the failure to report for the
     * key, or null when the key succeeded. A download that wrote no file succeeds and leaves the destination as it is.
     */
    private static Throwable moveIntoPlace(CompletableFuture<CompletedDirectoryDownload> returnFuture,
                                           Path tempFile,
                                           Path destination) {
        if (returnFuture.isCompletedExceptionally()) {
            deleteQuietly(tempFile);
            return SdkClientException.create("Download was cancelled before the file could be moved to " + destination);
        }

        if (!Files.exists(tempFile)) {
            return null;
        }

        try {
            try {
                Files.move(tempFile, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return null;
        } catch (IOException | RuntimeException e) {
            deleteQuietly(tempFile);
            return SdkClientException.create("Failed to move the downloaded file to " + destination, e);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException | RuntimeException e) {
            log.debug(() -> LogSanitizer.escapeControlCharacters("Failed to delete temporary file " + path), e);
        }
    }


    /**
     * Reports a key that can't be mapped to a path inside the destination directory as a single failed download, without
     * sending a request or creating any directory for it.
     */
    private static CompletableFuture<CompletedFileDownload> refuseKey(DownloadDirectoryRequest downloadDirectoryRequest,
                                                                      Collection<FailedFileDownload> failedFileDownloads,
                                                                      S3Object s3Object,
                                                                      RuntimeException cause) {
        SdkClientException failure = cause instanceof SdkClientException
                                     ? (SdkClientException) cause
                                     : SdkClientException.create("Cannot download key " + s3Object.key()
                                                                 + ", it is not a valid path on this file system", cause);
        log.debug(() -> LogSanitizer.escapeControlCharacters("Skipping download of key " + s3Object.key()
                                                             + ", it is reported in failedTransfers(): "
                                                             + failure.getMessage()));

        // Both fields of FailedFileDownload are required, so the request needs some destination. It is never the refused
        // path, and never the destination directory itself. Instead it is a placeholder under the destination directory
        // whose parent directory does not exist and is not created by the SDK. A caller that retries failedTransfers() verbatim
        // through downloadFile(f.request()) then fails without writing anything, and a caller that deletes
        // f.request().destination() to clean up partial files deletes nothing. The downloadFileRequestTransformer is not
        // applied because this request is never sent.
        Path placeholder = downloadDirectoryRequest.destination()
                                                   .resolve(".s3tm-refused-" + UUID.randomUUID())
                                                   .resolve("refused");
        DownloadFileRequest refused =
            DownloadFileRequest.builder()
                               .getObjectRequest(b -> b.bucket(downloadDirectoryRequest.bucket()).key(s3Object.key()))
                               .destination(placeholder)
                               .build();
        failedFileDownloads.add(FailedFileDownload.builder()
                                                  .exception(failure)
                                                  .request(refused)
                                                  .build());
        return CompletableFutureUtils.failedFuture(failure);
    }

    private static String getRelativePath(FileSystem fileSystem, String delimiter, String key) {
        if (delimiter == null) {
            return key;
        }
        if (fileSystem.getSeparator().equals(delimiter)) {
            return key;
        }

        return StringUtils.replace(key, delimiter, fileSystem.getSeparator());
    }

    private static DownloadFileRequest downloadFileRequest(DownloadDirectoryRequest downloadDirectoryRequest,
                                                           S3Object s3Object, Path destinationPath) {

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                            .bucket(downloadDirectoryRequest.bucket())
                                                            .key(s3Object.key())
                                                            .build();
        return DownloadFileRequest.builder()
                                  .destination(destinationPath)
                                  .getObjectRequest(getObjectRequest)
                                  .applyMutation(downloadDirectoryRequest.downloadFileRequestTransformer())
                                  .build();
    }

    private static void createParentDirectoriesIfNeeded(Path destinationPath) {
        Path parentDirectory = destinationPath.getParent();
        try {
            if (parentDirectory != null) {
                Files.createDirectories(parentDirectory);
            }
        } catch (IOException e) {
            throw SdkClientException.create("Failed to create parent directories for " + destinationPath, e);
        }
    }
}
