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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.internal.chunkedencoding.AwsChunkedEncodingInputStream;
import software.amazon.awssdk.authcrt.signer.SignerTestUtils;
import software.amazon.awssdk.authcrt.signer.SigningTestCase;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Unit tests for the {@link DefaultAwsCrtS3V4aSigner}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultAwsCrtS3V4aSignerTest {

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

    private void verifyUnsignedPayload(SdkHttpFullRequest signedRequest) {
        assertThat(signedRequest.firstMatchingHeader("Authorization")).isPresent();
        assertThat(signedRequest.firstMatchingHeader("x-amz-decoded-content-length")).isNotPresent();
        assertThat(signedRequest.contentStreamProvider()).isPresent();
        assertThat(signedRequest.contentStreamProvider().get().newStream()).isNotInstanceOf(AwsChunkedEncodingInputStream.class);

        Mockito.verify(signerAdapter).signRequest(any(), configCaptor.capture());
        AwsSigningConfig usedConfig = configCaptor.getValue();
        assertThat(usedConfig.getSignedBodyValue()).isEqualTo(AwsSigningConfig.AwsSignedBodyValue.UNSIGNED_PAYLOAD);
    }

    private void verifySignedPayload(SdkHttpFullRequest signedRequest) {
        assertThat(signedRequest.firstMatchingHeader("Authorization")).isPresent();
        assertThat(signedRequest.firstMatchingHeader("Content-Length")).isPresent();
        assertThat(signedRequest.firstMatchingHeader("x-amz-decoded-content-length")).isPresent();
        assertThat(signedRequest.contentStreamProvider()).isPresent();
        assertThat(signedRequest.contentStreamProvider().get().newStream()).isInstanceOf(AwsChunkedEncodingInputStream.class);

        Mockito.verify(signerAdapter).sign(any(), configCaptor.capture());
        AwsSigningConfig usedConfig = configCaptor.getValue();
        assertThat(usedConfig.getSignedBodyValue()).isEqualTo(AwsSigningConfig.AwsSignedBodyValue.STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD);
    }

}
