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

package software.amazon.awssdk.codegen.lite;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringUtils;

public final class Utils {

    private Utils() {
    }

    public static String capitalize(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        return name.length() < 2 ? StringUtils.upperCase(name) : StringUtils.upperCase(name.substring(0, 1))
                                                                 + name.substring(1);
    }

    public static File createDirectory(String path) {
        if (isNullOrEmpty(path)) {
            throw new IllegalArgumentException(
                    "Invalid path directory. Path directory cannot be null or empty");
        }

        File dir = new File(path);
        createDirectory(dir);
        return dir;
    }

    public static void createDirectory(File dir) {
        if (!(dir.exists())) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Not able to create directory. "
                        + dir.getAbsolutePath());
            }
        }
    }

    public static File createFile(String dir, String fileName) throws IOException {

        if (isNullOrEmpty(fileName)) {
            throw new IllegalArgumentException(
                    "Invalid file name. File name cannot be null or empty");
        }

        createDirectory(dir);

        File file = new File(dir, fileName);

        if (!(file.exists())) {
            if (!(file.createNewFile())) {
                throw new RuntimeException("Not able to create file . "
                        + file.getAbsolutePath());
            }
        }

        return file;
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static void closeQuietly(Closeable closeable) {
        IoUtils.closeQuietly(closeable, null);
    }
}
