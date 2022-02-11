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

import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.authcrt.signer.AwsCrtV4aSigner;
import software.amazon.awssdk.authcrt.signer.SignerTestUtils;
import software.amazon.awssdk.authcrt.signer.SigningTestCase;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.regions.RegionScope;

public class AwsCrtV4aSignerSigningScopeTest extends BaseSigningScopeTest {
    @Override
    protected SdkHttpFullRequest signRequestWithScope(SigningTestCase testCase, RegionScope defaultRegionScope,
                                                      RegionScope regionScope) {
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);
        if (regionScope != null) {
            executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE, regionScope);
        }
        SdkHttpFullRequest request = testCase.requestBuilder.build();
        return AwsCrtV4aSigner.builder().defaultRegionScope(defaultRegionScope).build().sign(request, executionAttributes);
    }

    @Override
    protected SdkHttpFullRequest presignRequestWithScope(SigningTestCase testCase, RegionScope defaultRegionScope,
                                                         RegionScope regionScope) {
        ExecutionAttributes executionAttributes = SignerTestUtils.buildBasicExecutionAttributes(testCase);
        if (regionScope != null) {
            executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE, regionScope);
        }
        SdkHttpFullRequest request = testCase.requestBuilder.build();
        return AwsCrtV4aSigner.builder().defaultRegionScope(defaultRegionScope).build().presign(request, executionAttributes);
    }
}
