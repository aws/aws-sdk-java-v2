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

package software.amazon.awssdk.awscore.checksum;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

import static org.assertj.core.api.Assertions.assertThat;

public class AwsSignerWithChecksumTest {

    final ChecksumSpecs SHA_256_HEADER = getCheckSum(Algorithm.SHA256, false, "header-sha256");
    final ChecksumSpecs SHA_256_TRAILER = getCheckSum(Algorithm.SHA256, true, "trailer-sha256");
    private final Aws4Signer signer = Aws4Signer.create();
    private final AwsBasicCredentials credentials = AwsBasicCredentials.create("access", "secret");

    @Test
    public void signingWithChecksumWithSha256ShouldHaveChecksumInHeaders() throws Exception {
        SdkHttpFullRequest.Builder request = generateBasicRequest("abc");
        ExecutionAttributes executionAttributes = getExecutionAttributes(SHA_256_HEADER);
        SdkHttpFullRequest signed = signer.sign(request.build(), executionAttributes);
        final Optional<String> checksumHeader = signed.firstMatchingHeader("header-sha256");
        assertThat(checksumHeader).hasValue("ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=");
    }

    @Test
    public void signingWithNoChecksumHeaderAlgorithmShouldNotAddChecksumInHeaders() throws Exception {
        SdkHttpFullRequest.Builder request = generateBasicRequest("abc");
        ExecutionAttributes executionAttributes = getExecutionAttributes(null);
        SdkHttpFullRequest signed = signer.sign(request.build(), executionAttributes);
        assertThat(signed.firstMatchingHeader("header-sha256")).isNotPresent();
        assertThat(signed.firstMatchingHeader("trailer-sha256")).isNotPresent();
    }

    @Test
    public void signingWithNoChecksumShouldNotHaveChecksumInHeaders() throws Exception {
        SdkHttpFullRequest.Builder request = generateBasicRequest("abc");
        ExecutionAttributes executionAttributes = getExecutionAttributes(null);
        SdkHttpFullRequest signed = signer.sign(request.build(), executionAttributes);
        assertThat(signed.firstMatchingHeader("header-sha256")).isNotPresent();
        assertThat(signed.firstMatchingHeader("trailer-sha25")).isNotPresent();
    }

    private ExecutionAttributes getExecutionAttributes(ChecksumSpecs checksumSpecs) {
        ExecutionAttributes executionAttributes =
            ExecutionAttributes.builder()
                               .put(AwsSignerExecutionAttribute.AWS_CREDENTIALS, credentials)
                               .put(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, "demo")
                               .put(AwsSignerExecutionAttribute.SIGNING_REGION, Region.of("us-east-1"))
                               .put(SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS, checksumSpecs)
                               .build();
        return executionAttributes;
    }

    private SdkHttpFullRequest.Builder generateBasicRequest(String stringInput) {
        return SdkHttpFullRequest.builder()
                                 .contentStreamProvider(() -> {
                                     return new ByteArrayInputStream(stringInput.getBytes());
                                 })
                                 .method(SdkHttpMethod.POST)
                                 .putHeader("Host", "demo.us-east-1.amazonaws.com")
                                 .putHeader("x-amz-archive-description", "test  test")
                                 .encodedPath("/")
                                 .uri(URI.create("http://demo.us-east-1.amazonaws.com"));
    }

    private ChecksumSpecs getCheckSum(Algorithm algorithm, boolean isStreamingRequest, String headerName) {
        return ChecksumSpecs.builder().algorithm(algorithm)
                            .isRequestStreaming(isStreamingRequest).headerName(headerName).build();
    }
}
