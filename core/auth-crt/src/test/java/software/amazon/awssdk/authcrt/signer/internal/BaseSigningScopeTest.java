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

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.authcrt.signer.SignerTestUtils;
import software.amazon.awssdk.authcrt.signer.SigningTestCase;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionScope;

/**
 * Functional tests for signing scope handling. These tests call the CRT native signer code.
 */
public abstract class BaseSigningScopeTest {
    @Test
    public void signing_withDefaultRegionScopeOnly_usesDefaultRegionScope() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();
        SdkHttpFullRequest signedRequest = signRequestWithScope(testCase, RegionScope.GLOBAL, null);

        String regionHeader = signedRequest.firstMatchingHeader("X-Amz-Region-Set").get();
        assertThat(regionHeader).isEqualTo(RegionScope.GLOBAL.id());
    }

    @Test
    public void presigning_withDefaultRegionScopeOnly_usesDefaultRegionScope() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();
        SdkHttpFullRequest signedRequest = presignRequestWithScope(testCase, RegionScope.GLOBAL, null);

        assertThat(signedRequest.rawQueryParameters().get("X-Amz-Region-Set")).containsExactly(RegionScope.GLOBAL.id());
    }

    @Test
    public void signing_withDefaultScopeAndExplicitScope_usesExplicitScope() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();

        String expectdScope = "us-west-2";
        SdkHttpFullRequest signedRequest = signRequestWithScope(testCase, RegionScope.GLOBAL, RegionScope.create(expectdScope));

        String regionHeader = signedRequest.firstMatchingHeader("X-Amz-Region-Set").get();
        assertThat(regionHeader).isEqualTo(expectdScope);
    }

    @Test
    public void presigning_withDefaultScopeAndExplicitScope_usesExplicitScope() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();

        String expectdScope = "us-west-2";
        SdkHttpFullRequest signedRequest = presignRequestWithScope(testCase, RegionScope.GLOBAL,
                                                                   RegionScope.create(expectdScope));

        assertThat(signedRequest.rawQueryParameters().get("X-Amz-Region-Set")).containsExactly(expectdScope);
    }

    @Test
    public void signing_withSigningRegionAndRegionScope_usesRegionScope() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();
        SdkHttpFullRequest signedRequest = signRequestWithScope(testCase, null, RegionScope.GLOBAL);

        String regionHeader = signedRequest.firstMatchingHeader("X-Amz-Region-Set").get();
        assertThat(regionHeader).isEqualTo(RegionScope.GLOBAL.id());
    }

    @Test
    public void presigning_withSigningRegionAndRegionScope_usesRegionScope() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();
        SdkHttpFullRequest signedRequest = presignRequestWithScope(testCase, null, RegionScope.GLOBAL);

        assertThat(signedRequest.rawQueryParameters().get("X-Amz-Region-Set")).containsExactly(RegionScope.GLOBAL.id());
    }

    @Test
    public void signing_withSigningRegionOnly_usesSigningRegion() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();

        SdkHttpFullRequest signedRequest = signRequestWithScope(testCase, null, null);

        String regionHeader = signedRequest.firstMatchingHeader("X-Amz-Region-Set").get();
        assertThat(regionHeader).isEqualTo(Region.AWS_GLOBAL.id());
    }

    @Test
    public void presigning_withSigningRegionOnly_usesSigningRegion() {
        SigningTestCase testCase = SignerTestUtils.createBasicHeaderSigningTestCase();

        SdkHttpFullRequest signedRequest = presignRequestWithScope(testCase, null, null);

        assertThat(signedRequest.rawQueryParameters().get("X-Amz-Region-Set")).containsExactly(Region.AWS_GLOBAL.id());
    }

    protected abstract SdkHttpFullRequest signRequestWithScope(SigningTestCase testCase,
                                                               RegionScope defaultRegionScope,
                                                               RegionScope regionScope);

    protected abstract SdkHttpFullRequest presignRequestWithScope(SigningTestCase testCase,
                                                                  RegionScope defaultRegionScope,
                                                                  RegionScope regionScope);
}
