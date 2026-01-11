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

package software.amazon.awssdk.services.s3.checksums;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.checksums.Md5Checksum;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.internal.checksums.S3ChecksumValidatingInputStream;
import software.amazon.awssdk.utils.IoUtils;

public class S3ChecksumValidatingInputStreamTest {
    private static final int TEST_DATA_SIZE = 32;
    private static final int CHECKSUM_SIZE = 16;

    private static byte[] testData;
    private static byte[] testDataWithoutChecksum;

    @BeforeAll
    public static void populateData() {
        testData = new byte[TEST_DATA_SIZE + CHECKSUM_SIZE];
        for (int i = 0; i < TEST_DATA_SIZE; i++) {
            testData[i] = (byte)(i & 0x7f);
        }

        Md5Checksum checksum = new Md5Checksum();
        checksum.update(testData, 0, TEST_DATA_SIZE);
        byte[] checksumBytes = checksum.getChecksumBytes();

        for (int i = 0; i < CHECKSUM_SIZE; i++) {
            testData[TEST_DATA_SIZE + i] = checksumBytes[i];
        }

        testDataWithoutChecksum = Arrays.copyOfRange(testData, 0, TEST_DATA_SIZE);
    }

    @Test
    public void validChecksumSucceeds() throws IOException {
        InputStream validatingInputStream = newValidatingStream(testData, TEST_DATA_SIZE, CHECKSUM_SIZE);
        byte[] dataFromValidatingStream = IoUtils.toByteArray(validatingInputStream);

        assertArrayEquals(testDataWithoutChecksum, dataFromValidatingStream);
    }

    @Test
    public void emptyObjectSingleByteReadReturnsEOF() throws IOException {
        Md5Checksum checksum = new Md5Checksum();
        byte[] checksumBytes = checksum.getChecksumBytes();
        byte[] emptyWithChecksum = new byte[CHECKSUM_SIZE];

        for (int i = 0; i < CHECKSUM_SIZE; i++) {
            emptyWithChecksum[i] = checksumBytes[i];
        }

        InputStream validatingInputStream = newValidatingStream(emptyWithChecksum, 0, CHECKSUM_SIZE);

        assertEquals(-1, validatingInputStream.read());
    }

    @Test
    public void emptyObjectByteArrayReadReturnsEOF() throws IOException {
        Md5Checksum checksum = new Md5Checksum();
        byte[] checksumBytes = checksum.getChecksumBytes();
        byte[] emptyWithChecksum = new byte[CHECKSUM_SIZE];

        for (int i = 0; i < CHECKSUM_SIZE; i++) {
            emptyWithChecksum[i] = checksumBytes[i];
        }

        InputStream validatingInputStream = newValidatingStream(emptyWithChecksum, 0, CHECKSUM_SIZE);
        byte[] buffer = new byte[1];

        assertEquals(-1, validatingInputStream.read(buffer));
    }

    @Test
    public void invalidChecksumFails() throws IOException {
        for (int i = 0; i < testData.length; i++) {
            // Make sure that corruption of any byte in the test data causes a checksum validation failure.
            byte[] corruptedChecksumData = Arrays.copyOf(testData, testData.length);
            corruptedChecksumData[i] = (byte) ~corruptedChecksumData[i];

            InputStream validatingInputStream = newValidatingStream(corruptedChecksumData, TEST_DATA_SIZE, CHECKSUM_SIZE);

            try {
                IoUtils.toByteArray(validatingInputStream);
                fail("Corruption at byte " + i + " was not detected.");
            } catch (SdkClientException e) {
                // Expected
            }
        }
    }

    private InputStream newValidatingStream(byte[] dataFromS3, int testDataSize, int checkSumSize) {
        return new S3ChecksumValidatingInputStream(new ByteArrayInputStream(dataFromS3),
                                                   new Md5Checksum(),
                                                   testDataSize + checkSumSize);
    }
}