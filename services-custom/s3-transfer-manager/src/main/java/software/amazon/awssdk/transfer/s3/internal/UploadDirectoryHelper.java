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

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.CompletedUpload;
import software.amazon.awssdk.transfer.s3.CompletedUploadDirectory;
import software.amazon.awssdk.transfer.s3.FailedFileUpload;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.Upload;
import software.amazon.awssdk.transfer.s3.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.UploadDirectoryTransfer;
import software.amazon.awssdk.transfer.s3.UploadRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * An internal helper class that traverses the file tree and send the upload request
 * for each file.
 */
@SdkInternalApi
public class UploadDirectoryHelper {
    private static final Logger log = Logger.loggerFor(S3TransferManager.class);

    private final TransferManagerConfiguration transferConfiguration;
    private final Function<UploadRequest, Upload> uploadFunction;
    private final FileSystem fileSystem;

    public UploadDirectoryHelper(TransferManagerConfiguration transferConfiguration,
                                 Function<UploadRequest, Upload> uploadFunction) {

        this.transferConfiguration = transferConfiguration;
        this.uploadFunction = uploadFunction;
        this.fileSystem = FileSystems.getDefault();
    }

    @SdkTestInternalApi
    UploadDirectoryHelper(TransferManagerConfiguration transferConfiguration,
                          Function<UploadRequest, Upload> uploadFunction,
                          FileSystem fileSystem) {

        this.transferConfiguration = transferConfiguration;
        this.uploadFunction = uploadFunction;
        this.fileSystem = fileSystem;
    }

    public UploadDirectoryTransfer uploadDirectory(UploadDirectoryRequest uploadDirectoryRequest) {

        CompletableFuture<CompletedUploadDirectory> returnFuture = new CompletableFuture<>();

        // offload the execution to the transfer manager executor
        CompletableFuture.runAsync(() -> doUploadDirectory(returnFuture, uploadDirectoryRequest),
                                   transferConfiguration.option(TransferConfigurationOption.EXECUTOR))
                         .whenComplete((r, t) -> {
                             if (t != null) {
                                 returnFuture.completeExceptionally(t);
                             }
                         });

        return UploadDirectoryTransfer.builder().completionFuture(returnFuture).build();
    }

    private void doUploadDirectory(CompletableFuture<CompletedUploadDirectory> returnFuture,
                                   UploadDirectoryRequest uploadDirectoryRequest) {

        Path directory = uploadDirectoryRequest.sourceDirectory();

        validateDirectory(uploadDirectoryRequest);

        Collection<FailedFileUpload> failedUploads = new ConcurrentLinkedQueue<>();
        List<CompletableFuture<CompletedUpload>> futures;

        try (Stream<Path> entries = listFiles(directory, uploadDirectoryRequest)) {
            futures = entries.map(path -> {
                CompletableFuture<CompletedUpload> future = uploadSingleFile(uploadDirectoryRequest,
                                                                             failedUploads, path);

                // Forward cancellation of the return future to all individual futures.
                CompletableFutureUtils.forwardExceptionTo(returnFuture, future);
                return future;
            }).collect(Collectors.toList());
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                         .whenComplete((r, t) -> returnFuture.complete(CompletedUploadDirectory.builder()
                                                                                               .failedUploads(failedUploads)
                                                                                               .build()));
    }

    private void validateDirectory(UploadDirectoryRequest uploadDirectoryRequest) {
        Path directory = uploadDirectoryRequest.sourceDirectory();
        Validate.isTrue(Files.exists(directory), "The source directory provided (%s) does not exist", directory);
        boolean followSymbolicLinks = transferConfiguration.resolveUploadDirectoryFollowSymbolicLinks(uploadDirectoryRequest);
        if (followSymbolicLinks) {
            Validate.isTrue(Files.isDirectory(directory), "The source directory provided (%s) is not a "
                                                          + "directory", directory);
        } else {
            Validate.isTrue(Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS), "The source directory provided (%s)"
                                                                                     + " is not a "
                                                                                     + "directory", directory);
        }
    }

    private CompletableFuture<CompletedUpload> uploadSingleFile(UploadDirectoryRequest uploadDirectoryRequest,
                                                                Collection<FailedFileUpload> failedUploads,
                                                                Path path) {
        int nameCount = uploadDirectoryRequest.sourceDirectory().getNameCount();
        UploadRequest uploadRequest = constructUploadRequest(uploadDirectoryRequest, nameCount, path);
        log.debug(() -> String.format("Sending upload request (%s) for path (%s)", uploadRequest, path));
        CompletableFuture<CompletedUpload> future = uploadFunction.apply(uploadRequest).completionFuture();
        future.whenComplete((r, t) -> {
            if (t != null) {
                failedUploads.add(FailedFileUpload.builder()
                                                  .exception(t)
                                                  .request(uploadRequest)
                                                  .build());
            }
        });
        return future;
    }

    private Stream<Path> listFiles(Path directory, UploadDirectoryRequest request) {

        try {
            boolean recursive = transferConfiguration.resolveUploadDirectoryRecursive(request);
            boolean followSymbolicLinks = transferConfiguration.resolveUploadDirectoryFollowSymbolicLinks(request);

            if (!recursive) {
                return Files.list(directory)
                            .filter(p -> isRegularFile(p, followSymbolicLinks));
            }

            int maxDepth = transferConfiguration.resolveUploadDirectoryMaxDepth(request);

            if (followSymbolicLinks) {
                return Files.walk(directory, maxDepth, FileVisitOption.FOLLOW_LINKS)
                            .filter(path -> isRegularFile(path, true));
            }

            return Files.walk(directory, maxDepth)
                        .filter(path -> isRegularFile(path, false));

        } catch (IOException e) {
            throw SdkClientException.create("Failed to list files within the provided directory: " + directory, e);
        }
    }

    private boolean isRegularFile(Path path, boolean followSymlinks) {
        if (followSymlinks) {
            return Files.isRegularFile(path);
        }

        return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS);
    }

    /**
     * If the prefix already ends with the same string as delimiter, there is no need to add delimiter.
     */
    private static String normalizePrefix(String prefix, String delimiter) {
        if (StringUtils.isEmpty(prefix)) {
            return "";
        }
        return prefix.endsWith(delimiter) ? prefix : prefix + delimiter;
    }

    private String getRelativePathName(int directoryNameCount, Path path, String delimiter) {
        String relativePathName = path.subpath(directoryNameCount,
                                               path.getNameCount()).toString();

        String separator = fileSystem.getSeparator();

        // Optimization for the case where separator equals to the delimiter: there is no need to call String#replace which
        // invokes Pattern#compile in Java 8
        if (delimiter.equals(separator)) {
            return relativePathName;
        }

        return relativePathName.replace(separator, delimiter);
    }

    private UploadRequest constructUploadRequest(UploadDirectoryRequest uploadDirectoryRequest, int directoryNameCount,
                                                        Path path) {
        String delimiter =
            uploadDirectoryRequest.delimiter()
                                  .filter(s -> !s.isEmpty())
                                  .orElse(DEFAULT_DELIMITER);

        String prefix = uploadDirectoryRequest.prefix()
                                              .map(s -> normalizePrefix(s, delimiter))
                                              .orElse("");

        String relativePathName = getRelativePathName(directoryNameCount, path, delimiter);
        String key = prefix + relativePathName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                                            .bucket(uploadDirectoryRequest.bucket())
                                                            .key(key)
                                                            .build();
        return UploadRequest.builder()
                            .source(path)
                            .putObjectRequest(putObjectRequest)
                            .build();
    }
}
