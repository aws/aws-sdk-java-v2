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

package software.amazon.awssdk.codegen.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class VersionUtilsTest {

    @Test
    public void serviceVersionInfo_redactPatchVersion() {
        String currentVersion = "2.35.13";
        String currentSnapshotVersion = "2.35.13-SNAPSHOT";

        String actualVersion = VersionUtils.convertToMajorMinorX(currentVersion);
        String actualSnapshotVersion = VersionUtils.convertToMajorMinorX(currentSnapshotVersion);
        assertThat(actualVersion).isEqualTo("2.35.x");
        assertThat(actualSnapshotVersion).isEqualTo("2.35.x-SNAPSHOT");
    }
}
