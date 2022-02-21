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

import java.net.URI;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;

public class HttpChecksumResolverTest {

    HttpChecksum CRC32_STREAMING_CHECKSUM = HttpChecksum.builder()
                                                        .requestChecksumRequired(true)
                                                        .responseAlgorithms("crc32c", "crc32")
                                                        .requestAlgorithm("crc32")
                                                        .requestValidationMode("ENABLED").build();

    @Test
    public void testResolvedChecksumSpecsWithAllTraitsSet() {

        ExecutionAttributes executionAttributes = getExecutionAttributeWithAllFieldsSet();
        ChecksumSpecs resolvedChecksumSpecs = HttpChecksumResolver.getResolvedChecksumSpecs(executionAttributes);
        ChecksumSpecs expectedChecksum = ChecksumSpecs.builder()
                                                      .isValidationEnabled(true)
                                                      .isRequestChecksumRequired(true)
                                                      .headerName("x-amz-checksum-crc32")
                                                      .algorithm(Algorithm.CRC32)
                                                      .responseValidationAlgorithms(Arrays.asList(Algorithm.CRC32C,
                                                                                                  Algorithm.CRC32))
                                                      .build();
        Assert.assertEquals(expectedChecksum, resolvedChecksumSpecs);
    }

    private ExecutionAttributes getExecutionAttributeWithAllFieldsSet() {
        return ExecutionAttributes.builder().put(
            SdkInternalExecutionAttribute.HTTP_CHECKSUM, CRC32_STREAMING_CHECKSUM).build();
    }

    @Test
    public void testResolvedChecksumSpecsWithOnlyHttpChecksumRequired() {

        ExecutionAttributes executionAttributes = ExecutionAttributes.builder().put(
            SdkInternalExecutionAttribute.HTTP_CHECKSUM, HttpChecksum.builder()
                                                                     .requestChecksumRequired(true)
                                                                     .build()).build();
        ChecksumSpecs resolvedChecksumSpecs = HttpChecksumResolver.getResolvedChecksumSpecs(executionAttributes);
        ChecksumSpecs expectedChecksum = ChecksumSpecs.builder()
                                                      .isRequestChecksumRequired(true)
                                                      .build();
        Assert.assertEquals(expectedChecksum, resolvedChecksumSpecs);
    }

    @Test
    public void testResolvedChecksumSpecsWithDefaults() {
        ExecutionAttributes executionAttributes = ExecutionAttributes.builder().put(
            SdkInternalExecutionAttribute.HTTP_CHECKSUM, HttpChecksum.builder().build()).build();
        ChecksumSpecs resolvedChecksumSpecs = HttpChecksumResolver.getResolvedChecksumSpecs(executionAttributes);
        ChecksumSpecs expectedChecksum = ChecksumSpecs.builder()
                                                      .build();
        Assert.assertEquals(expectedChecksum, resolvedChecksumSpecs);
    }

    @Test
    public void headerBasedChecksumAlreadyPresent() {

        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                                                  .uri(URI.create("http://localhost:12345/"))
                                                  .appendHeader("x-amz-checksum-crc32", "checksum_data")
                                                  .method(SdkHttpMethod.HEAD)
                                                  .build();
        ExecutionAttributes executionAttributes = getExecutionAttributeWithAllFieldsSet();
        ChecksumSpecs checksumSpecs =
            HttpChecksumUtils.checksumSpecWithRequestAlgorithm(executionAttributes).orElse(null);
        boolean checksumHeaderAlreadyUpdated = HttpChecksumUtils.isHttpChecksumPresent(sdkRequest, checksumSpecs);
        Assert.assertTrue(checksumHeaderAlreadyUpdated);
    }

    @Test
    public void headerBasedChecksumWithDifferentHeader() {

        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                                                  .uri(URI.create("http://localhost:12345/"))
                                                  .appendHeader("x-amz-checksum-sha256", "checksum_data")
                                                  .method(SdkHttpMethod.HEAD)
                                                  .build();
        ExecutionAttributes executionAttributes = getExecutionAttributeWithAllFieldsSet();
        ChecksumSpecs checksumSpecs =
            HttpChecksumUtils.checksumSpecWithRequestAlgorithm(executionAttributes).orElse(null);
        boolean checksumHeaderAlreadyUpdated = HttpChecksumUtils.isHttpChecksumPresent(sdkRequest,
                                                                                       checksumSpecs);
        Assert.assertFalse(checksumHeaderAlreadyUpdated);
    }

    @Test
    public void trailerBasedChecksumAlreadyPresent() {

        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                                                  .uri(URI.create("http://localhost:12345/"))
                                                  .appendHeader("x-amz-trailer", "x-amz-checksum-crc32")
                                                  .method(SdkHttpMethod.HEAD)
                                                  .build();
        ExecutionAttributes executionAttributes = getExecutionAttributeWithAllFieldsSet();
        ChecksumSpecs checksumSpecs =
            HttpChecksumUtils.checksumSpecWithRequestAlgorithm(executionAttributes).orElse(null);
        boolean checksumHeaderAlreadyUpdated = HttpChecksumUtils
            .isHttpChecksumPresent(sdkRequest, checksumSpecs);
        Assert.assertTrue(checksumHeaderAlreadyUpdated);
    }

    @Test
    public void trailerBasedChecksumWithDifferentTrailerHeader() {
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                                                  .uri(URI.create("http://localhost:12345/"))
                                                  .appendHeader("x-amz-trailer", "x-amz-checksum-sha256")
                                                  .method(SdkHttpMethod.HEAD)
                                                  .build();

        ExecutionAttributes executionAttributes = getExecutionAttributeWithAllFieldsSet();
        ChecksumSpecs checksumSpecs =
            HttpChecksumUtils.checksumSpecWithRequestAlgorithm(executionAttributes).orElse(null);
        boolean checksumHeaderAlreadyUpdated = HttpChecksumUtils
            .isHttpChecksumPresent(sdkRequest, checksumSpecs);
        Assert.assertFalse(checksumHeaderAlreadyUpdated);
    }
}


