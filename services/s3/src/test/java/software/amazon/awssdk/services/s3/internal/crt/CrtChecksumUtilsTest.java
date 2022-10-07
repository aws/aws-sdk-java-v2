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

package software.amazon.awssdk.services.s3.internal.crt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.crt.s3.ChecksumAlgorithm;
import software.amazon.awssdk.crt.s3.S3MetaRequestOptions;

class CrtChecksumUtilsTest {

    private static Stream<Arguments> crtChecksumAlgorithmInput() {
        return Stream.of(
            Arguments.of(HttpChecksum.builder().build(), S3MetaRequestOptions.MetaRequestType.DEFAULT, true, null),
            Arguments.of(HttpChecksum.builder()
                                     .requestAlgorithm("sha256")
                                     .build(),
                         S3MetaRequestOptions.MetaRequestType.PUT_OBJECT,
                         true,
                         ChecksumAlgorithm.SHA256),
            Arguments.of(HttpChecksum.builder().build(), S3MetaRequestOptions.MetaRequestType.PUT_OBJECT, true,
                         ChecksumAlgorithm.CRC32),
            Arguments.of(null, S3MetaRequestOptions.MetaRequestType.PUT_OBJECT, true, null),
            Arguments.of(HttpChecksum.builder().build(), S3MetaRequestOptions.MetaRequestType.PUT_OBJECT, false, null)
        );
    }

    @ParameterizedTest
    @MethodSource("crtChecksumAlgorithmInput")
    void crtChecksumAlgorithm_differentInput(HttpChecksum checksum,
                                             S3MetaRequestOptions.MetaRequestType type,
                                             boolean checksumValidationEnabled,
                                             ChecksumAlgorithm expected) {
        assertThat(CrtChecksumUtils.crtChecksumAlgorithm(checksum,
                                                         type,
                                                         checksumValidationEnabled)).isEqualTo(expected);

    }

    private static Stream<Arguments> validateResponseChecksumInput() {
        return Stream.of(
            Arguments.of(HttpChecksum.builder().build(), S3MetaRequestOptions.MetaRequestType.DEFAULT, true, false),
            Arguments.of(HttpChecksum.builder()
                             .requestValidationMode("true").build(),
                         S3MetaRequestOptions.MetaRequestType.GET_OBJECT,
                         false,
                         true),
            Arguments.of(HttpChecksum.builder().build(), S3MetaRequestOptions.MetaRequestType.GET_OBJECT, true,
                         true),
            Arguments.of(HttpChecksum.builder().build(), S3MetaRequestOptions.MetaRequestType.GET_OBJECT, true, true),
            Arguments.of(HttpChecksum.builder().build(), S3MetaRequestOptions.MetaRequestType.GET_OBJECT, false, false)
        );
    }

    @ParameterizedTest
    @MethodSource("validateResponseChecksumInput")
    void validateResponseChecksum_differentInput(HttpChecksum checksum,
                                                 S3MetaRequestOptions.MetaRequestType type,
                                                 boolean checksumValidationEnabled,
                                                 boolean expected) {
        assertThat(CrtChecksumUtils.validateResponseChecksum(checksum,
                                                             type,
                                                             checksumValidationEnabled)).isEqualTo(expected);

    }

}
