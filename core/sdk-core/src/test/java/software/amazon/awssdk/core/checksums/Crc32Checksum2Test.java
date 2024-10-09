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

package software.amazon.awssdk.core.checksums;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

public class Crc32Checksum2Test {
    @Test
    public void test() {
        compareTwoChecksums(RandomStringUtils.random(10000).getBytes(StandardCharsets.UTF_8));
    }

    public void compareTwoChecksums(byte[] input) {
        CRC32 reference = new CRC32();
        reference.update(input, 0, input.length);
        long referenceValue = reference.getValue();

        // Test without reset
        Crc32Checksum2 subject1 = new Crc32Checksum2();
        subject1.update(input, 0, input.length);
        assertThat(subject1.getValue()).isEqualTo(referenceValue);

        // Test with resetting at every point in the stream
        for (int i = 0; i <= input.length; i++) {
            Crc32Checksum2 subject2 = new Crc32Checksum2();
            subject2.update(input, 0, i);
            subject2.mark(0);
            subject2.update(input, 0, 1); // Write some stuff to reset before
            subject2.reset();
            subject2.update(input, i, input.length - i);
            assertThat(subject2.getValue()).isEqualTo(referenceValue);
        }
    }

}