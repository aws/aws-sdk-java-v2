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

package software.amazon.awssdk.transfer.s3.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.s3.model.S3Object;

public class DownloadFilterTest {

    public static Stream<Arguments> s3Objects() {
        return Stream.of(
            Arguments.of(S3Object.builder().key("no-slash-zero-content").size(0L).build(), true),
            Arguments.of(S3Object.builder().key("slash-zero-content/").size(0L).build(), false),
            Arguments.of(S3Object.builder().key("key").size(10L).build(), true)
        );
    }

    @ParameterizedTest
    @MethodSource("s3Objects")
    void allObjectsFilter_shouldWork(S3Object s3Object, boolean result) {
        assertThat(DownloadFilter.allObjects().test(s3Object)).isEqualTo(result);
    }
}
