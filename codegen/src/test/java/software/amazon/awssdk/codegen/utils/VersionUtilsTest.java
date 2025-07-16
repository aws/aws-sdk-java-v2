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

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class VersionUtilsTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("versionsTestCases")
    public void serviceVersionInfo_redactPatchVersion(String testName, String versionToTruncate, String expectedVersion) {
        String actualVersion = VersionUtils.convertToMajorMinorX(versionToTruncate);
        assertThat(actualVersion).isEqualTo(expectedVersion);
    }

    private static Stream<Arguments> versionsTestCases() {
        return Stream.of(
            Arguments.of("valid version", "2.35.13", "2.35.x"),
            Arguments.of("valid snapshot version", "2.35.13-SNAPSHOT", "2.35.x-SNAPSHOT"),
            Arguments.of("different major version", "3.5.5", "3.5.x"),
            Arguments.of("invalid version, uses version as-is", "23513", "23513")
        );
    }
}
