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

package software.amazon.awssdk.services.s3.internal.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS;
import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.RESOLVED_ENDPOINT;
import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.CHECKSUM_ALGORITHM;
import static software.amazon.awssdk.services.s3.utils.InterceptorTestUtils.modifyHttpRequestContext;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.services.s3.internal.s3express.DefaultS3ExpressHttpSigner;
import software.amazon.awssdk.services.s3.endpoints.internal.KnownS3ExpressEndpointProperty;
import software.amazon.awssdk.services.s3.s3express.S3ExpressSessionCredentials;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3ExpressChecksumInterceptorTest {

    private final S3ExpressChecksumInterceptor interceptor = new S3ExpressChecksumInterceptor();

    @Test
    public void putObjectRequest_withNoChecksumSet_addsCrc32() {
        PutObjectRequest request = PutObjectRequest.builder().build();
        Algorithm algorithm = null;
        ExecutionAttributes executionAttributes = createExecutionAttributes(algorithm);

        Context.ModifyRequest modifyRequestContext = () -> request;
        SdkRequest modifiedRequest = interceptor.modifyRequest(modifyRequestContext, executionAttributes);
        assertThat(executionAttributes.getAttribute(SELECTED_AUTH_SCHEME).authSchemeOption().signerProperty(CHECKSUM_ALGORITHM)).isEqualTo(DefaultChecksumAlgorithm.CRC32);

        Context.ModifyHttpRequest modifyHttpRequestContext = modifyHttpRequestContext(modifiedRequest);
        SdkHttpRequest sdkHttpRequest = interceptor.modifyHttpRequest(modifyHttpRequestContext, executionAttributes);
        assertThat(sdkHttpRequest.firstMatchingHeader("x-amz-sdk-checksum-algorithm")).contains("CRC32");
    }

    @Test
    public void putObjectRequest_withNonCrc32AlgorithmTypeSet_doesNotAddCrc32() {
        PutObjectRequest request = PutObjectRequest.builder().build();
        Algorithm algorithm = Algorithm.SHA1;
        ExecutionAttributes executionAttributes = createExecutionAttributes(algorithm);

        Context.ModifyRequest modifyRequestContext = () -> request;
        SdkRequest modifiedRequest = interceptor.modifyRequest(modifyRequestContext, executionAttributes);
        assertThat(executionAttributes.getAttribute(SELECTED_AUTH_SCHEME).authSchemeOption().signerProperty(CHECKSUM_ALGORITHM)).isEqualTo(DefaultChecksumAlgorithm.SHA1);

        Context.ModifyHttpRequest modifyHttpRequestContext = modifyHttpRequestContext(modifiedRequest);
        SdkHttpRequest sdkHttpRequest = interceptor.modifyHttpRequest(modifyHttpRequestContext, executionAttributes);
        assertThat(sdkHttpRequest.firstMatchingHeader("x-amz-sdk-checksum-algorithm")).isEmpty();
    }

    @Test
    public void putObjectRequest_withChecksumSha1ValueSet_doesNotAddCrc32() {
        PutObjectRequest request = PutObjectRequest.builder()
                                                   .checksumSHA1("sha1Value")
                                                   .build();
        Algorithm algorithm = null;
        ExecutionAttributes executionAttributes = createExecutionAttributes(algorithm);

        Context.ModifyRequest modifyRequestContext = () -> request;
        SdkRequest modifiedRequest = interceptor.modifyRequest(modifyRequestContext, executionAttributes);
        assertThat(executionAttributes.getAttribute(SELECTED_AUTH_SCHEME).authSchemeOption().signerProperty(CHECKSUM_ALGORITHM)).isNull();

        Context.ModifyHttpRequest modifyHttpRequestContext = modifyHttpRequestContext(modifiedRequest);
        SdkHttpRequest sdkHttpRequest = interceptor.modifyHttpRequest(modifyHttpRequestContext, executionAttributes);
        assertThat(sdkHttpRequest.firstMatchingHeader("x-amz-sdk-checksum-algorithm")).isEmpty();
    }

    @Test
    public void putObjectRequest_withChecksumSha256ValueSet_doesNotAddCrc32() {
        PutObjectRequest request = PutObjectRequest.builder()
                                                   .checksumSHA256("sha256Value")
                                                   .build();
        Algorithm algorithm = null;
        ExecutionAttributes executionAttributes = createExecutionAttributes(algorithm);

        Context.ModifyRequest modifyRequestContext = () -> request;
        SdkRequest modifiedRequest = interceptor.modifyRequest(modifyRequestContext, executionAttributes);
        assertThat(executionAttributes.getAttribute(SELECTED_AUTH_SCHEME).authSchemeOption().signerProperty(CHECKSUM_ALGORITHM)).isNull();

        Context.ModifyHttpRequest modifyHttpRequestContext = modifyHttpRequestContext(modifiedRequest);
        SdkHttpRequest sdkHttpRequest = interceptor.modifyHttpRequest(modifyHttpRequestContext, executionAttributes);
        assertThat(sdkHttpRequest.firstMatchingHeader("x-amz-sdk-checksum-algorithm")).isEmpty();
    }

    @Test
    public void putObjectRequest_withChecksumCrc32ValueSet_doesNotAddCrc32() {
        PutObjectRequest request = PutObjectRequest.builder()
                                                   .checksumCRC32("crc32Value")
                                                   .build();
        Algorithm algorithm = null;
        ExecutionAttributes executionAttributes = createExecutionAttributes(algorithm);

        Context.ModifyRequest modifyRequestContext = () -> request;
        SdkRequest modifiedRequest = interceptor.modifyRequest(modifyRequestContext, executionAttributes);
        assertThat(executionAttributes.getAttribute(SELECTED_AUTH_SCHEME).authSchemeOption().signerProperty(CHECKSUM_ALGORITHM)).isNull();

        Context.ModifyHttpRequest modifyHttpRequestContext = modifyHttpRequestContext(modifiedRequest);
        SdkHttpRequest sdkHttpRequest = interceptor.modifyHttpRequest(modifyHttpRequestContext, executionAttributes);
        assertThat(sdkHttpRequest.firstMatchingHeader("x-amz-sdk-checksum-algorithm")).isEmpty();
    }

    @Test
    public void putObjectRequest_withChecksumCrc32CValueSet_doesNotAddCrc32() {
        PutObjectRequest request = PutObjectRequest.builder()
                                                   .checksumCRC32C("crc32CValue")
                                                   .build();
        Algorithm algorithm = null;
        ExecutionAttributes executionAttributes = createExecutionAttributes(algorithm);

        Context.ModifyRequest modifyRequestContext = () -> request;
        SdkRequest modifiedRequest = interceptor.modifyRequest(modifyRequestContext, executionAttributes);
        assertThat(executionAttributes.getAttribute(SELECTED_AUTH_SCHEME).authSchemeOption().signerProperty(CHECKSUM_ALGORITHM)).isNull();

        Context.ModifyHttpRequest modifyHttpRequestContext = modifyHttpRequestContext(modifiedRequest);
        SdkHttpRequest sdkHttpRequest = interceptor.modifyHttpRequest(modifyHttpRequestContext, executionAttributes);
        assertThat(sdkHttpRequest.firstMatchingHeader("x-amz-sdk-checksum-algorithm")).isEmpty();
    }

    private ExecutionAttributes createExecutionAttributes(Algorithm algorithm) {
        Endpoint endpoint = Endpoint.builder()
                                    .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                    .build();

        S3ExpressSessionCredentials credentials = S3ExpressSessionCredentials.create("akid", "sak", "token");
        AuthSchemeOption authSchemeOption = AuthSchemeOption.builder().schemeId("aws.auth#sigv4-s3express").build();
        SelectedAuthScheme<S3ExpressSessionCredentials> authScheme =
            new SelectedAuthScheme<>(CompletableFuture.completedFuture(credentials), DefaultS3ExpressHttpSigner.create(), authSchemeOption);

        ChecksumSpecs checksumSpecs = ChecksumSpecs.builder()
                                                   .algorithm(algorithm)
                                                   .build();

        return ExecutionAttributes.builder()
                                  .put(RESOLVED_ENDPOINT, endpoint)
                                  .put(SELECTED_AUTH_SCHEME, authScheme)
                                  .put(RESOLVED_CHECKSUM_SPECS, checksumSpecs)
                                  .build();
    }
}
