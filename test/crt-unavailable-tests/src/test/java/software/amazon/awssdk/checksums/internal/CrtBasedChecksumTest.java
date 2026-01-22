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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CrtBasedChecksumTest {

    @Test
    void createCrc64WithoutCrtDependency(){

        RuntimeException runtimeException = assertThrows(RuntimeException.class, Crc64NvmeChecksum::new);
        assertEquals("Could not load software.amazon.awssdk.crt.checksums.CRC64NVME. "
        + "Add dependency on 'software.amazon.awssdk.crt:aws-crt' module to enable CRC64NVME feature.",
                     runtimeException.getMessage());

    }

    @Test
    void createCrtBased32CWithoutCrtDependency(){
        assertNull(CrcChecksumProvider.createCrtCrc32C());
    }

}