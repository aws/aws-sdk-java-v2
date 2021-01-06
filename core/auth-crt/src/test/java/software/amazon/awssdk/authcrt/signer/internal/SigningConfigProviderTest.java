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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.internal.SignerConstant;
import software.amazon.awssdk.authcrt.signer.SignerTestUtils;
import software.amazon.awssdk.authcrt.signer.SigningTestCase;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;

public class SigningConfigProviderTest {

    SigningConfigProvider configProvider;

    @Before
    public void setup() {
        configProvider = new SigningConfigProvider();
    }

    @Test
    public void testBasicHeaderSigningConfiguration() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);

        AwsSigningConfig signingConfig = configProvider.createCrtSigningConfig(executionAttributes);

        assertTrue(signingConfig.getAlgorithm() == AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC);
        assertTrue(signingConfig.getSignatureType() == AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_HEADERS);
        assertTrue(signingConfig.getRegion().equals(testCase.regionSet));
        assertTrue(signingConfig.getService().equals(testCase.signingName));
        assertTrue(signingConfig.getShouldNormalizeUriPath());
        assertTrue(signingConfig.getUseDoubleUriEncode());
    }

    @Test
    public void testBasicQuerySigningConfiguration() {
        SigningTestCase testCase = SignerTestUtils.createBasicQuerySigningTestCase();
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);

        AwsSigningConfig signingConfig = configProvider.createCrtPresigningConfig(executionAttributes);

        assertTrue(signingConfig.getAlgorithm() == AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC);
        assertTrue(signingConfig.getSignatureType() == AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS);
        assertTrue(signingConfig.getRegion().equals(testCase.regionSet));
        assertTrue(signingConfig.getService().equals(testCase.signingName));
        assertTrue(signingConfig.getShouldNormalizeUriPath());
        assertTrue(signingConfig.getUseDoubleUriEncode());
        assertTrue(signingConfig.getExpirationInSeconds() == SignerConstant.PRESIGN_URL_MAX_EXPIRATION_SECONDS);
    }

    @Test
    public void testQuerySigningExpirationConfiguration() {
        SigningTestCase testCase = SignerTestUtils.createBasicQuerySigningTestCase();
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);

        Instant expirationTime = testCase.signingTime.plus(900, ChronoUnit.SECONDS);
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.PRESIGNER_EXPIRATION, expirationTime);

        AwsSigningConfig signingConfig = configProvider.createCrtPresigningConfig(executionAttributes);

        assertTrue(signingConfig.getExpirationInSeconds() == 900);
    }

    @Test
    public void testS3SigningConfiguration() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();

        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);
        executionAttributes.putAttribute(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING, true);
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE, false);

        AwsSigningConfig signingConfig = configProvider.createS3CrtSigningConfig(executionAttributes);

        /* first check basic configuration */
        assertTrue(signingConfig.getAlgorithm() == AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC);
        assertTrue(signingConfig.getSignatureType() == AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_HEADERS);
        assertTrue(signingConfig.getRegion().equals(testCase.regionSet));
        assertTrue(signingConfig.getService().equals(testCase.signingName));
        assertTrue(signingConfig.getShouldNormalizeUriPath());
        assertFalse(signingConfig.getUseDoubleUriEncode());

        /* body signing should be enabled */
        assertTrue(signingConfig.getSignedBodyHeader() == AwsSigningConfig.AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256);
        assertTrue(signingConfig.getSignedBodyValue() == null);

        /* try again with body signing explicitly disabled
         * we should still see the header but it should be using UNSIGNED_PAYLOAD for the value */
        executionAttributes.putAttribute(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING, false);
        signingConfig = configProvider.createS3CrtSigningConfig(executionAttributes);

        assertTrue(signingConfig.getSignedBodyHeader() == AwsSigningConfig.AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256);
    }

    @Test
    public void testS3PresigningConfiguration() {
        SigningTestCase testCase = SignerTestUtils.createBasicQuerySigningTestCase();

        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);
        executionAttributes.putAttribute(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING, true);
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE, false);

        AwsSigningConfig signingConfig = configProvider.createS3CrtPresigningConfig(executionAttributes);

        /* first check basic configuration */
        assertTrue(signingConfig.getAlgorithm() == AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC);
        assertTrue(signingConfig.getSignatureType() == AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS);
        assertTrue(signingConfig.getRegion().equals(testCase.regionSet));
        assertTrue(signingConfig.getService().equals(testCase.signingName));
        assertTrue(signingConfig.getShouldNormalizeUriPath());
        assertFalse(signingConfig.getUseDoubleUriEncode());

        /* body signing should be disabled and the body should be UNSIGNED_PAYLOAD */
        assertTrue(signingConfig.getSignedBodyHeader() == AwsSigningConfig.AwsSignedBodyHeaderType.NONE);
        assertTrue(signingConfig.getSignedBodyValue() == AwsSigningConfig.AwsSignedBodyValue.UNSIGNED_PAYLOAD);
    }

    @Test
    public void testChunkedSigningConfiguration() {
        SigningTestCase testCase = SignerTestUtils.createBasicChunkedSigningTestCase();

        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);
        executionAttributes.putAttribute(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING, true);
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE, false);

        AwsSigningConfig signingConfig = configProvider.createChunkedSigningConfig(executionAttributes);

        assertThat(signingConfig.getCredentials()).isNotNull();
        assertThat(signingConfig.getService()).isEqualTo(executionAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME));
        assertThat(signingConfig.getRegion()).isEqualTo(executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION).id());

        assertThat(signingConfig.getTime()).isEqualTo(testCase.signingTime.toEpochMilli());
        assertThat(signingConfig.getAlgorithm()).isEqualTo(AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC);

        assertThat(signingConfig.getSignatureType()).isEqualTo(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_CHUNK);
        assertThat(signingConfig.getSignedBodyHeader()).isEqualTo(AwsSigningConfig.AwsSignedBodyHeaderType.NONE);
    }
}