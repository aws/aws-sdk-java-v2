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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.crt.s3.ChecksumAlgorithm;
import software.amazon.awssdk.crt.s3.ChecksumConfig;
import software.amazon.awssdk.crt.s3.S3MetaRequestOptions;

class CrtChecksumUtilsTest {

    private static Stream<Arguments> crtChecksumInput() {
        List<ChecksumAlgorithm> checksumAlgorithms = new ArrayList<>();
        checksumAlgorithms.add(ChecksumAlgorithm.SHA256);
        checksumAlgorithms.add(ChecksumAlgorithm.SHA1);
        return Stream.of(
            // DEFAULT request, should pass empty config
            Arguments.of(HttpChecksum.builder().build(), S3MetaRequestOptions.MetaRequestType.DEFAULT, true,
                         new ChecksumConfig()),

            // PUT request with request algorithm, should set algorithm
            Arguments.of(HttpChecksum.builder()
                                     .requestAlgorithm("sha256")
                                     .build(),
                         S3MetaRequestOptions.MetaRequestType.PUT_OBJECT,
                         true,
                         new ChecksumConfig().withChecksumAlgorithm(ChecksumAlgorithm.SHA256).withChecksumLocation(ChecksumConfig.ChecksumLocation.TRAILER)),

            // PUT request w/o request algorithm, should set default algorithm
            Arguments.of(HttpChecksum.builder().build(), S3MetaRequestOptions.MetaRequestType.PUT_OBJECT, true,
                         new ChecksumConfig().withChecksumAlgorithm(ChecksumAlgorithm.CRC32).withChecksumLocation(ChecksumConfig.ChecksumLocation.TRAILER)),

            // PUT request w/o request algorithm and checksum disabled, should pass empty config
            Arguments.of(HttpChecksum.builder().build(), S3MetaRequestOptions.MetaRequestType.PUT_OBJECT, false,
                         new ChecksumConfig()),

            // No HttpChecksum, should pass empty config
            Arguments.of(null, S3MetaRequestOptions.MetaRequestType.PUT_OBJECT, true,
                         new ChecksumConfig()),

            // GET request w/o validate response algorithm, should set validate to true by default
            Arguments.of(HttpChecksum.builder().build(),
                         S3MetaRequestOptions.MetaRequestType.GET_OBJECT, true,
                         new ChecksumConfig().withValidateChecksum(true)),

            // GET request w/o validate response algorithm, should set validate to true by defaultt
            Arguments.of(HttpChecksum.builder().responseAlgorithms("sha256", "sha1").build(),
                         S3MetaRequestOptions.MetaRequestType.GET_OBJECT, true,
                         new ChecksumConfig().withValidateChecksum(true).withValidateChecksumAlgorithmList(checksumAlgorithms)),

            // GET request with validate response algorithm, should set ChecksumConfig
            Arguments.of(HttpChecksum.builder().requestValidationMode("true").build(),
                         S3MetaRequestOptions.MetaRequestType.GET_OBJECT, true,
                         new ChecksumConfig().withValidateChecksum(true)),

            // GET request w/o requestValidationMode and checksum disabled, should pass empty config
            Arguments.of(HttpChecksum.builder().build(),
                         S3MetaRequestOptions.MetaRequestType.GET_OBJECT, false,
                         new ChecksumConfig().withValidateChecksum(false)),

            // GET request, No HttpChecksum, should pass empty config
            Arguments.of(null,
                         S3MetaRequestOptions.MetaRequestType.GET_OBJECT, true,
                         new ChecksumConfig())

        );
    }

    @ParameterizedTest
    @MethodSource("crtChecksumInput")
    void crtChecksumAlgorithm_differentInput(HttpChecksum checksum,
                                             S3MetaRequestOptions.MetaRequestType type,
                                             boolean checksumValidationEnabled,
                                             ChecksumConfig expected) {
        assertThat(CrtChecksumUtils.checksumConfig(checksum, type, checksumValidationEnabled)).usingRecursiveComparison().isEqualTo(expected);
    }
}
