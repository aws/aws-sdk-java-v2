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

package software.amazon.awssdk.core.checksum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.utils.BinaryUtils;

class ChecksumInstantiationTest {

    static final String TEST_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static Stream<Arguments> provideAlgorithmAndTestStringChecksums() {
        return Stream.of(
            Arguments.of(Algorithm.SHA1, "761c457bf73b14d27e9e9265c46f4b4dda11f940"),
            Arguments.of(Algorithm.SHA256, "db4bfcbd4da0cd85a60c3c37d3fbd8805c77f15fc6b1fdfe614ee0a7c8fdb4c0"),
            Arguments.of(Algorithm.CRC32, "000000000000000000000000000000001fc2e6d2"),
            Arguments.of(Algorithm.CRC32C, "00000000000000000000000000000000a245d57d")
        );
    }

    @ParameterizedTest
    @MethodSource("provideAlgorithmAndTestStringChecksums")
    void validateCheckSumValues(Algorithm algorithm, String expectedValue) {
        final SdkChecksum sdkChecksum = SdkChecksum.forAlgorithm(algorithm);
        final byte[] bytes = TEST_STRING.getBytes(StandardCharsets.UTF_8);
        sdkChecksum.update(bytes, 0, bytes.length);
        assertThat(getAsString(sdkChecksum.getChecksumBytes())).isEqualTo(expectedValue);
    }


    private static Stream<Arguments> provideAlgorithmAndTestBaseEncodedValue() {
        return Stream.of(
            Arguments.of(Algorithm.CRC32C, "Nks/tw=="),
            Arguments.of(Algorithm.CRC32, "NSRBwg=="),
            Arguments.of(Algorithm.SHA1, "qZk+NkcGgWq6PiVxeFDCbJzQ2J0="),
            Arguments.of(Algorithm.SHA256, "ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=")

        );
    }

    @ParameterizedTest
    @MethodSource("provideAlgorithmAndTestBaseEncodedValue")
    void validateEncodedBase64ForAlgorithm(Algorithm algorithm, String expectedValue) {
        SdkChecksum sdkChecksum = SdkChecksum.forAlgorithm(algorithm);
        sdkChecksum.update("abc".getBytes(StandardCharsets.UTF_8));
        String toBase64 = BinaryUtils.toBase64(sdkChecksum.getChecksumBytes());
        assertThat(toBase64).isEqualTo(expectedValue);
    }

    private static Stream<Arguments> provideAlgorithmAndTestMarkReset() {
        return Stream.of(
            Arguments.of(Algorithm.CRC32C, "Nks/tw=="),
            Arguments.of(Algorithm.CRC32, "NSRBwg=="),
            Arguments.of(Algorithm.SHA1, "qZk+NkcGgWq6PiVxeFDCbJzQ2J0="),
            Arguments.of(Algorithm.SHA256, "ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=")

        );
    }

    @ParameterizedTest
    @MethodSource("provideAlgorithmAndTestMarkReset")
    void validateMarkAndResetForAlgorithm(Algorithm algorithm, String expectedValue) {
        SdkChecksum sdkChecksum = SdkChecksum.forAlgorithm(algorithm);
        sdkChecksum.update("ab".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.mark(3);
        sdkChecksum.update("xyz".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.reset();
        sdkChecksum.update("c".getBytes(StandardCharsets.UTF_8));
        String toBase64 = BinaryUtils.toBase64(sdkChecksum.getChecksumBytes());
        assertThat(toBase64).isEqualTo(expectedValue);
    }

    private static Stream<Arguments> provideAlgorithmAndTestMarkNoReset() {
        return Stream.of(
            Arguments.of(Algorithm.CRC32C, "crUfeA=="),
            Arguments.of(Algorithm.CRC32, "i9aeUg=="),
            Arguments.of(Algorithm.SHA1, "e1AsOh9IyGCa4hLN+2Od7jlnP14="),
            Arguments.of(Algorithm.SHA256, "ZOyIygCyaOW6GjVnihtTFtIS9PNmskdyMlNKiuyjfzw=")

        );
    }

    @ParameterizedTest
    @MethodSource("provideAlgorithmAndTestMarkNoReset")
    void validateMarkForMarkNoReset(Algorithm algorithm, String expectedValue) {
        SdkChecksum sdkChecksum = SdkChecksum.forAlgorithm(algorithm);
        sdkChecksum.update("Hello ".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.mark(3);
        sdkChecksum.update("world".getBytes(StandardCharsets.UTF_8));
        String toBase64 = BinaryUtils.toBase64(sdkChecksum.getChecksumBytes());
        assertThat(toBase64).isEqualTo(expectedValue);
    }

    private static Stream<Arguments> provideAlgorithmAndIntChecksums() {
        return Stream.of(
            Arguments.of(Algorithm.CRC32, "MtcGkw=="),
            Arguments.of(Algorithm.SHA256, "AbpHGcgLb+kRsJGnwFEktk7uzpZOCcBY74+YBdrKVGs="),
            Arguments.of(Algorithm.CRC32C, "OZ97aQ=="),
            Arguments.of(Algorithm.SHA1, "rcg7GeeTSRscbqD9i0bNnzLlkvw=")
        );
    }

    @ParameterizedTest
    @MethodSource("provideAlgorithmAndIntChecksums")
    void validateChecksumWithIntAsInput(Algorithm algorithm, String expectedValue) {
        SdkChecksum sdkChecksum = SdkChecksum.forAlgorithm(algorithm);
        sdkChecksum.update(10);
        assertThat(BinaryUtils.toBase64(sdkChecksum.getChecksumBytes())).isEqualTo(expectedValue);
    }


    private static Stream<Arguments> provide_SHA_AlgorithmAndExpectedException() {
        return Stream.of(
            Arguments.of(Algorithm.SHA256),
            Arguments.of(Algorithm.SHA1)
        );
    }

    @ParameterizedTest
    @MethodSource("provide_SHA_AlgorithmAndExpectedException")
    void validateShaChecksumWithIntAsInput(Algorithm algorithm) {
        SdkChecksum sdkChecksum = SdkChecksum.forAlgorithm(algorithm);
        sdkChecksum.update(10);
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> sdkChecksum.getValue());
    }

    private static Stream<Arguments> provide_CRC_AlgorithmAndExpectedValues() {
        return Stream.of(
            Arguments.of(Algorithm.CRC32, 852952723L),
            Arguments.of(Algorithm.CRC32C, 966753129L)
        );
    }

    @ParameterizedTest
    @MethodSource("provide_CRC_AlgorithmAndExpectedValues")
    void validateCrcChecksumWithIntAsInput(Algorithm algorithm, long expectedValue) {
        SdkChecksum sdkChecksum = SdkChecksum.forAlgorithm(algorithm);
        sdkChecksum.update(10);
        assertThat(sdkChecksum.getValue()).isEqualTo(expectedValue);
    }


    private static Stream<Arguments> provideAlgorithmAndResetWithNoMark() {
        return Stream.of(
            Arguments.of(Algorithm.CRC32C, "AAAAAA=="),
            Arguments.of(Algorithm.CRC32, "AAAAAA=="),
            Arguments.of(Algorithm.SHA1, "2jmj7l5rSw0yVb/vlWAYkK/YBwk="),
            Arguments.of(Algorithm.SHA256, "47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=")
        );
    }

    @ParameterizedTest
    @MethodSource("provideAlgorithmAndResetWithNoMark")
    void validateChecksumWithResetAndNoMark(Algorithm algorithm, String expectedValue) {
        SdkChecksum sdkChecksum = SdkChecksum.forAlgorithm(algorithm);
        sdkChecksum.update("abc".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.reset();
        assertThat(BinaryUtils.toBase64(sdkChecksum.getChecksumBytes())).isEqualTo(expectedValue);
    }

    private String getAsString(byte[] checksumBytes) {
        return String.format("%040x", new BigInteger(1, checksumBytes));
    }

}