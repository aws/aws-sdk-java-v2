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
import static software.amazon.awssdk.transfer.s3.internal.TransferConfigurationOption.DEFAULT_DOWNLOAD_DIRECTORY_MAX_CONCURRENCY;
import static software.amazon.awssdk.transfer.s3.internal.TransferConfigurationOption.DEFAULT_PREFIX;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Queue;
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
                                           DEFAULT_DOWNLOAD_DIRECTORY_MAX_CONCURRENCY);
        listObjectsHelper.listS3ObjectsRecursively(request)
                         .filter(downloadDirectoryRequest.filter())
                         .subscribe(asyncBufferingSubscriber);

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

        return s3Object -> {
            CompletableFuture<CompletedFileDownload> future = doDownloadSingleFile(downloadDirectoryRequest,
                                                                                   failedFileDownloads,
                                                                                   listRequest,
                                                                                   s3Object);
            CompletableFutureUtils.forwardExceptionTo(returnFuture, future);
            return future;
        };
    }

    private Path determineDestinationPath(DownloadDirectoryRequest downloadDirectoryRequest,
                                          ListObjectsV2Request listRequest,
                                          S3Object s3Object) {
        FileSystem fileSystem = downloadDirectoryRequest.destination().getFileSystem();
        String delimiter = listRequest.delimiter() == null ? DEFAULT_DELIMITER : listRequest.delimiter();
        String key = normalizeKey(listRequest, s3Object.key(), delimiter);
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

    private CompletableFuture<CompletedFileDownload> doDownloadSingleFile(DownloadDirectoryRequest downloadDirectoryRequest,
                                                                          Collection<FailedFileDownload> failedFileDownloads,
                                                                          ListObjectsV2Request listRequest,
                                                                          S3Object s3Object) {

        Path destinationPath = determineDestinationPath(downloadDirectoryRequest, listRequest, s3Object);

        DownloadFileRequest downloadFileRequest = downloadFileRequest(downloadDirectoryRequest, s3Object, destinationPath);

        try {
            log.debug(() -> "Sending download request " + downloadFileRequest);
            createParentDirectoriesIfNeeded(destinationPath);

            CompletableFuture<CompletedFileDownload> executionFuture =
                downloadFileFunction.apply(downloadFileRequest).completionFuture();
            CompletableFuture<CompletedFileDownload> future = executionFuture.whenComplete((r, t) -> {
                if (t != null) {
                    failedFileDownloads.add(FailedFileDownload.builder()
                                                              .exception(t instanceof CompletionException ? t.getCause() : t)
                                                              .request(downloadFileRequest)
                                                              .build());
                }
            });
            CompletableFutureUtils.forwardExceptionTo(future, executionFuture);
            return future;

        } catch (Throwable throwable) {
            failedFileDownloads.add(FailedFileDownload.builder()
                                                      .exception(throwable)
                                                      .request(downloadFileRequest)
                                                      .build());
            return CompletableFutureUtils.failedFuture(throwable);
        }
    }

    /**
     * If the prefix is not empty AND the key contains the delimiter, normalize the key by stripping the prefix from the key.
     *
     * If a delimiter is null (not provided by user), use "/" by default.
     *
     * For example: given a request with prefix = "notes/2021"  or "notes/2021/", delimiter = "/" and key = "notes/2021/1.txt",
     * the normalized key should be "1.txt".
     */
    private static String normalizeKey(ListObjectsV2Request listObjectsRequest,
                                       String key,
                                       String delimiter) {
        if (StringUtils.isEmpty(listObjectsRequest.prefix())) {
            return key;
        }

        String prefix = listObjectsRequest.prefix();

        if (!key.contains(delimiter)) {
            return key;
        }

        String normalizedKey;

        if (prefix.endsWith(delimiter)) {
            normalizedKey = key.substring(prefix.length());
        } else {
            normalizedKey = key.substring(prefix.length() + delimiter.length());
        }
        return normalizedKey;

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
