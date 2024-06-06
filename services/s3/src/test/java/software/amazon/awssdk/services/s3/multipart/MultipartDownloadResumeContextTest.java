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

package software.amazon.awssdk.services.s3.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.s3.internal.multipart.MultipartDownloadResumeContext;

class MultipartDownloadResumeContextTest {

    @ParameterizedTest
    @MethodSource("source")
    void highest(List<Integer> completedParts, int expectedNextNonCompleted) {
        MultipartDownloadResumeContext context = new MultipartDownloadResumeContext();
        completedParts.forEach(context::addCompletedPart);
        assertThat(context.highestSequentialCompletedPart()).isEqualTo(expectedNextNonCompleted);
    }

    private static Stream<Arguments> source() {
        return Stream.of(
            Arguments.of(Arrays.asList(), 0),
            Arguments.of(Arrays.asList(0), 0),
            Arguments.of(Arrays.asList(1), 1),
            Arguments.of(Arrays.asList(1, 2), 2),
            Arguments.of(Arrays.asList(1, 2, 3), 3),
            Arguments.of(Arrays.asList(1, 2, 3, 4), 4),
            Arguments.of(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 10),
            Arguments.of(Arrays.asList(1, 3, 4, 5), 1),
            Arguments.of(Arrays.asList(1, 2, 4, 5), 2),
            Arguments.of(Arrays.asList(1, 2, 3, 5), 3),
            Arguments.of(Arrays.asList(1, 3, 5), 1),
            Arguments.of(Arrays.asList(1, 4, 5), 1),
            Arguments.of(Arrays.asList(1, 5), 1),
            Arguments.of(Arrays.asList(1, 2, 3, 4, 6, 8, 9), 4),
            Arguments.of(Arrays.asList(2, 4, 6), 0),
            Arguments.of(Arrays.asList(2, 3, 5), 0)
        );
    }

}