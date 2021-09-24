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
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.CompletedUpload;
import software.amazon.awssdk.transfer.s3.CompletedUploadDirectory;
import software.amazon.awssdk.transfer.s3.FailedUpload;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.Upload;
import software.amazon.awssdk.transfer.s3.UploadDirectory;
import software.amazon.awssdk.transfer.s3.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.UploadRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;

@SdkInternalApi
public class UploadDirectoryManager {
    private static final Logger log = Logger.loggerFor(S3TransferManager.class);

    private final TransferConfiguration transferConfiguration;
    private final Function<UploadRequest, Upload> uploadFunction;

    public UploadDirectoryManager(TransferConfiguration transferConfiguration,
                                  Function<UploadRequest, Upload> uploadFunction) {

        this.transferConfiguration = transferConfiguration;
        this.uploadFunction = uploadFunction;
    }

    public UploadDirectory uploadDirectory(UploadDirectoryRequest uploadDirectoryRequest) {
        CompletableFuture<CompletedUploadDirectory> returnFuture = new CompletableFuture<>();
        Path directory = uploadDirectoryRequest.sourceDirectory();

        List<CompletedUpload> uploads = new ArrayList<>();
        List<FailedUpload> failedUploads = new ArrayList<>();
        List<CompletableFuture<CompletedUpload>> uploadFutures;

        try (Stream<Path> entries = listFiles(directory, uploadDirectoryRequest)) {
            uploadFutures =
                entries.map(path -> {
                    CompletableFuture<CompletedUpload> future =
                        uploadSingleFile(uploadDirectoryRequest, uploads, failedUploads, path);
                    // Forward cancellation of the return future to all individual futures.
                    CompletableFutureUtils.forwardExceptionTo(returnFuture, future);
                    return future;
                }).collect(Collectors.toList());
        }

        CompletableFuture.allOf(uploadFutures.toArray(new CompletableFuture[0]))
                         .whenComplete((r, t) -> returnFuture.complete(
                             DefaultCompletedUploadDirectory.builder()
                                                            .failedUploads(failedUploads)
                                                            .successfulUploads(uploads)
                                                            .build()));
        return new DefaultUploadDirectory(returnFuture);
    }

    private CompletableFuture<CompletedUpload> uploadSingleFile(UploadDirectoryRequest uploadDirectoryRequest,
                                                                List<CompletedUpload> uploads,
                                                                List<FailedUpload> failedUploads,
                                                                Path path) {
        int nameCount = uploadDirectoryRequest.sourceDirectory().getNameCount();
        UploadRequest uploadRequest = constructUploadRequest(uploadDirectoryRequest, nameCount, path);
        CompletableFuture<CompletedUpload> future = uploadFunction.apply(uploadRequest)
                                                                  .completionFuture();
        future.whenComplete((r, t) -> {
            if (t != null) {
                failedUploads.add(DefaultFailedUpload.builder()
                                                     .exception(t)
                                                     .path(path)
                                                     .build());
            } else {
                uploads.add(r);
            }
        });
        return future;
    }

    private Stream<Path> listFiles(Path directory, UploadDirectoryRequest request) {

        try {
            boolean recursive = transferConfiguration.resolveUploadDirectoryRecursive(request);

            if (!recursive) {
                return Files.list(directory).filter(Files::isRegularFile);
            }

            boolean followSymbolicLinks = transferConfiguration.resolveUploadDirectoryFollowSymbolicLinks(request);

            int maxDepth = transferConfiguration.resolveUploadDirectoryMaxDepth(request);

            if (followSymbolicLinks) {
                return Files.walk(directory, maxDepth, FileVisitOption.FOLLOW_LINKS)
                            .peek(p -> log.trace(() -> "Processing path: " + p))
                            .filter(Files::isRegularFile);
            }

            return Files.walk(directory, maxDepth)
                        .peek(p -> log.trace(() -> "Processing path: " + p))
                        .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS));

        } catch (IOException e) {
            throw SdkClientException.create("Failed to list files under the provided directory", e);
        }
    }

    private static String processPrefix(String prefix) {
        if (StringUtils.isEmpty(prefix)) {
            return "";
        }
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }

    private static String getRelativePathName(int directoryNameCount, Path path) {
        String relativePathName = path.subpath(directoryNameCount,
                                               path.getNameCount()).toString();

        // Replace "\" (Windows FS) with "/"
        return relativePathName.replace('\\', '/');
    }

    private static UploadRequest constructUploadRequest(UploadDirectoryRequest uploadDirectoryRequest, int directoryNameCount,
                                                        Path path) {
        String prefix = processPrefix(uploadDirectoryRequest.prefix());
        String relativePathName = getRelativePathName(directoryNameCount, path);
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
