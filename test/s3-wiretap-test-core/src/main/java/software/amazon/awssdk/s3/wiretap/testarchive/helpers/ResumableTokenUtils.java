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

package software.amazon.awssdk.s3.wiretap.testarchive.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import software.amazon.awssdk.s3.wiretap.client.Payload;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

public final class ResumableTokenUtils {

    public static final Instant FILE_LAST_MODIFIED = Instant.parse("2025-08-01T00:00:00Z");

    private ResumableTokenUtils() {
    }

    public static ResumableFileUpload upload(UploadFileRequest request, String uploadId,
                                             long partSize, long totalParts, long transferredParts) {
        return upload(request, uploadId, partSize, totalParts, transferredParts,
                FILE_LAST_MODIFIED, length(request.source()));
    }

    public static ResumableFileUpload upload(UploadFileRequest request, String uploadId,
                                             long partSize, long totalParts, long transferredParts,
                                             Instant fileLastModified, long fileLength) {
        ResumableFileUpload.Builder builder = ResumableFileUpload.builder()
                .uploadFileRequest(request)
                .fileLastModified(fileLastModified)
                .fileLength(fileLength)
                .partSizeInBytes(partSize)
                .totalParts(totalParts)
                .transferredParts(transferredParts);
        if (uploadId != null) {
            builder.multipartUploadId(uploadId);
        }
        return builder.build();
    }

    public static ResumableFileUpload uploadWithoutToken(UploadFileRequest request) {
        return ResumableFileUpload.builder()
                .uploadFileRequest(request)
                .fileLastModified(FILE_LAST_MODIFIED)
                .fileLength(length(request.source()))
                .build();
    }

    public static ResumableFileDownload download(DownloadFileRequest request, long bytesTransferred,
                                                 long totalSize, String objectETag,
                                                 Instant objectLastModified) {
        return download(request, bytesTransferred, totalSize, objectETag, objectLastModified,
                FILE_LAST_MODIFIED);
    }

    public static ResumableFileDownload download(DownloadFileRequest request, long bytesTransferred,
                                                 long totalSize, String objectETag,
                                                 Instant objectLastModified,
                                                 Instant fileLastModified) {
        ResumableFileDownload.Builder builder = ResumableFileDownload.builder()
                .downloadFileRequest(request)
                .bytesTransferred(bytesTransferred)
                .totalSizeInBytes(totalSize)
                .s3ObjectLastModified(objectLastModified)
                .fileLastModified(fileLastModified);
        if (objectETag != null) {
            builder.s3ObjectEtag(objectETag);
        }
        return builder.build();
    }

    public static Path pinLastModified(Path file) {
        return setLastModified(file, FILE_LAST_MODIFIED);
    }

    public static Path setLastModified(Path file, Instant when) {
        try {
            Files.setLastModifiedTime(file, FileTime.from(when));
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("could not set mtime on " + file, e);
        }
    }

    public static Instant lastModified(Path file) {
        return Instant.ofEpochMilli(file.toFile().lastModified());
    }

    public static long length(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new UncheckedIOException("could not size " + file, e);
        }
    }

    public static Path overwriteInPlace(Path file, Payload replacement) {
        long before = length(file);
        if (before != replacement.size()) {
            throw new IllegalArgumentException("overwriteInPlace must not change the length: file is "
                    + before + " bytes, replacement is " + replacement.size()
                    + " — a length change is visible to the SDK and would make the test vacuous");
        }
        Instant mtime = lastModified(file);
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE);
             InputStream in = replacement.newInputStream()) {
            channel.position(0);
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) > 0) {
                channel.write(java.nio.ByteBuffer.wrap(buffer, 0, read));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("could not overwrite " + file, e);
        }
        return setLastModified(file, mtime);
    }

    public static Path partialFile(String prefix, Payload payload, int length) {
        return partialFile(prefix, payload, 0, length);
    }

    public static Path partialFile(String prefix, Payload payload, int offset, int length) {
        if (offset < 0 || length < 0 || offset + length > payload.size()) {
            throw new IllegalArgumentException("[offset, offset + length) must lie in [0, "
                    + payload.size() + "], was offset " + offset + ", length " + length);
        }
        try {
            Path file = Files.createTempFile(prefix, ".dat");
            Files.write(file, payload.toByteArray(offset, length));
            return pinLastModified(file);
        } catch (IOException e) {
            throw new UncheckedIOException("could not write partial file", e);
        }
    }

    public static Path freshTempFile(String prefix) {
        try {
            Path file = Files.createTempFile(prefix, ".dat");
            Files.delete(file);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("could not allocate a temp path", e);
        }
    }
}
