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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.utils.internal.Base16;
import software.amazon.awssdk.utils.internal.Base16Lower;

/**
 * @author hchar
 */
public class Base16CodecTest {
    @Test
    public void testVectorsPerRfc4648()
        throws Exception {
        String[] testVectors = {"", "f", "fo", "foo", "foob", "fooba", "foobar"};
        String[] expected = {"", "66", "666F", "666F6F", "666F6F62", "666F6F6261", "666F6F626172"};
        for (int i = 0; i < testVectors.length; i++) {
            String data = testVectors[i];
            byte[] source = data.getBytes("UTF-8");
            String b16encoded = Base16.encodeAsString(data.getBytes("UTF-8"));
            assertEquals(expected[i], b16encoded);
            byte[] b16 = b16encoded.getBytes("UTF-8");

            byte[] decoded = Base16.decode(b16);
            assertTrue(Arrays.equals(source, decoded));
            decoded = Base16Lower.decode(b16);
            assertTrue(Arrays.equals(source, decoded));
        }
    }

    @Test
    public void testCodecConsistency()
        throws Exception {
        byte[] decoded = null;

        for (int h = 0; h < 1000; h++) {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(
                    UUID.randomUUID().toString().getBytes("UTF-8")
                                                                     );
            String b16Encoded = Base16.encodeAsString(digest);
            {
                decoded = Base16.decode(b16Encoded);
                assertTrue(Arrays.equals(decoded, digest));
                decoded = Base16Lower.decode(b16Encoded);
                assertTrue(Arrays.equals(decoded, digest));
            }
            {   // test decoding case insensitivity
                decoded = Base16.decode(b16Encoded.toLowerCase());
                assertTrue(Arrays.equals(decoded, digest));
                decoded = Base16Lower.decode(b16Encoded.toLowerCase());
                assertTrue(Arrays.equals(decoded, digest));
            }
        }
    }
}
