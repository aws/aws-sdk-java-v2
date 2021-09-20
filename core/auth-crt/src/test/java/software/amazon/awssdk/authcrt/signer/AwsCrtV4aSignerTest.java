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

package software.amazon.awssdk.authcrt.signer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.authcrt.signer.internal.SigningConfigProvider;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.regions.Region;

/**
 * Functional tests for the Sigv4a signer. These tests call the CRT native signer code.
 */
@RunWith(MockitoJUnitRunner.class)
public class AwsCrtV4aSignerTest {

    private SigningConfigProvider configProvider;
    private AwsCrtV4aSigner v4aSigner;

    @Before
    public void setup() {
        configProvider = new SigningConfigProvider();
        v4aSigner = AwsCrtV4aSigner.create();
    }

    @Test
    public void hostHeaderExcludesStandardHttpPort() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);

        SdkHttpFullRequest request = testCase.requestBuilder.protocol("http")
                                                            .port(80)
                                                            .build();
        SdkHttpFullRequest signed = v4aSigner.sign(request, executionAttributes);

        assertThat(signed.firstMatchingHeader("Host")).hasValue("demo.us-east-1.amazonaws.com");
    }

    @Test
    public void hostHeaderExcludesStandardHttpsPort() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);

        SdkHttpFullRequest request = testCase.requestBuilder.protocol("https")
                                                            .port(443)
                                                            .build();
        SdkHttpFullRequest signed = v4aSigner.sign(request, executionAttributes);

        assertThat(signed.firstMatchingHeader("Host")).hasValue("demo.us-east-1.amazonaws.com");
    }

    @Test
    public void hostHeaderIncludesNonStandardPorts() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);

        SdkHttpFullRequest request = testCase.requestBuilder.protocol("http")
                                                            .port(443)
                                                            .build();
        SdkHttpFullRequest signed = v4aSigner.sign(request, executionAttributes);

        assertThat(signed.firstMatchingHeader("Host")).hasValue("demo.us-east-1.amazonaws.com:443");
    }

    @Test
    public void testHeaderSigning() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);

        SdkHttpFullRequest request = testCase.requestBuilder.build();
        SdkHttpFullRequest signed = v4aSigner.sign(request, executionAttributes);

        String authHeader = signed.firstMatchingHeader("Authorization").get();
        String signatureKey = "Signature=";
        String signatureValue = authHeader.substring(authHeader.indexOf(signatureKey) + signatureKey.length());

        AwsSigningConfig signingConfig = configProvider.createCrtSigningConfig(executionAttributes);

        String regionHeader = signed.firstMatchingHeader("X-Amz-Region-Set").get();
        assertThat(regionHeader).isEqualTo(Region.AWS_GLOBAL.id());
        assertTrue(SignerTestUtils.verifyEcdsaSignature(request, testCase.expectedCanonicalRequest, signingConfig, signatureValue));
    }

    @Test
    public void testQuerySigning() {
        SigningTestCase testCase = SignerTestUtils.createBasicQuerySigningTestCase();
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);

        SdkHttpFullRequest request = testCase.requestBuilder.build();
        SdkHttpFullRequest signed = v4aSigner.presign(request, executionAttributes);

        List<String> signatureValues = signed.rawQueryParameters().get("X-Amz-Signature");
        String signatureValue = signatureValues.get(0);

        List<String> regionHeader = signed.rawQueryParameters().get("X-Amz-Region-Set");
        assertThat(regionHeader.get(0)).isEqualTo(Region.AWS_GLOBAL.id());

        AwsSigningConfig signingConfig = configProvider.createCrtPresigningConfig(executionAttributes);

        assertTrue(SignerTestUtils.verifyEcdsaSignature(request, testCase.expectedCanonicalRequest, signingConfig, signatureValue));
    }

}
