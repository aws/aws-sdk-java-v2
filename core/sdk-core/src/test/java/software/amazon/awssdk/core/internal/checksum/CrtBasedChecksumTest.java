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

package software.amazon.awssdk.core.internal.checksum;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.zip.Checksum;
import org.junit.Test;
import software.amazon.awssdk.core.internal.checksums.factory.CrtBasedChecksumProvider;

public class CrtBasedChecksumTest {

    @Test
    public void doNot_loadCrc32CrtPathClassesInCore() {
        Checksum checksum = CrtBasedChecksumProvider.createCrc32();
        assertThat(checksum).isNull();
    }

    @Test
    public void doNot_loadCrc32_C_CrtPathClassesInCore() {
        Checksum checksum = CrtBasedChecksumProvider.createCrc32C();
        assertThat(checksum).isNull();
    }
}
