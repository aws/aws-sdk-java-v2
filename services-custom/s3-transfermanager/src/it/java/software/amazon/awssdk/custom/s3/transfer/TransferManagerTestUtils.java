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

package software.amazon.awssdk.custom.s3.transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import software.amazon.awssdk.utils.BinaryUtils;

public final class TransferManagerTestUtils {


    private TransferManagerTestUtils() {

    }

    public static void tryDeleteFiles(Path... files) {
        for (Path file : files) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                System.err.println("Could not delete file " + file);
            }
        }
    }

    public static String computeMd5(Path file, MessageDigest messageDigest) {
        try (InputStream is = Files.newInputStream(file, StandardOpenOption.READ)) {
            messageDigest.reset();
            byte[] buff = new byte[4096];
            int read;
            while ((read = is.read(buff)) != -1) {
                messageDigest.update(buff, 0, read);
            }
            return BinaryUtils.toBase64(messageDigest.digest());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
