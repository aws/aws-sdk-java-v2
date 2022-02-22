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

package software.amazon.awssdk.authcrt.signer.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute.ENABLE_CHUNKED_ENCODING;
import static software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.internal.chunkedencoding.AwsSignedChunkedEncodingInputStream;
import software.amazon.awssdk.auth.signer.internal.util.SignerMethodResolver;
import software.amazon.awssdk.authcrt.signer.AwsCrtV4aSigner;
import software.amazon.awssdk.authcrt.signer.SignerTestUtils;
import software.amazon.awssdk.authcrt.signer.SigningTestCase;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.core.internal.signer.SigningMethod;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Unit tests for the {@link DefaultAwsCrtS3V4aSigner}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultAwsCrtS3V4aSignerTest {

    private static HttpChecksum HTTP_CRC32_CHECKSUM =
        HttpChecksum.builder().requestAlgorithm("crc32").isRequestStreaming(true).build();


    @Mock
    AwsCrt4aSigningAdapter signerAdapter;

    ArgumentCaptor<AwsSigningConfig> configCaptor = ArgumentCaptor.forClass(AwsSigningConfig.class);

    private SigningConfigProvider configProvider;
    private DefaultAwsCrtS3V4aSigner s3V4aSigner;

    @Before
    public void setup() {
        configProvider = new SigningConfigProvider();
        s3V4aSigner = new DefaultAwsCrtS3V4aSigner(signerAdapter, configProvider);

        SdkHttpFullRequest unsignedPayloadSignedRequest = SignerTestUtils.createSignedHttpRequest("data");

        SdkHttpFullRequest signedPayloadSignedRequest = SignerTestUtils.createSignedPayloadHttpRequest("data");
        String signedPayloadSignature = SignerTestUtils.extractSignatureFromAuthHeader(signedPayloadSignedRequest);

//        when(configProvider.createS3CrtSigningConfig(any())).thenReturn(new AwsSigningConfig());
        when(signerAdapter.sign(any(), any())).thenReturn(new SdkSigningResult(signedPayloadSignature.getBytes(StandardCharsets.UTF_8),
                                                                               signedPayloadSignedRequest));
        when(signerAdapter.signRequest(any(), any())).thenReturn(unsignedPayloadSignedRequest);
    }

    @Test
    public void when_credentials_are_anonymous_return_request() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS,
                                         AnonymousCredentialsProvider.create().resolveCredentials());

        SdkHttpFullRequest request = testCase.requestBuilder.build();
        SdkHttpFullRequest signedRequest = s3V4aSigner.sign(request, executionAttributes);

        assertThat(signedRequest).isEqualTo(request);
    }

    @Test
    public void no_special_configuration_does_not_sign_payload() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);

        SdkHttpFullRequest request = testCase.requestBuilder.build();
        SdkHttpFullRequest signedRequest = s3V4aSigner.sign(request, executionAttributes);

        verifyUnsignedPayload(signedRequest);
    }

    @Test
    public void protocol_http_triggers_payload_signing() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);

        SdkHttpFullRequest.Builder requestBuilder = testCase.requestBuilder;
        requestBuilder.uri(URI.create("http://demo.us-east-1.amazonaws.com"));

        SdkHttpFullRequest signedRequest = s3V4aSigner.sign(requestBuilder.build(), executionAttributes);

        verifySignedPayload(signedRequest);
    }

    @Test
    public void protocol_http_triggers_payload_signing_with_trailer_checksums() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM, HTTP_CRC32_CHECKSUM);
        SdkHttpFullRequest.Builder requestBuilder = testCase.requestBuilder;
        requestBuilder.uri(URI.create("http://demo.us-east-1.amazonaws.com"));

        SdkHttpFullRequest signedRequest = s3V4aSigner.sign(requestBuilder.build(), executionAttributes);
        verifySignedChecksumPayload(signedRequest);
    }


    @Test
    public void unsigned_payload_signing_with_trailer_checksums() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM, HTTP_CRC32_CHECKSUM);
        SdkHttpFullRequest request = testCase.requestBuilder.build();
        SdkHttpFullRequest signedRequest = s3V4aSigner.sign(request, executionAttributes);

        verifyUnsignedPayloadWithTrailerChecksum(signedRequest);
    }


    @Test
    public void payloadSigning_AND_chunkedEnabled_triggers_payload_signing() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);

        executionAttributes.putAttribute(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING, true);
        executionAttributes.putAttribute(S3SignerExecutionAttribute.ENABLE_CHUNKED_ENCODING, true);

        SdkHttpFullRequest signedRequest = s3V4aSigner.sign(testCase.requestBuilder.build(), executionAttributes);

        verifySignedPayload(signedRequest);
    }

    @Test
    public void payloadSigning_and_chunked_disabled_does_not_sign_payload() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);

        executionAttributes.putAttribute(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING, true);
        executionAttributes.putAttribute(S3SignerExecutionAttribute.ENABLE_CHUNKED_ENCODING, false);

        SdkHttpFullRequest signedRequest = s3V4aSigner.sign(testCase.requestBuilder.build(), executionAttributes);

        verifyUnsignedPayload(signedRequest);
    }

    @Test
    public void no_payloadSigning_and_chunkedEnabled_does_not_sign_payload() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);

        executionAttributes.putAttribute(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING, false);
        executionAttributes.putAttribute(S3SignerExecutionAttribute.ENABLE_CHUNKED_ENCODING, true);

        SdkHttpFullRequest signedRequest = s3V4aSigner.sign(testCase.requestBuilder.build(), executionAttributes);

        verifyUnsignedPayload(signedRequest);
    }

    @Test
    public void presigning_returns_signed_params() {
        SdkHttpFullRequest signedPresignedRequest = SignerTestUtils.createPresignedPayloadHttpRequest("data");
        when(signerAdapter.signRequest(any(), any())).thenReturn(signedPresignedRequest);

        SigningTestCase testCase = SignerTestUtils.createBasicQuerySigningTestCase();
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);

        SdkHttpFullRequest request = testCase.requestBuilder.build();
        SdkHttpFullRequest signed = s3V4aSigner.presign(request, executionAttributes);

        assertThat(signed.rawQueryParameters().get("X-Amz-Signature").get(0)).isEqualTo("signature");
    }

    @Test
    public void defaultAwsCrtS3V4aSigner_resolves_to_correct_signing_method(){
        ExecutionAttributes executionAttributes = Mockito.mock(ExecutionAttributes.class);
        AwsCredentials awsCredentials = Mockito.mock(AwsCredentials.class);
        when(executionAttributes.getOptionalAttribute(ENABLE_PAYLOAD_SIGNING)).thenReturn(Optional.of(false));
        when(executionAttributes.getOptionalAttribute(ENABLE_CHUNKED_ENCODING)).thenReturn(Optional.of(true));
        SigningMethod signingMethod = SignerMethodResolver.resolveSigningMethodUsed(DefaultAwsCrtS3V4aSigner.create(),
                                                                                    executionAttributes,
                                                                                    awsCredentials);
        assertThat(signingMethod).isEqualTo(SigningMethod.PROTOCOL_STREAMING_SIGNING_AUTH);
    }

    private void verifyUnsignedPayload(SdkHttpFullRequest signedRequest) {
        verifyUnsignedHeaderRequest(signedRequest);
        Mockito.verify(signerAdapter).signRequest(any(), configCaptor.capture());
        AwsSigningConfig usedConfig = configCaptor.getValue();
        assertThat(usedConfig.getSignedBodyValue()).isEqualTo(AwsSigningConfig.AwsSignedBodyValue.UNSIGNED_PAYLOAD);
    }

    private void verifyUnsignedPayloadWithTrailerChecksum(SdkHttpFullRequest signedRequest) {
        verifyUnsignedHeaderRequest(signedRequest);
        Mockito.verify(signerAdapter).signRequest(any(), configCaptor.capture());
        AwsSigningConfig usedConfig = configCaptor.getValue();
        assertThat(usedConfig.getSignedBodyValue()).isEqualTo("STREAMING-UNSIGNED-PAYLOAD-TRAILER");
    }

    private void verifyUnsignedHeaderRequest(SdkHttpFullRequest signedRequest) {
        assertThat(signedRequest.firstMatchingHeader("Authorization")).isPresent();
        assertThat(signedRequest.firstMatchingHeader("x-amz-decoded-content-length")).isNotPresent();
        assertThat(signedRequest.contentStreamProvider()).isPresent();
        assertThat(signedRequest.contentStreamProvider().get().newStream()).isNotInstanceOf(AwsSignedChunkedEncodingInputStream.class);
    }

    private void verifySignedPayload(SdkHttpFullRequest signedRequest) {
        verifySignedRequestHeaders(signedRequest);
        verifySignedBody(AwsSigningConfig.AwsSignedBodyValue.STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD);
    }

    private void verifySignedChecksumPayload(SdkHttpFullRequest signedRequest) {
        verifySignedRequestHeaders(signedRequest);
        verifySignedBody(AwsSigningConfig.AwsSignedBodyValue.STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD_TRAILER);
    }


    private void verifySignedBody(String awsSignedBodyValue) {
        Mockito.verify(signerAdapter).sign(any(), configCaptor.capture());
        AwsSigningConfig usedConfig = configCaptor.getValue();
        assertThat(usedConfig.getSignedBodyValue()).isEqualTo(awsSignedBodyValue);
    }

    private void verifySignedRequestHeaders(SdkHttpFullRequest signedRequest) {
        assertThat(signedRequest.firstMatchingHeader("Authorization")).isPresent();
        assertThat(signedRequest.firstMatchingHeader("Content-Length")).isPresent();
        assertThat(signedRequest.firstMatchingHeader("x-amz-decoded-content-length")).isPresent();
        assertThat(signedRequest.contentStreamProvider()).isPresent();
        assertThat(signedRequest.contentStreamProvider().get().newStream()).isInstanceOf(AwsSignedChunkedEncodingInputStream.class);
    }

}
