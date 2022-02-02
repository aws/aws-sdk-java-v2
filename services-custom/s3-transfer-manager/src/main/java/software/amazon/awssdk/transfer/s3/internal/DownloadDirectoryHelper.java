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

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.transfer.s3.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.DirectoryDownload;
import software.amazon.awssdk.transfer.s3.DownloadDirectoryOverrideConfiguration;
import software.amazon.awssdk.transfer.s3.DownloadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.FailedFileDownload;
import software.amazon.awssdk.transfer.s3.FileDownload;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * An internal helper class that first retrieves the objects to download from S3 recursively and then send a download request for
 * each object.
 */
@SdkInternalApi
public class DownloadDirectoryHelper {
    private static final String DEFAULT_DELIMITER = "/";
    private static final String DEFAULT_PREFIX = "";
    private static final Logger log = Logger.loggerFor(S3TransferManager.class);

    private final TransferManagerConfiguration transferConfiguration;
    private final FileSystem fileSystem;
    private final Function<DownloadFileRequest, FileDownload> downloadFileFunction;
    private final ListObjectsRecursivelyHelper listObjectsRecursivelyHelper;

    public DownloadDirectoryHelper(TransferManagerConfiguration transferConfiguration,
                                   Function<ListObjectsV2Request, CompletableFuture<ListObjectsV2Response>> listObjectsFunction,
                                   Function<DownloadFileRequest, FileDownload> downloadFileFunction) {

        this.transferConfiguration = transferConfiguration;
        this.downloadFileFunction = downloadFileFunction;
        this.fileSystem = FileSystems.getDefault();
        this.listObjectsRecursivelyHelper = new ListObjectsRecursivelyHelper(listObjectsFunction);
    }

    @SdkTestInternalApi
    DownloadDirectoryHelper(TransferManagerConfiguration transferConfiguration,
                            FileSystem fileSystem,
                            Function<DownloadFileRequest, FileDownload> downloadFileFunction,
                            ListObjectsRecursivelyHelper listObjectsRecursivelyHelper) {

        this.transferConfiguration = transferConfiguration;
        this.fileSystem = fileSystem;
        this.downloadFileFunction = downloadFileFunction;
        this.listObjectsRecursivelyHelper = listObjectsRecursivelyHelper;
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

    private static void validateDirectory(DownloadDirectoryRequest downloadDirectoryRequest) {
        Path directory = downloadDirectoryRequest.destinationDirectory();

        if (!Files.exists(directory)) {
            try {
                Files.createDirectory(directory);
            } catch (IOException e) {
                throw SdkClientException.create("Failed to create the destination directory " + directory, e);
            }
        }

        Validate.isTrue(Files.isDirectory(directory), "The destination directory provided (%s) is not a "
                                                      + "directory", directory);
    }

    private void doDownloadDirectory(CompletableFuture<CompletedDirectoryDownload> returnFuture,
                                     DownloadDirectoryRequest downloadDirectoryRequest) {
        validateDirectory(downloadDirectoryRequest);
        String bucket = downloadDirectoryRequest.bucket();
        String delimiter = getDelimiter(downloadDirectoryRequest);
        String prefix = downloadDirectoryRequest.prefix().orElse(DEFAULT_PREFIX);

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                                                           .bucket(bucket)
                                                           .prefix(prefix)
                                                           .delimiter(delimiter)
                                                           .build();

        Collection<FailedFileDownload> failedFileDownloads = new ConcurrentLinkedQueue<>();
        List<CompletableFuture<?>> futures = new ArrayList<>();

        listObjectsRecursivelyHelper.s3Objects(request).subscribe(s3Object -> {
            log.debug(() -> "s3Object key: " + s3Object.key());
            futures.add(downloadSingleFile(downloadDirectoryRequest,
                                           failedFileDownloads,
                                           s3Object));
        }).whenComplete((r, t) -> {
            if (t != null) {
                returnFuture.completeExceptionally(SdkClientException.create("Failed to call ListObjectsV2  ", t));
            } else {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                                 .whenComplete((response, throwable) -> returnFuture.complete(
                                     CompletedDirectoryDownload.builder()
                                                               .failedTransfers(failedFileDownloads)
                                                               .build()));
            }
        });
    }

    private static String getDelimiter(DownloadDirectoryRequest downloadDirectoryRequest) {
        return downloadDirectoryRequest.delimiter().orElse(DEFAULT_DELIMITER);
    }

    private CompletableFuture<CompletedFileDownload> downloadSingleFile(DownloadDirectoryRequest downloadDirectoryRequest,
                                                                        Collection<FailedFileDownload> failedFileDownloads,
                                                                        S3Object s3Object) {
        String directory = downloadDirectoryRequest.destinationDirectory().toString();
        String relativePath = getRelativePath(getDelimiter(downloadDirectoryRequest),
                                              s3Object.key());

        Path destinationPath = fileSystem.getPath(directory, relativePath);
        DownloadFileRequest request = downloadFileRequest(downloadDirectoryRequest, s3Object, destinationPath);

        try {
            log.debug(() -> "Sending download request " + request);
            createParentDirectoriesIfNeeded(destinationPath);

            CompletableFuture<CompletedFileDownload> future = downloadFileFunction.apply(request).completionFuture();
            future.whenComplete((r, t) -> {
                if (t != null) {
                    failedFileDownloads.add(FailedFileDownload.builder()
                                                              .exception(t)
                                                              .request(request)
                                                              .build());
                }
            });
            return future;
        } catch (Throwable throwable) {
            failedFileDownloads.add(FailedFileDownload.builder()
                                                      .exception(throwable)
                                                      .request(request)
                                                      .build());
            return CompletableFutureUtils.failedFuture(throwable);
        }
    }

    private static DownloadFileRequest downloadFileRequest(DownloadDirectoryRequest downloadDirectoryRequest,
                                                           S3Object s3Object,
                                                           Path destinationPath) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                            .bucket(downloadDirectoryRequest.bucket())
                                                            .key(s3Object.key())
                                                            .build();
        DownloadFileRequest.Builder requestBuilder = DownloadFileRequest.builder()
                                                                        .destination(destinationPath)
                                                                        .getObjectRequest(
                                                                            getObjectRequest);


        downloadDirectoryRequest.overrideConfiguration()
                                .map(DownloadDirectoryOverrideConfiguration::downloadFileRequestTransformer)
                                .ifPresent(c -> c.accept(requestBuilder));
        return requestBuilder.build();
    }

    private static void createParentDirectoriesIfNeeded(Path destinationPath) {
        Path parentDirectory = destinationPath.getParent();
        if (parentDirectory != null && !Files.exists(parentDirectory)) {
            try {
                Files.createDirectories(parentDirectory);
            } catch (IOException e) {
                throw SdkClientException.create(String.format("Failed to create directory %s and all its nonexistent parent "
                                                              + "directories ", parentDirectory.toAbsolutePath()), e);
            }
        }
    }

    private String getRelativePath(String delimiter, String key) {
        if (fileSystem.getSeparator().equals(delimiter)) {
            return key;
        }

        return key.replace(delimiter, fileSystem.getSeparator());
    }
}
