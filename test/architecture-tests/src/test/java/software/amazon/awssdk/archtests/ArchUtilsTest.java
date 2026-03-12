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

package software.amazon.awssdk.archtests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ArchUtilsTest {

    public static Stream<Arguments> params() {
        return Stream.of(
            Arguments.of("software.amazon.awssdk.arns",
                         "software.amazon.awssdk.core",
                         false),
            Arguments.of("software.amazon.awssdk.arns",
                         "software.amazon.awssdk.arns",
                         true),
            Arguments.of("software.amazon.awssdk.arns.internal",
                         "software.amazon.awssdk.core",
                         false),
            Arguments.of("software.amazon.awssdk.core.internal.config",
                         "software.amazon.awssdk.core",
                         true),
            Arguments.of("software.amazon.awssdk.services.s3",
                         "software.amazon.awssdk.services.s3.model",
                         true),
            Arguments.of("software.amazon.awssdk.services.s3",
                         "software.amazon.awssdk.services.s3.internal.transform",
                         true),
            Arguments.of("software.amazon.awssdk.services.s3",
                         "software.amazon.awssdk.services.rds",
                         false),
            Arguments.of("software.amazon.awssdk.services.s3.internal",
                         "software.amazon.awssdk.rds.internal",
                         false));
    }

    @ParameterizedTest
    @MethodSource("params")
    void resideInSameRootPackage(String pkg1, String pkg2, boolean expected) {
        assertThat(ArchUtils.resideInSameRootPackage(pkg1, pkg2)).isEqualTo(expected);
    }
}
