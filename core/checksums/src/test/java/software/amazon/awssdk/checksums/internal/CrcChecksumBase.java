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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.utils.BinaryUtils;

abstract class CrcChecksumBase {

    protected SdkChecksum sdkChecksum;

    static final String TEST_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @ParameterizedTest
    @MethodSource("provideParametersForCrcCheckSumValues")
    void crcCheckSumValues(String expectedChecksum) throws UnsupportedEncodingException {
        byte[] bytes = TEST_STRING.getBytes("UTF-8");
        sdkChecksum.update(bytes, 0, bytes.length);
        assertEquals(expectedChecksum, getAsString(sdkChecksum.getChecksumBytes()));
    }

    @ParameterizedTest
    @MethodSource("provideValidateEncodedBase64ForCrc")
    void validateEncodedBase64ForCrc(String expectedChecksum) {
        sdkChecksum.update("abc".getBytes(StandardCharsets.UTF_8));
        String toBase64 = BinaryUtils.toBase64(sdkChecksum.getChecksumBytes());
        assertEquals(expectedChecksum, toBase64);
    }

    @ParameterizedTest
    @MethodSource("provideValidateMarkAndResetForCrc")
    void validateMarkAndResetForCrc(String expectedChecksum) {
        sdkChecksum.update("ab".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.mark(3);
        sdkChecksum.update("xyz".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.reset();
        sdkChecksum.update("c".getBytes(StandardCharsets.UTF_8));
        String toBase64 = BinaryUtils.toBase64(sdkChecksum.getChecksumBytes());
        assertEquals(expectedChecksum, toBase64);
    }

    @ParameterizedTest
    @MethodSource("provideValidateMarkForCrc")
    void validateMarkForCrc(String expectedChecksum) {
        sdkChecksum.update("Hello ".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.mark(4);
        sdkChecksum.update("world".getBytes(StandardCharsets.UTF_8));
        String toBase64 = BinaryUtils.toBase64(sdkChecksum.getChecksumBytes());
        assertEquals(expectedChecksum, toBase64);
    }

    @ParameterizedTest
    @MethodSource("provideValidateSingleMarksForCrc")
    void validateSingleMarksForCrc(String expectedChecksum) {
        sdkChecksum.update("alpha".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.mark(3);
        sdkChecksum.update("beta".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.reset();
        String toBase64 = BinaryUtils.toBase64(sdkChecksum.getChecksumBytes());
        assertEquals(expectedChecksum, toBase64);
    }

    @ParameterizedTest
    @MethodSource("provideValidateMultipleMarksForCrc")
    void validateMultipleMarksForCrc(String expectedChecksum) {
        sdkChecksum.update("alpha".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.mark(3);
        sdkChecksum.update("beta".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.mark(5);
        sdkChecksum.update("gamma".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.reset();
        sdkChecksum.update("delta".getBytes(StandardCharsets.UTF_8));
        String toBase64 = BinaryUtils.toBase64(sdkChecksum.getChecksumBytes());
        //final checksum of "alphabetadelta"
        assertEquals(expectedChecksum, toBase64);
    }

    @ParameterizedTest
    @MethodSource("provideValidateResetWithoutMarkForCrc")
    void validateResetWithoutMarkForCrc(String expectedChecksum) {
        sdkChecksum.update("beta".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.reset();
        sdkChecksum.update("alpha".getBytes(StandardCharsets.UTF_8));
        String toBase64 = BinaryUtils.toBase64(sdkChecksum.getChecksumBytes());
        //checksum of alpha
        assertEquals(expectedChecksum, toBase64);
    }

    private String getAsString(byte[] checksumBytes) {
        return String.format("%040x", new BigInteger(1, checksumBytes));
    }
}


