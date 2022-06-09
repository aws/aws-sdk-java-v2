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

package software.amazon.awssdk.awscore.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.http.NoopTestAwsRequest;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.Pair;

class SignerOverrideUtilsTest {

    @Test
    @DisplayName("If signer is already overridden, assert that it is not modified")
    void overrideSignerIfNotOverridden() {
        Pair<SdkRequest, ExecutionAttributes> stubs = stubInitialSigner(new FooSigner());
        SdkRequest request = stubs.left();
        ExecutionAttributes executionAttributes = stubs.right();

        SdkRequest result = SignerOverrideUtils.overrideSignerIfNotOverridden(request, executionAttributes, BarSigner::new);

        assertThat(result.overrideConfiguration()).isPresent();
        assertThat(result.overrideConfiguration().get().signer().get()).isInstanceOf(FooSigner.class);
    }

    @Test
    @DisplayName("If signer is not already overridden, assert that it is overridden with the new signer")
    void overrideSignerIfNotOverridden2() {
        Pair<SdkRequest, ExecutionAttributes> stubs = stubInitialSigner(null);
        SdkRequest request = stubs.left();
        ExecutionAttributes executionAttributes = stubs.right();

        SdkRequest result = SignerOverrideUtils.overrideSignerIfNotOverridden(request, executionAttributes, BarSigner::new);

        assertThat(result.overrideConfiguration()).isPresent();
        assertThat(result.overrideConfiguration().get().signer().get()).isInstanceOf(BarSigner.class);
    }

    @Test
    @DisplayName("If signer will be overridden, assert that the other existing override configuration properties are preserved")
    public void overrideSignerOriginalConfigPreserved() {
        AwsRequestOverrideConfiguration originalOverride =
            AwsRequestOverrideConfiguration.builder()
                                           .putHeader("Header1", "HeaderValue1")
                                           .putRawQueryParameter("QueryParam1", "QueryValue1")
                                           .addApiName(ApiName.builder().name("foo").version("bar").build())
                                           .build();

        Pair<SdkRequest, ExecutionAttributes> stubs = stubInitialSigner(null);
        SdkRequest request = ((AwsRequest) stubs.left()).toBuilder()
                                                        .overrideConfiguration(originalOverride)
                                                        .build();
        ExecutionAttributes executionAttributes = stubs.right();

        BarSigner overrideSigner = new BarSigner();
        SdkRequest result = SignerOverrideUtils.overrideSignerIfNotOverridden(request, executionAttributes, () -> overrideSigner);
        AwsRequestOverrideConfiguration originalOverrideWithNewSigner = originalOverride.toBuilder()
                                                                                        .signer(overrideSigner)
                                                                                        .build();

        assertThat(result.overrideConfiguration().get()).isEqualTo(originalOverrideWithNewSigner);
    }

    private static Pair<SdkRequest, ExecutionAttributes> stubInitialSigner(Signer signer) {
        AwsRequest request;
        if (signer != null) {
            AwsRequestOverrideConfiguration config = AwsRequestOverrideConfiguration.builder()
                                                                                    .signer(signer)
                                                                                    .build();
            request = NoopTestAwsRequest.builder()
                                        .overrideConfiguration(config)
                                        .build();
        } else {
            request = NoopTestAwsRequest.builder().build();
        }
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        if (signer != null) {
            executionAttributes.putAttribute(SdkExecutionAttribute.SIGNER_OVERRIDDEN, true);
        }
        return Pair.of(request, executionAttributes);
    }

    private static class FooSigner implements Signer {
        @Override
        public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
            throw new UnsupportedOperationException();
        }
    }

    private static class BarSigner implements Signer {
        @Override
        public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
            throw new UnsupportedOperationException();
        }
    }
}