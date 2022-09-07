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

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class FileUtils {

    private FileUtils() {
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
