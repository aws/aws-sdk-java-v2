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

package software.amazon.awssdk.transfer.s3.internal.progress;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.OptionalLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ContentRangeParserTest {

    private ContentRangeParser parser;

    @ParameterizedTest
    @MethodSource("argumentProvider")
    void testContentRangeParser(String contentRange, OptionalLong expected) {
        assertThat(ContentRangeParser.totalBytes(contentRange)).isEqualTo(expected);
    }

    static Stream<Arguments> argumentProvider() {
        return Stream.of(
            Arguments.of(null, OptionalLong.empty()),
            Arguments.of("", OptionalLong.empty()),
            Arguments.of("bytes 0-0/1", OptionalLong.of(1)),
            Arguments.of("bytes 1-2/3", OptionalLong.of(3)),
            Arguments.of("bytes 0-23456/890890890", OptionalLong.of(890890890)),
            Arguments.of("bytes 1023-81204/890890890", OptionalLong.of(890890890)),
            Arguments.of("bytes 1023-81204/999999999999999999999999999999", OptionalLong.empty()),
            Arguments.of("bytes 1023-81204/-1234", OptionalLong.empty()),
            Arguments.of("bytes 1023-81204/not-a-number", OptionalLong.empty()),
            Arguments.of("bytes 1-2/*", OptionalLong.empty()),
            Arguments.of("mib 1-2/3", OptionalLong.empty()),
            Arguments.of("mib/bla 1-2/3", OptionalLong.empty()),
            Arguments.of("bla bla bla", OptionalLong.empty()));
    }

}