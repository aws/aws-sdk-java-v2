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
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
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
            // DEFAULT request, operation w/o checksum required, WHEN_SUPPORTED config, should set default algorithm CRC32 in
            // header
            Arguments.of(HttpChecksum.builder().build(), S3MetaRequestOptions.MetaRequestType.DEFAULT,
                         RequestChecksumCalculation.WHEN_SUPPORTED, ResponseChecksumValidation.WHEN_SUPPORTED,
                         new ChecksumConfig().withChecksumAlgorithm(ChecksumAlgorithm.CRC32)
                                             .withChecksumLocation(ChecksumConfig.ChecksumLocation.HEADER)),

            // DEFAULT request, operation w/o checksum required, WHEN_REQUIRED config, should pass empty config
            Arguments.of(HttpChecksum.builder().build(), S3MetaRequestOptions.MetaRequestType.DEFAULT,
                         RequestChecksumCalculation.WHEN_REQUIRED, ResponseChecksumValidation.WHEN_SUPPORTED,
                         new ChecksumConfig()),

            // DEFAULT request, operation w/ checksum required, WHEN_REQUIRED config, should set default algorithm CRC32 in
            // header
            Arguments.of(HttpChecksum.builder().requestChecksumRequired(true).build(),
                         S3MetaRequestOptions.MetaRequestType.DEFAULT,
                         RequestChecksumCalculation.WHEN_REQUIRED, ResponseChecksumValidation.WHEN_SUPPORTED,
                         new ChecksumConfig().withChecksumAlgorithm(ChecksumAlgorithm.CRC32)
                                             .withChecksumLocation(ChecksumConfig.ChecksumLocation.HEADER)),

            // DEFAULT request, operation w/ checksum required, WHEN_SUPPORTED config, should set default algorithm CRC32 in
            // header
            Arguments.of(HttpChecksum.builder().requestChecksumRequired(true).build(),
                         S3MetaRequestOptions.MetaRequestType.DEFAULT,
                         RequestChecksumCalculation.WHEN_SUPPORTED, ResponseChecksumValidation.WHEN_SUPPORTED,
                         new ChecksumConfig().withChecksumAlgorithm(ChecksumAlgorithm.CRC32)
                                             .withChecksumLocation(ChecksumConfig.ChecksumLocation.HEADER)),

            // PUT request with SHA256 request algorithm, should set algorithm in trailer
            Arguments.of(HttpChecksum.builder()
                                     .requestAlgorithm("sha256")
                                     .isRequestStreaming(true)
                                     .build(),
                         S3MetaRequestOptions.MetaRequestType.PUT_OBJECT,
                         RequestChecksumCalculation.WHEN_SUPPORTED, ResponseChecksumValidation.WHEN_SUPPORTED,
                         new ChecksumConfig().withChecksumAlgorithm(ChecksumAlgorithm.SHA256)
                                             .withChecksumLocation(ChecksumConfig.ChecksumLocation.TRAILER)),
            // PUT request with CRC64NVME request algorithm, should set algorithm in trailer
            Arguments.of(HttpChecksum.builder()
                                     .requestAlgorithm("crc64nvme")
                                     .isRequestStreaming(true)
                                     .build(),
                         S3MetaRequestOptions.MetaRequestType.PUT_OBJECT,
                         RequestChecksumCalculation.WHEN_SUPPORTED, ResponseChecksumValidation.WHEN_SUPPORTED,
                         new ChecksumConfig().withChecksumAlgorithm(ChecksumAlgorithm.CRC64NVME)
                                             .withChecksumLocation(ChecksumConfig.ChecksumLocation.TRAILER)),

            // PUT request w/o request algorithm, should set default algorithm CRC32 in trailer
            Arguments.of(HttpChecksum.builder().isRequestStreaming(true).build(),
                         S3MetaRequestOptions.MetaRequestType.PUT_OBJECT,
                         RequestChecksumCalculation.WHEN_SUPPORTED, ResponseChecksumValidation.WHEN_SUPPORTED,
                         new ChecksumConfig().withChecksumAlgorithm(ChecksumAlgorithm.CRC32)
                                             .withChecksumLocation(ChecksumConfig.ChecksumLocation.TRAILER)),

            // PUT request w/o request algorithm and checksum disabled, should pass empty config
            Arguments.of(HttpChecksum.builder().build(),
                         S3MetaRequestOptions.MetaRequestType.PUT_OBJECT,
                         RequestChecksumCalculation.WHEN_REQUIRED, ResponseChecksumValidation.WHEN_REQUIRED,
                         new ChecksumConfig()),

            // No HttpChecksum, should pass empty config
            Arguments.of(null,
                         S3MetaRequestOptions.MetaRequestType.PUT_OBJECT,
                         RequestChecksumCalculation.WHEN_SUPPORTED, ResponseChecksumValidation.WHEN_SUPPORTED,
                         new ChecksumConfig()),

            // GET request w/o validate response algorithm, should set validate to true by default and set default algorithm CRC32 in header
            Arguments.of(HttpChecksum.builder().build(),
                         S3MetaRequestOptions.MetaRequestType.GET_OBJECT,
                         RequestChecksumCalculation.WHEN_SUPPORTED, ResponseChecksumValidation.WHEN_SUPPORTED,
                         new ChecksumConfig().withChecksumAlgorithm(ChecksumAlgorithm.CRC32)
                                             .withChecksumLocation(ChecksumConfig.ChecksumLocation.HEADER)
                                             .withValidateChecksum(true)),

            // GET request w/o validate response algorithm, should set validate to true by default and set default algorithm CRC32 in header
            Arguments.of(HttpChecksum.builder()
                                     .responseAlgorithms("sha256", "sha1")
                                     .build(),
                         S3MetaRequestOptions.MetaRequestType.GET_OBJECT,
                         RequestChecksumCalculation.WHEN_SUPPORTED, ResponseChecksumValidation.WHEN_SUPPORTED,
                         new ChecksumConfig().withChecksumAlgorithm(ChecksumAlgorithm.CRC32)
                                             .withChecksumLocation(ChecksumConfig.ChecksumLocation.HEADER)
                                             .withValidateChecksum(true)
                                             .withValidateChecksumAlgorithmList(checksumAlgorithms)),

            // GET request with validate response algorithm, should set default algorithm CRC32 in header
            Arguments.of(HttpChecksum.builder()
                                     .requestValidationMode("true")
                                     .build(),
                         S3MetaRequestOptions.MetaRequestType.GET_OBJECT,
                         RequestChecksumCalculation.WHEN_SUPPORTED, ResponseChecksumValidation.WHEN_SUPPORTED,
                         new ChecksumConfig().withChecksumAlgorithm(ChecksumAlgorithm.CRC32)
                                             .withChecksumLocation(ChecksumConfig.ChecksumLocation.HEADER)
                                             .withValidateChecksum(true)),

            // GET request w/o requestValidationMode and checksum disabled, should pass empty config
            Arguments.of(HttpChecksum.builder().build(),
                         S3MetaRequestOptions.MetaRequestType.GET_OBJECT,
                         RequestChecksumCalculation.WHEN_REQUIRED, ResponseChecksumValidation.WHEN_REQUIRED,
                         new ChecksumConfig().withValidateChecksum(false)),

            // GET request, No HttpChecksum, should pass empty config
            Arguments.of(null,
                         S3MetaRequestOptions.MetaRequestType.GET_OBJECT,
                         RequestChecksumCalculation.WHEN_SUPPORTED, ResponseChecksumValidation.WHEN_SUPPORTED,
                         new ChecksumConfig())

        );
    }

    @ParameterizedTest
    @MethodSource("crtChecksumInput")
    void crtChecksumAlgorithm_differentInput(HttpChecksum checksum,
                                             S3MetaRequestOptions.MetaRequestType type,
                                             RequestChecksumCalculation requestChecksumCalculation,
                                             ResponseChecksumValidation responseChecksumValidation,
                                             ChecksumConfig expected) {
        assertThat(CrtChecksumUtils.checksumConfig(checksum, type, requestChecksumCalculation, responseChecksumValidation))
            .usingRecursiveComparison().isEqualTo(expected);
    }
}
