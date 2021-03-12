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

package software.amazon.awssdk.services.s3.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Utilities for computing the SHA-256 checksums of various binary objects.
 */
public final class ChecksumUtils {
    public static byte[] computeCheckSum(InputStream is) throws IOException, NoSuchAlgorithmException {
        MessageDigest instance = MessageDigest.getInstance("SHA-256");

        byte buff[] = new byte[16384];
        int read;
        while ((read = is.read(buff)) != -1) {
            instance.update(buff, 0, read);
        }

        return instance.digest();
    }

    public static byte[] computeCheckSum(ByteBuffer bb) throws NoSuchAlgorithmException {
        MessageDigest instance = MessageDigest.getInstance("SHA-256");

        instance.update(bb);

        bb.rewind();

        return instance.digest();
    }

    public static byte[] computeCheckSum(List<ByteBuffer> buffers) throws NoSuchAlgorithmException {
        MessageDigest instance = MessageDigest.getInstance("SHA-256");

        buffers.forEach(bb -> {
            instance.update(bb);
            bb.rewind();
        });

        return instance.digest();
    }
}
