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
import java.nio.file.FileSystem;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultDirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.DirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.FailedFileUpload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
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
    private final Function<UploadFileRequest, FileUpload> uploadFunction;

    public UploadDirectoryHelper(TransferManagerConfiguration transferConfiguration,
                                 Function<UploadFileRequest, FileUpload> uploadFunction) {

        this.transferConfiguration = transferConfiguration;
        this.uploadFunction = uploadFunction;
    }

    public DirectoryUpload uploadDirectory(UploadDirectoryRequest uploadDirectoryRequest) {

        CompletableFuture<CompletedDirectoryUpload> returnFuture = new CompletableFuture<>();

        // offload the execution to the transfer manager executor
        CompletableFuture.runAsync(() -> doUploadDirectory(returnFuture, uploadDirectoryRequest),
                                   transferConfiguration.option(TransferConfigurationOption.EXECUTOR))
                         .whenComplete((r, t) -> {
                             if (t != null) {
                                 returnFuture.completeExceptionally(t);
                             }
                         });

        return new DefaultDirectoryUpload(returnFuture);
    }

    private void doUploadDirectory(CompletableFuture<CompletedDirectoryUpload> returnFuture,
                                   UploadDirectoryRequest uploadDirectoryRequest) {

        Path directory = uploadDirectoryRequest.source();

        validateDirectory(uploadDirectoryRequest);

        Collection<FailedFileUpload> failedFileUploads = new ConcurrentLinkedQueue<>();

        // Two files can only derive the same key if the delimiter can occur inside a file or directory name. Files.walk
        // yields no empty or "." / ".." segments, so otherwise the relative paths, and therefore the keys, are distinct.
        // Keyed by objectIdentity (bucket and key), because a transformer can route files to different buckets.
        ConcurrentMap<String, Path> keysToPaths =
            delimiterCanOccurInFileName(resolveDelimiter(uploadDirectoryRequest), directory.getFileSystem())
            ? new ConcurrentHashMap<>()
            : null;

        Stream<Path> stream = listFiles(directory, uploadDirectoryRequest);

        SdkPublisher<Path> iterablePublisher = SdkPublisher.fromIterable(() -> stream.iterator())
                                                           .doAfterOnCancel(() -> stream.close())
                                                           .doAfterOnError(t -> stream.close())
                                                           .doAfterOnComplete(() -> stream.close());

        CompletableFuture<Void> allOfFutures = new CompletableFuture<>();

        AsyncBufferingSubscriber<Path> bufferingSubscriber =
            new AsyncBufferingSubscriber<>(path -> uploadSingleFile(uploadDirectoryRequest, failedFileUploads, keysToPaths,
                                                                    path),
                                           allOfFutures, 
                                           transferConfiguration.option(
                                               TransferConfigurationOption.DIRECTORY_TRANSFER_MAX_CONCURRENCY
                                           ));

        iterablePublisher.subscribe(bufferingSubscriber);
        CompletableFutureUtils.forwardExceptionTo(returnFuture, allOfFutures);

        allOfFutures.whenComplete((r, t) -> {
            if (t != null) {
                returnFuture.completeExceptionally(SdkClientException.create("Failed to send request", t));
                return;
            }
            returnFuture.complete(CompletedDirectoryUpload.builder()
                                                          .failedTransfers(failedFileUploads)
                                                          .build());
        });
    }

    private void validateDirectory(UploadDirectoryRequest uploadDirectoryRequest) {
        Path directory = uploadDirectoryRequest.source();
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

    private CompletableFuture<CompletedFileUpload> uploadSingleFile(UploadDirectoryRequest uploadDirectoryRequest,
                                                                    Collection<FailedFileUpload> failedFileUploads,
                                                                    ConcurrentMap<String, Path> keysToPaths,
                                                                    Path path) {
        int nameCount = uploadDirectoryRequest.source().getNameCount();
        UploadFileRequest uploadFileRequest = constructUploadRequest(uploadDirectoryRequest, nameCount, path);

        // Checked after the transformer, so a transformer that gives the files distinct buckets or keys is honored.
        Path earlier = keysToPaths == null ? null : keysToPaths.putIfAbsent(objectIdentity(uploadFileRequest), path);
        if (earlier != null) {
            return skipDuplicateKey(uploadDirectoryRequest, failedFileUploads, uploadFileRequest, earlier, path);
        }

        log.debug(() -> String.format("Sending upload request (%s) for path (%s)", uploadFileRequest, path));
        CompletableFuture<CompletedFileUpload> executionFuture = uploadFunction.apply(uploadFileRequest).completionFuture();
        CompletableFuture<CompletedFileUpload> future = executionFuture.whenComplete((r, t) -> {
            if (t != null) {
                failedFileUploads.add(FailedFileUpload.builder()
                                                      .exception(t instanceof CompletionException ? t.getCause() : t)
                                                      .request(uploadFileRequest)
                                                      .build());
            }
        });
        CompletableFutureUtils.forwardExceptionTo(future, executionFuture);
        return future;
    }

    /**
     * Reports {@code path} as a failed upload because an earlier file in this request is already being uploaded to the same
     * bucket and key.
     *
     * <p>
     * The failure keeps the real key in its request. This is deliberately the opposite of a refused key in downloadDirectory,
     * which reports a placeholder destination: there, the real path is unsafe to touch, while here the key is a valid S3 key
     * and is the only way for the caller to see which object the two files collided on. The cost is that the key belongs to
     * the earlier file's object. A verbatim retry of this request overwrites that object, and a cleanup that deletes the keys
     * of failed uploads deletes it, so callers must not blindly retry or delete failed keys. The exception message and the
     * {@code s3Delimiter} Javadoc say so.
     */
    private static CompletableFuture<CompletedFileUpload> skipDuplicateKey(UploadDirectoryRequest uploadDirectoryRequest,
                                                                           Collection<FailedFileUpload> failedFileUploads,
                                                                           UploadFileRequest uploadFileRequest,
                                                                           Path earlier,
                                                                           Path path) {
        String key = uploadFileRequest.putObjectRequest().key();
        SdkClientException e = SdkClientException.create(String.format(
            "Skipped uploading %s: %s is already being uploaded to key %s in this request. The delimiter \"%s\" appears in "
            + "a file or directory name, so two files map to the same key. Retrying this request would overwrite that object.",
            path, earlier, key, resolveDelimiter(uploadDirectoryRequest)));
        log.debug(() -> e.getMessage());
        failedFileUploads.add(FailedFileUpload.builder()
                                              .exception(e)
                                              .request(uploadFileRequest)
                                              .build());
        return CompletableFutureUtils.failedFuture(e);
    }

    /**
     * The S3 object a request writes to. A bucket name can't contain "\0", so joining on it can't make two different
     * bucket and key pairs equal.
     */
    private static String objectIdentity(UploadFileRequest uploadFileRequest) {
        PutObjectRequest putObjectRequest = uploadFileRequest.putObjectRequest();
        return putObjectRequest.bucket() + "\0" + putObjectRequest.key();
    }

    /**
     * Whether {@code delimiter} can occur inside a single file or directory name on {@code fileSystem}. Only then can two
     * different files derive the same key. "/" is excluded on every file system because it is not allowed in a name on Unix
     * or on Windows, where it is also accepted as a separator.
     */
    @SdkTestInternalApi
    static boolean delimiterCanOccurInFileName(String delimiter, FileSystem fileSystem) {
        return !delimiter.equals(fileSystem.getSeparator()) && !"/".equals(delimiter);
    }

    private static String resolveDelimiter(UploadDirectoryRequest uploadDirectoryRequest) {
        return uploadDirectoryRequest.s3Delimiter()
                                     .filter(s -> !s.isEmpty())
                                     .orElse(DEFAULT_DELIMITER);
    }

    private Stream<Path> listFiles(Path directory, UploadDirectoryRequest request) {

        try {
            boolean followSymbolicLinks = transferConfiguration.resolveUploadDirectoryFollowSymbolicLinks(request);
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

    private String getRelativePathName(Path source, int directoryNameCount, Path path, String delimiter) {
        String relativePathName = path.subpath(directoryNameCount,
                                               path.getNameCount()).toString();

        String separator =  source.getFileSystem().getSeparator();

        // Optimization for the case where separator equals to the delimiter: there is no need to call String#replace which
        // invokes Pattern#compile in Java 8
        if (delimiter.equals(separator)) {
            return relativePathName;
        }

        return StringUtils.replace(relativePathName, separator, delimiter);
    }

    private UploadFileRequest constructUploadRequest(UploadDirectoryRequest uploadDirectoryRequest,
                                                     int directoryNameCount,
                                                     Path path) {
        String delimiter = resolveDelimiter(uploadDirectoryRequest);

        String prefix = uploadDirectoryRequest.s3Prefix()
                                              .map(s -> normalizePrefix(s, delimiter))
                                              .orElse(DEFAULT_PREFIX);

        String relativePathName = getRelativePathName(uploadDirectoryRequest.source(),
                                                      directoryNameCount,
                                                      path,
                                                      delimiter);
        String key = prefix + relativePathName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                                            .bucket(uploadDirectoryRequest.bucket())
                                                            .key(key)
                                                            .build();

        UploadFileRequest.Builder requestBuilder = UploadFileRequest.builder()
                                                                    .source(path)
                                                                    .putObjectRequest(putObjectRequest);

        uploadDirectoryRequest.uploadFileRequestTransformer().accept(requestBuilder);
        return requestBuilder.build();
    }

}
