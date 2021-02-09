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
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.authcrt.signer.AwsCrtS3V4aSigner;
import software.amazon.awssdk.authcrt.signer.AwsCrtV4aSigner;
import software.amazon.awssdk.authcrt.signer.SignerTestUtils;
import software.amazon.awssdk.authcrt.signer.SigningTestCase;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.internal.util.RegionScope;

/**
 * Functional tests for the S3 specific Sigv4a signer. These tests call the CRT native signer code.
 */
@RunWith(MockitoJUnitRunner.class)
public class SigningScopeTest {

    @Test
    public void signing_withSigningRegionAndRegionScope_usesRegionScope() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();
        SdkHttpFullRequest signedRequest = signRequestWithScope(testCase, RegionScope.GLOBAL.id());

        String regionHeader = signedRequest.firstMatchingHeader("X-Amz-Region-Set").get();
        assertThat(regionHeader).isEqualTo(RegionScope.GLOBAL.id());
    }

    @Test
    public void signing_withSigningRegionOnly_usesSigningRegion() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);
        SdkHttpFullRequest request = testCase.requestBuilder.build();
        SdkHttpFullRequest signedRequest = AwsCrtV4aSigner.create().sign(request, executionAttributes);

        String regionHeader = signedRequest.firstMatchingHeader("X-Amz-Region-Set").get();
        assertThat(regionHeader).isEqualTo(Region.AWS_GLOBAL.id());
    }

    @Test
    public void signing_chunkedEncoding_regionalScope_present_overrides() {
        SigningTestCase testCase = SignerTestUtils.createBasicChunkedSigningTestCase();
        SdkHttpFullRequest signedRequest = signRequestWithScope(testCase, RegionScope.GLOBAL.id());

        String regionHeader = signedRequest.firstMatchingHeader("X-Amz-Region-Set").get();
        assertThat(regionHeader).isEqualTo(RegionScope.GLOBAL.id());
    }

    @Test
    public void testS3Presigning() {
        SigningTestCase testCase = SignerTestUtils.createBasicQuerySigningTestCase();

        SdkHttpFullRequest signedRequest = presignRequestWithScope(testCase, RegionScope.GLOBAL.id());

        List<String> regionHeader = signedRequest.rawQueryParameters().get("X-Amz-Region-Set");
        assertThat(regionHeader.get(0)).isEqualTo(RegionScope.GLOBAL.id());
    }

    private SdkHttpFullRequest signRequestWithScope(SigningTestCase testCase, String regionScope) {
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE,
                                         RegionScope.builder().regionScope(regionScope).build());

        SdkHttpFullRequest request = testCase.requestBuilder.build();
        return AwsCrtS3V4aSigner.create().sign(request, executionAttributes);
    }

    private SdkHttpFullRequest presignRequestWithScope(SigningTestCase testCase, String regionScope) {
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE,
                                         RegionScope.builder().regionScope(regionScope).build());
        SdkHttpFullRequest request = testCase.requestBuilder.build();
        return AwsCrtS3V4aSigner.create().presign(request, executionAttributes);
    }

}
