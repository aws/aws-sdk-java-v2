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

package software.amazon.awssdk.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Utility methods for computing MD5 sums.
 */
@SdkProtectedApi
public final class Md5Utils {
    private static final int SIXTEEN_K = 1 << 14;

    private Md5Utils() {
    }

    /**
     * Computes the MD5 hash of the data in the given input stream and returns
     * it as an array of bytes.
     * Note this method closes the given input stream upon completion.
     */
    public static byte[] computeMD5Hash(InputStream is) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(is);
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[SIXTEEN_K];
            int bytesRead;
            while ((bytesRead = bis.read(buffer, 0, buffer.length)) != -1) {
                messageDigest.update(buffer, 0, bytesRead);
            }
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            // should never get here
            throw new IllegalStateException(e);
        } finally {
            try {
                bis.close();
            } catch (Exception e) {
                LoggerFactory.getLogger(Md5Utils.class).debug("Unable to close input stream of hash candidate: {}", e);
            }
        }
    }

    /**
     * Returns the MD5 in base64 for the data from the given input stream.
     * Note this method closes the given input stream upon completion.
     */
    public static String md5AsBase64(InputStream is) throws IOException {
        return BinaryUtils.toBase64(computeMD5Hash(is));
    }

    /**
     * Computes the MD5 hash of the given data and returns it as an array of
     * bytes.
     */
    public static byte[] computeMD5Hash(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(input);
        } catch (NoSuchAlgorithmException e) {
            // should never get here
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the MD5 in base64 for the given byte array.
     */
    public static String md5AsBase64(byte[] input) {
        return BinaryUtils.toBase64(computeMD5Hash(input));
    }

    /**
     * Computes the MD5 of the given file.
     */
    public static byte[] computeMD5Hash(File file) throws FileNotFoundException, IOException {
        return computeMD5Hash(new FileInputStream(file));
    }

    /**
     * Returns the MD5 in base64 for the given file.
     */
    public static String md5AsBase64(File file) throws FileNotFoundException, IOException {
        return BinaryUtils.toBase64(computeMD5Hash(file));
    }
}
