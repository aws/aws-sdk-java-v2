/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.util;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import org.junit.Test;

/**
 * Test CRC32ChecksumInputStream can calculate CRC32 checksum correctly.
 */
public class Crc32ChecksumInputStreamTest {

    private static final String TEST_DATA = "Jason, Yifei, Zach";

    @Test
    public void testCrc32Checksum() throws IOException {
        CRC32 crc32 = new CRC32();
        crc32.update(TEST_DATA.getBytes());
        long expectedCRC32Checksum = crc32.getValue();
        Crc32ChecksumCalculatingInputStream crc32InputStream =
                new Crc32ChecksumCalculatingInputStream(new ByteArrayInputStream(TEST_DATA.getBytes()));
        while (crc32InputStream.read() != -1) {
            ;
        }
        assertEquals(expectedCRC32Checksum, crc32InputStream.getCrc32Checksum());
    }

}
