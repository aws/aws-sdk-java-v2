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

import java.net.URI;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.internal.chunkedencoding.AwsChunkedEncodingInputStream;
import software.amazon.awssdk.authcrt.signer.internal.DefaultAwsCrtV4aSigner;
import software.amazon.awssdk.authcrt.signer.internal.SigningConfigProvider;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.internal.util.RegionScope;

/**
 * Functional tests for the S3 specific Sigv4a signer. These tests call the CRT native signer code.
 */
@RunWith(MockitoJUnitRunner.class)
public class AwsCrtS3V4aSignerTest {

    private SigningConfigProvider configProvider;
    private AwsCrtS3V4aSigner s3V4aSigner;

    @Before
    public void setup() {
        configProvider = new SigningConfigProvider();
        s3V4aSigner = AwsCrtS3V4aSigner.create();
    }

    @Test
    public void testS3Signing() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);

        SdkHttpFullRequest request = testCase.requestBuilder.build();
        SdkHttpFullRequest signedRequest = s3V4aSigner.sign(request, executionAttributes);

        assertThat(signedRequest.firstMatchingHeader("Authorization")).isPresent();
    }

    @Test
    public void testS3ChunkedSigning() {
        SigningTestCase testCase = SignerTestUtils.createBasicChunkedSigningTestCase();
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);

        SdkHttpFullRequest.Builder requestBuilder = testCase.requestBuilder;
        requestBuilder.uri(URI.create("http://demo.us-east-1.amazonaws.com"));
        SdkHttpFullRequest request = requestBuilder.build();
        SdkHttpFullRequest signedRequest = s3V4aSigner.sign(request, executionAttributes);

        assertThat(signedRequest.firstMatchingHeader("Authorization")).isPresent();
        assertThat(signedRequest.contentStreamProvider()).isPresent();
        assertThat(signedRequest.contentStreamProvider().get().newStream()).isInstanceOf(AwsChunkedEncodingInputStream.class);
    }

    @Test
    public void testS3Presigning() {
        SigningTestCase testCase = SignerTestUtils.createBasicQuerySigningTestCase();
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);
        SdkHttpFullRequest request = testCase.requestBuilder.build();

        SdkHttpFullRequest signed = s3V4aSigner.presign(request, executionAttributes);

        List<String> regionHeader = signed.rawQueryParameters().get("X-Amz-Region-Set");
        assertThat(regionHeader.get(0)).isEqualTo(Region.AWS_GLOBAL.id());
    }

}
