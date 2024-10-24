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


import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

abstract class Crc32CChecksumTest extends CrcChecksumBase {

    protected static Stream<Arguments> provideParametersForCrcCheckSumValues() {
        return Stream.of(
            Arguments.of("00000000000000000000000000000000a245d57d")
        );
    }

    protected static Stream<Arguments> provideValidateEncodedBase64ForCrc() {
        return Stream.of(
            Arguments.of("Nks/tw==")
        );
    }

    protected static Stream<Arguments> provideValidateMarkAndResetForCrc() {
        return Stream.of(
            Arguments.of("Nks/tw==")
        );
    }

    protected static Stream<Arguments> provideValidateMarkForCrc() {
        return Stream.of(
            Arguments.of("crUfeA==")
        );
    }

    protected static Stream<Arguments> provideValidateSingleMarksForCrc() {
        return Stream.of(
            Arguments.of("eNkvgQ==")
        );
    }

    protected static Stream<Arguments> provideValidateMultipleMarksForCrc() {
        return Stream.of(
            Arguments.of("Bf1liQ==")
        );
    }

    protected static Stream<Arguments> provideValidateResetWithoutMarkForCrc() {
        return Stream.of(
            Arguments.of("eNkvgQ==")
        );
    }

}