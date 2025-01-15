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

package software.amazon.awssdk.services.s3.checksum;

import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ChecksumMode;

public final class HttpChecksumTestUtils {

    private HttpChecksumTestUtils() {
    }

    public static Stream<Arguments> getObjectChecksumValidationParams() {
        return Stream.of(
            Arguments.of(ResponseChecksumValidation.WHEN_SUPPORTED, null, ChecksumAlgorithm.CRC32, true, null,
                         "responseChecksumWhenSupported_checksumModeNotEnabledAndHasChecksum_shouldUseMd5"),

            Arguments.of(ResponseChecksumValidation.WHEN_SUPPORTED, null, null, true, null,
                         "responseChecksumWhenSupported_checksumModeNotEnabledAndNoChecksum_shouldUseMd5"),

            Arguments.of(ResponseChecksumValidation.WHEN_SUPPORTED, ChecksumMode.ENABLED, ChecksumAlgorithm.CRC32,
                         false, "x-amz-checksum-crc32",
                         "responseChecksumWhenSupported_checksumModeEnabledAndHasChecksum_shouldNotUseMd5"),

            Arguments.of(ResponseChecksumValidation.WHEN_SUPPORTED, ChecksumMode.ENABLED, null,
                         false, null,
                         "responseChecksumWhenSupported_checksumModeEnabledAndNoChecksum_shouldNotUseMd5"),

            Arguments.of(ResponseChecksumValidation.WHEN_REQUIRED, null, ChecksumAlgorithm.CRC32, false, null,
                         "responseChecksumWhenRequired_checksumModeNotEnabled_shouldNotUseMd5"),

            Arguments.of(ResponseChecksumValidation.WHEN_REQUIRED, ChecksumMode.ENABLED, ChecksumAlgorithm.CRC32,
                         false, null,
                         "responseChecksumWhenRequired_checksumModeEnabled_shouldNotUseToMd5"));
    }

    /**
     * Checksum is optional for putObject
     */
    public static Stream<Arguments> putObjectChecksumCalculationParams() {
        return Stream.of(Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, null, null, "x-amz-checksum-crc32",
                                      "requestChecksumWhenSupported_checksumAlgorithmAndChecksumValueNotProvided_shouldAddCrc32ChecksumTrailerByDefault"),

                         Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, ChecksumAlgorithm.SHA1, null,
                                      "x-amz-checksum-sha1",
                                      "requestChecksumWhenSupported_checksumAlgorithmProvided_shouldHonor"),

                         Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, null, "crUfeA==",
                                      null,
                                      "requestChecksumWhenSupported_checksumValueProvided_shouldHonor"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, null, null, null,
                                      "requestChecksumWhenRequired_checksumAlgorithmAndChecksumValueNotProvided_shouldNotAddChecksum"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, null, "crUfeA==",
                                      null,
                                      "requestChecksumWhenRequired_checksumValueProvided_shouldAddChecksumTrailer"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, ChecksumAlgorithm.CRC32_C, null,
                                      "x-amz-checksum-crc32c",
                                      "requestChecksumWhenRequired_checksumAlgorithmProvided_shouldAddChecksumTrailer"));
    }

    /**
     * Checksum header is required for putObjectLifeCycle
     */
    public static Stream<Arguments> putObjectLifecycleChecksumCalculationParams() {
        return Stream.of(Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, null, null,
                                      "x-amz-checksum-crc32",
                                      "requestChecksumWhenSupported_checksumAlgorithmAndValueNotProvided_shouldAddCrc32ChecksumHeaderByDefault"),

                         Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, ChecksumAlgorithm.SHA1, null,
                                      "x-amz-checksum-sha1",
                                      "requestChecksumWhenSupported_checksumAlgorithmProvided_shouldHonor"),

                         Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, null, "7khuxQ==",
                                      "x-amz-checksum-crc32c",
                                      "requestChecksumWhenSupported_checksumValueProvided_shouldHonor"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, null, null,
                                      "x-amz-checksum-crc32",
                                      "requestChecksumWhenRequired_checksumAlgorithmAndValueNotProvided_shouldAddCrc32Checksum"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, null, "7khuxQ==",
                                      "x-amz-checksum-crc32c",
                                      "requestChecksumWhenRequired_checksumValueProvided_shouldHonor"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, ChecksumAlgorithm.CRC32_C, null,
                                      "x-amz-checksum-crc32c",
                                      "requestChecksumWhenRequired_checksumAlgorithmProvided_shouldAddChecksumHeader"));
    }


}
