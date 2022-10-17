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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Utilities for computing the SHA-256 checksums of various binary objects.
 */
public final class ChecksumUtils {

    public static final int KB = 1024;

    public static byte[] computeCheckSum(InputStream is) throws IOException {
        MessageDigest instance = createMessageDigest();

        byte buff[] = new byte[16384];
        int read;
        while ((read = is.read(buff)) != -1) {
            instance.update(buff, 0, read);
        }

        return instance.digest();
    }

    public static byte[] computeCheckSum(ByteBuffer bb) {
        MessageDigest instance = createMessageDigest();

        instance.update(bb);

        bb.rewind();

        return instance.digest();
    }

    public static byte[] computeCheckSum(List<ByteBuffer> buffers) {
        MessageDigest instance = createMessageDigest();

        buffers.forEach(bb -> {
            instance.update(bb);
            bb.rewind();
        });

        return instance.digest();
    }

    private static MessageDigest createMessageDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to create SHA-256 MessageDigest instance", e);
        }
    }

    public static String calculatedChecksum(String contentString, Algorithm algorithm) {
        SdkChecksum sdkChecksum = SdkChecksum.forAlgorithm(algorithm);
        for (byte character : contentString.getBytes(StandardCharsets.UTF_8)) {
            sdkChecksum.update(character);
        }
        return BinaryUtils.toBase64(sdkChecksum.getChecksumBytes());
    }

    public static String createDataOfSize(int dataSize, char contentCharacter) {
        return IntStream.range(0, dataSize).mapToObj(i -> String.valueOf(contentCharacter)).collect(Collectors.joining());
    }
}
