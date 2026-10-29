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

package software.amazon.awssdk.transfer.s3.internal.utils;

import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class FileUtils {

    private static final Set<PosixFilePermission> OWNER_ONLY =
        Collections.unmodifiableSet(EnumSet.of(OWNER_READ, OWNER_WRITE));

    private FileUtils() {
    }

    /**
     * Writes the given bytes to the given path. On a file system that supports POSIX permissions, a new file is created
     * readable and writable by its owner only, and an existing regular file is restricted to its owner before the new
     * content is written. On other file systems, this behaves like
     * {@link Files#write(Path, byte[], java.nio.file.OpenOption...)}.
     */
    public static void writeOwnerOnly(Path path, byte[] bytes) throws IOException {
        if (path.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            try {
                Files.createFile(path, PosixFilePermissions.asFileAttribute(OWNER_ONLY));
            } catch (FileAlreadyExistsException e) {
                if (Files.isRegularFile(path)) {
                    Files.setPosixFilePermissions(path, OWNER_ONLY);
                }
            }
        }
        Files.write(path, bytes);
    }

    // On certain platforms, File.lastModified() does not contain milliseconds precision, so we need to check the
    // file length as well https://bugs.java.com/bugdatabase/view_bug.do?bug_id=8177809
    public static boolean fileNotModified(long recordedFileContentLength,
                                          Instant recordedFileLastModified,
                                          Path path) {

        File file = path.toFile();
        Instant fileLastModified = Instant.ofEpochMilli(file.lastModified());

        return fileLastModified.equals(recordedFileLastModified)
               && recordedFileContentLength == file.length();
    }
}
