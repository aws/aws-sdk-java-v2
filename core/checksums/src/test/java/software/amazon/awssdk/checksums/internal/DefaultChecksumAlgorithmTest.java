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

package software.amazon.awssdk.checksums.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;

public class DefaultChecksumAlgorithmTest {

    @Test
    public void hasCRC32C() {
        Assertions.assertEquals("CRC32C", DefaultChecksumAlgorithm.CRC32C.algorithmId());
    }

    @Test
    public void hasCRC32() {
        assertEquals("CRC32", DefaultChecksumAlgorithm.CRC32.algorithmId());
    }

    @Test
    public void hasMD5() {
        assertEquals("MD5", DefaultChecksumAlgorithm.MD5.algorithmId());
    }

    @Test
    public void hasSHA256() {
        assertEquals("SHA256", DefaultChecksumAlgorithm.SHA256.algorithmId());
    }

    @Test
    public void hasSHA1() {
        assertEquals("SHA1", DefaultChecksumAlgorithm.SHA1.algorithmId());
    }
}

