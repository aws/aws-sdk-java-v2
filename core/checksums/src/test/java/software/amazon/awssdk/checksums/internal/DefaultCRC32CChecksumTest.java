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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class DefaultCRC32CChecksumTest extends Crc32CChecksumTest {

    @BeforeEach
    public void setUp() {
        sdkChecksum = Crc32cProvider.create();
    }


    @AfterEach
    void tearDown() {
        sdkChecksum = null;
        // Try to force garbage collection to clear weak references
        System.gc();
        System.runFinalization();
    }

}
