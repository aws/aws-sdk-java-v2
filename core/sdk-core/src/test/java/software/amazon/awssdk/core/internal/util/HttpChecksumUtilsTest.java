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

package software.amazon.awssdk.core.internal.util;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.core.HttpChecksumConstant.HEADER_FOR_TRAILER_REFERENCE;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksumRequired;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.Pair;

public class HttpChecksumUtilsTest {


    public static Stream<Arguments> httpChecksumCalculationParams() {

        return Stream.of(Arguments.of(false, null, RequestChecksumCalculation.WHEN_SUPPORTED, null, false,
                                      "noChecksumTrait_shouldReturnFalse"),

                         Arguments.of(true, null, RequestChecksumCalculation.WHEN_REQUIRED, null, true,
                                      "hasLegacyHttpChecksumRequired_checksumWhenRequired_shouldReturnTrue"),

                         Arguments.of(true, null, RequestChecksumCalculation.WHEN_SUPPORTED, null, true,
                                      "hasLegacyHttpChecksumRequired_checksumWhenSupported_shouldReturnTrue"),

                         Arguments.of(false, ChecksumSpecs.builder().isRequestChecksumRequired(true).build(),
                                      RequestChecksumCalculation.WHEN_SUPPORTED, null, true,
                                      "hasFlexibleHttpChecksumRequired_checksumWhenSupported_shouldReturnTrue"),

                         Arguments.of(false, ChecksumSpecs.builder().isRequestChecksumRequired(true).build(),
                                      RequestChecksumCalculation.WHEN_REQUIRED, null, true,
                                      "hasFlexibleHttpChecksumRequired_checksumWhenRequired_shouldReturnTrue"),

                         Arguments.of(false, ChecksumSpecs.builder().build(),
                                      RequestChecksumCalculation.WHEN_SUPPORTED, null,
                                      true,
                                      "hasFlexibleChecksumTraitWithNoAlgorithm_checksumWhenSupported_shouldReturnTrue"),

                         Arguments.of(false, ChecksumSpecs.builder().algorithmV2(DefaultChecksumAlgorithm.SHA1).build(),
                                      RequestChecksumCalculation.WHEN_REQUIRED, null,
                                      true,
                                      "hasFlexibleChecksumTraitWithAlgorithm_checksumWhenRequired_shouldReturnTrue"),

                         Arguments.of(false, ChecksumSpecs.builder().algorithmV2(DefaultChecksumAlgorithm.SHA1).build(),
                                      RequestChecksumCalculation.WHEN_SUPPORTED, null,
                                      true,
                                      "hasFlexibleChecksumTraitWithAlgorithm_checksumWhenSupported_shouldReturnTrue"),

                         Arguments.of(false, ChecksumSpecs.builder().build(),
                                      RequestChecksumCalculation.WHEN_REQUIRED, null,
                                      false,
                                      "hasFlexibleChecksumTraitWithNoAlgorithm_checksumWhenRequired_shouldReturnFalse"),

                         Arguments.of(true, null,
                                      RequestChecksumCalculation.WHEN_SUPPORTED, Pair.of("x-amz-checksum-crc32", "somevalue"),
                                      false,
                                      "knownChecksumHeaderPresent_checksumWhenSupported_shouldReturnFalse"),

                         Arguments.of(true, null,
                                      RequestChecksumCalculation.WHEN_SUPPORTED, Pair.of("x-amz-checksum-foobar", "somevalue"),
                                      false,
                                      "randomHeaderWithChecksumPrefixPresent_checksumWhenSupported_shouldReturnFalse"),

                         Arguments.of(true, null,
                                      RequestChecksumCalculation.WHEN_SUPPORTED, Pair.of("test-x-amz-checksum-crc32",
                                                                                         "somevalue"),
                                      true,
                                      "randomHeaderContainingChecksumPrefixPresent_checksumWhenSupported_shouldReturnTrue"),

                         Arguments.of(true, null,
                                      RequestChecksumCalculation.WHEN_REQUIRED, Pair.of("x-amz-checksum-crc32", "somevalue"),
                                      false,
                                      "checksumHeaderPresent_checksumWhenRequired_shouldReturnFalse"),

                         Arguments.of(true, null,
                                      RequestChecksumCalculation.WHEN_REQUIRED, Pair.of("x-amz-checksum-foobar", "somevalue"),
                                      false,
                                      "randomHeaderWithChecksumPrefixPresent_checksumWhenRequired_shouldReturnFalse"),

                         Arguments.of(false, ChecksumSpecs.builder().isRequestStreaming(true).build(),
                                      RequestChecksumCalculation.WHEN_SUPPORTED, Pair.of(HEADER_FOR_TRAILER_REFERENCE, "x-amz-checksum-crc32"),
                                      false,
                                      "checksumTrailerPresent_checksumWhenSupported_shouldReturnFalse"),

                         Arguments.of(false, ChecksumSpecs.builder().isRequestStreaming(true).build(),
                                      RequestChecksumCalculation.WHEN_REQUIRED, Pair.of(HEADER_FOR_TRAILER_REFERENCE, "x-amz-checksum"
                                                                                                           + "-crc32"),
                                      false,
                                      "checksumTrailerPresent_checksumWhenRequired_shouldReturnFalse")
                         );
    }

    @ParameterizedTest(name = "{index} {5}")
    @MethodSource("httpChecksumCalculationParams")
    void isHttpChecksumCalculationNeeded(boolean hasLegacyHttpChecksumTrait,
                                         ChecksumSpecs checksumSpecs,
                                         RequestChecksumCalculation requestChecksumCalculation,
                                         Pair<String, String> additionalHeader,
                                         boolean expectedValue,
                                         String description) {

        SdkHttpFullRequest.Builder httpRequestBuilder = createHttpRequestBuilder();
        if (additionalHeader != null) {
            httpRequestBuilder.putHeader(additionalHeader.left(), additionalHeader.right());
        }

        ExecutionAttributes.Builder executionAttributes =
            ExecutionAttributes.builder()
                               .put(SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS, checksumSpecs)
                               .put(SdkInternalExecutionAttribute.REQUEST_CHECKSUM_CALCULATION, requestChecksumCalculation);

        if (hasLegacyHttpChecksumTrait) {
            executionAttributes.put(SdkInternalExecutionAttribute.HTTP_CHECKSUM_REQUIRED, HttpChecksumRequired.create());
        }

        assertThat(HttpChecksumUtils.isHttpChecksumCalculationNeeded(httpRequestBuilder, executionAttributes.build())).isEqualTo(expectedValue);
    }

    private SdkHttpFullRequest.Builder createHttpRequestBuilder() {
        return SdkHttpFullRequest.builder().contentStreamProvider(RequestBody.fromString("test").contentStreamProvider());
    }
}
