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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.transfer.s3.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.DirectoryDownload;
import software.amazon.awssdk.transfer.s3.DownloadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.FailedFileDownload;
import software.amazon.awssdk.transfer.s3.FileDownload;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * An internal helper class that sends {@link DownloadFileRequest}s while it retrieves the objects to download from S3 recursively
 */
@SdkInternalApi
public class DownloadDirectoryHelper {
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

    @SdkTestInternalApi
    DownloadDirectoryHelper(TransferManagerConfiguration transferConfiguration,
                            Function<DownloadFileRequest, FileDownload> downloadFileFunction,
                            ListObjectsHelper listObjectsHelper) {

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
        validateDirectoryIfExists(downloadDirectoryRequest.destinationDirectory());
        String bucket = downloadDirectoryRequest.bucket();
        String delimiter = downloadDirectoryRequest.delimiter().orElse(DEFAULT_DELIMITER);
        String prefix = downloadDirectoryRequest.prefix().orElse(DEFAULT_PREFIX);

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                                                           .bucket(bucket)
                                                           .prefix(prefix)
                                                           .delimiter(delimiter)
                                                           .build();

        listObjectsHelper.listS3ObjectsRecursively(request)
                         .subscribe(new S3ObjectSubscriber(downloadDirectoryRequest,
                                                           returnFuture,
                                                           downloadFileFunction,
                                                           DEFAULT_DOWNLOAD_DIRECTORY_MAX_CONCURRENCY));
    }

    @SdkInternalApi
    static class S3ObjectSubscriber implements Subscriber<S3Object> {
        private static final Logger log = Logger.loggerFor(S3TransferManager.class);
        private final List<CompletableFuture<CompletedFileDownload>> currentBuffer;
        private final Collection<FailedFileDownload> failedFileDownloads;
        private final DownloadDirectoryRequest request;
        private Subscription subscription;
        private final CompletableFuture<CompletedDirectoryDownload> returnFuture;
        private final Function<DownloadFileRequest, FileDownload> downloadFileFunction;
        private final int bufferSize;

        S3ObjectSubscriber(DownloadDirectoryRequest request,
                           CompletableFuture<CompletedDirectoryDownload> returnFuture,
                           Function<DownloadFileRequest, FileDownload> downloadFileFunction,
                           int concurrentDownload) {
            this.failedFileDownloads = new ConcurrentLinkedQueue<>();
            this.currentBuffer = new ArrayList<>();
            this.request = request;
            this.returnFuture = returnFuture;
            this.downloadFileFunction = downloadFileFunction;
            this.bufferSize = concurrentDownload;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (subscription == null) {
                throw new NullPointerException("Subscription must not be null");
            }

            if (this.subscription != null) {
                subscription.cancel();
                return;
            }

            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(S3Object s3Object) {
            log.debug(() -> "Processing s3Object key: " + s3Object.key());
            CompletableFuture<CompletedFileDownload> singleFileFuture = downloadSingleFile(request,
                                                                                           failedFileDownloads,
                                                                                           s3Object);
            currentBuffer.add(singleFileFuture);

            if (currentBuffer.size() == bufferSize) {
                CompletableFuture.allOf(currentBuffer.toArray(new CompletableFuture[0]))
                                 .whenComplete((response, throwable) -> {
                                     subscription.request(1);
                                     currentBuffer.clear();
                                 });
            } else {
                subscription.request(1);
            }
        }

        @Override
        public void onError(Throwable t) {
            returnFuture.completeExceptionally(t);
        }

        @Override
        public void onComplete() {
            CompletableFuture.allOf(currentBuffer.toArray(new CompletableFuture[0])).whenComplete((r, t) -> {
                returnFuture.complete(CompletedDirectoryDownload.builder()
                                                                .failedTransfers(failedFileDownloads)
                                                                .build());
            });
        }

        private CompletableFuture<CompletedFileDownload> downloadSingleFile(DownloadDirectoryRequest downloadDirectoryRequest,
                                                                            Collection<FailedFileDownload> failedFileDownloads,
                                                                            S3Object s3Object) {
            FileSystem fileSystem = downloadDirectoryRequest.destinationDirectory().getFileSystem();
            String delimiter = downloadDirectoryRequest.delimiter().orElse(DEFAULT_DELIMITER);

            String key = normalizeKey(downloadDirectoryRequest, s3Object, delimiter);

            String relativePath = getRelativePath(fileSystem,
                                                  delimiter,
                                                  key);

            Path destinationPath = downloadDirectoryRequest.destinationDirectory().resolve(relativePath);
            DownloadFileRequest downloadFileRequest = downloadFileRequest(downloadDirectoryRequest, s3Object, destinationPath);

            try {
                log.debug(() -> "Sending download request " + downloadFileRequest);

                CompletableFuture<CompletedFileDownload> future =
                    downloadFileFunction.apply(downloadFileRequest).completionFuture();
                future.whenComplete((r, t) -> {
                    if (t != null) {
                        failedFileDownloads.add(FailedFileDownload.builder()
                                                                  .exception(t)
                                                                  .request(downloadFileRequest)
                                                                  .build());
                    }
                });
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
         * Normalizing the key by stripping the prefix from the s3 object key if the prefix is not empty. For example: given a
         * request with prefix = "notes/2021", delimiter = "/" and key = "notes/2021/1.txt", the returned string should be
         * "1.txt"
         */
        private static String normalizeKey(DownloadDirectoryRequest downloadDirectoryRequest, S3Object s3Object,
                                           String delimiter) {
            return downloadDirectoryRequest.prefix()
                                           .filter(prefix -> !prefix.isEmpty())
                                           .map(prefix -> s3Object.key().substring(prefix.length() + delimiter.length()))
                                           .orElseGet(s3Object::key);
        }

        private static String getRelativePath(FileSystem fileSystem, String delimiter, String key) {
            if (fileSystem.getSeparator().equals(delimiter)) {
                return key;
            }

            return key.replace(delimiter, fileSystem.getSeparator());
        }

        private static DownloadFileRequest downloadFileRequest(DownloadDirectoryRequest downloadDirectoryRequest,
                                                               S3Object s3Object,
                                                               Path destinationPath) {

            createParentDirectoriesIfNeeded(destinationPath);
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                                .bucket(downloadDirectoryRequest.bucket())
                                                                .key(s3Object.key())
                                                                .build();
            return DownloadFileRequest.builder()
                                      .destination(destinationPath)
                                      .getObjectRequest(getObjectRequest)
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
}
