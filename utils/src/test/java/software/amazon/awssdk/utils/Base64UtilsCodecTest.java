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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;

public class Base64UtilsCodecTest {
    @Test
    public void testVectorsPerRfc4648() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String[] testVectors = {"", "f", "fo", "foo", "foob", "fooba", "foobar"};
        String[] expected = {"", "Zg==", "Zm8=", "Zm9v", "Zm9vYg==", "Zm9vYmE=", "Zm9vYmFy"};

        for (int i = 0; i < testVectors.length; i++) {
            String data = testVectors[i];
            byte[] source = data.getBytes("UTF-8");
            String b64encoded = BinaryUtils.toBase64(data.getBytes("UTF-8"));
            Assert.assertEquals(expected[i], b64encoded);
            byte[] b64 = b64encoded.getBytes("UTF-8");

            byte[] decoded = BinaryUtils.fromBase64Bytes(b64);
            Assert.assertTrue(Arrays.equals(source, decoded));
        }
    }

    @Test
    public void testCodecConsistency() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        byte[] decoded = null;

        for (int h = 0; h < 1000; h++) {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(UUID.randomUUID().toString().getBytes("UTF-8"));
            String b64Encoded = BinaryUtils.toBase64(digest);
            decoded = BinaryUtils.fromBase64(b64Encoded);
            Assert.assertTrue(Arrays.equals(decoded, digest));
        }
    }
}
