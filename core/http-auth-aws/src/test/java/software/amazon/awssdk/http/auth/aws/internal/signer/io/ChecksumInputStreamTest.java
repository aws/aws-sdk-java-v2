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

package software.amazon.awssdk.http.auth.aws.internal.signer.io;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.readAll;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.auth.aws.internal.signer.checksums.Crc32Checksum;
import software.amazon.awssdk.http.auth.aws.internal.signer.checksums.Crc64NvmeChecksum;
import software.amazon.awssdk.http.auth.aws.internal.signer.checksums.SdkChecksum;
import software.amazon.awssdk.http.auth.aws.internal.signer.checksums.Sha256Checksum;
import software.amazon.awssdk.utils.BinaryUtils;

public class ChecksumInputStreamTest {

    @Test
    public void read_computesCorrectSha256() {
        String testString = "AWS SDK for Java";
        String expectedDigest = "004c6bbd87e7fe70109b3bc23c8b1ab8f18a8bede0ed38c9233f6cdfd4f7b5d6";

        ByteArrayInputStream backingStream = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8));
        SdkChecksum checksum = new Sha256Checksum();
        ChecksumInputStream inputStream = new ChecksumInputStream(backingStream, Collections.singleton(checksum));

        readAll(inputStream);
        String computedDigest = BinaryUtils.toHex(checksum.getChecksumBytes());

        assertThat(computedDigest).isEqualTo(expectedDigest);
    }

    @Test
    public void read_withMultipleChecksums_shouldComputeCorrectChecksums() {
        String testString = "AWS SDK for Java";
        String expectedSha256Digest = "004c6bbd87e7fe70109b3bc23c8b1ab8f18a8bede0ed38c9233f6cdfd4f7b5d6";
        String expectedCrc32Digest = "4ac37ece";
        String expectedCrc64Digest = "7c05fe704e3e02bc";

        ByteArrayInputStream backingStream = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8));
        SdkChecksum sha256Checksum = new Sha256Checksum();
        SdkChecksum crc32Checksum = new Crc32Checksum();
        SdkChecksum crc64NvmeChecksum = new Crc64NvmeChecksum();
        ChecksumInputStream inputStream = new ChecksumInputStream(backingStream, Arrays.asList(sha256Checksum, crc32Checksum, crc64NvmeChecksum));

        readAll(inputStream);
        String computedSha256Digest = BinaryUtils.toHex(sha256Checksum.getChecksumBytes());
        String computedCrc32Digest = BinaryUtils.toHex(crc32Checksum.getChecksumBytes());
        String computedCrc64NvmeDigest = BinaryUtils.toHex(crc64NvmeChecksum.getChecksumBytes());

        assertThat(computedSha256Digest).isEqualTo(expectedSha256Digest);
        assertThat(computedCrc32Digest).isEqualTo(expectedCrc32Digest);
        assertThat(computedCrc64NvmeDigest).isEqualTo(expectedCrc64Digest);
    }
}
