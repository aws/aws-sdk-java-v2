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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.CredentialUtils;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.authcrt.signer.AwsCrtV4aSigner;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.regions.RegionScope;

@SdkInternalApi
public final class DefaultAwsCrtV4aSigner implements AwsCrtV4aSigner {

    private final AwsCrt4aSigningAdapter signer;
    private final SigningConfigProvider configProvider;
    private final RegionScope defaultRegionScope;

    private DefaultAwsCrtV4aSigner(BuilderImpl builder) {
        this(new AwsCrt4aSigningAdapter(), new SigningConfigProvider(), builder.defaultRegionScope);
    }

    DefaultAwsCrtV4aSigner(AwsCrt4aSigningAdapter signer, SigningConfigProvider configProvider,
                           RegionScope defaultRegionScope) {
        this.signer = signer;
        this.configProvider = configProvider;
        this.defaultRegionScope = defaultRegionScope;
    }

    public static AwsCrtV4aSigner create() {
        return builder().build();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        if (CredentialUtils.isAnonymous(executionAttributes.getAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS))) {
            return request;
        }
        ExecutionAttributes defaultsApplied = applyDefaults(executionAttributes);
        return signer.signRequest(request, configProvider.createCrtSigningConfig(defaultsApplied));
    }

    @Override
    public SdkHttpFullRequest presign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        ExecutionAttributes defaultsApplied = applyDefaults(executionAttributes);
        return signer.signRequest(request, configProvider.createCrtPresigningConfig(defaultsApplied));
    }

    /**
     * Applies preconfigured defaults for values that are not present in {@code executionAttributes}.
     */
    private ExecutionAttributes applyDefaults(ExecutionAttributes executionAttributes) {
        return applyDefaultRegionScope(executionAttributes);
    }

    private ExecutionAttributes applyDefaultRegionScope(ExecutionAttributes executionAttributes) {
        if (executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE) != null) {
            return executionAttributes;
        }

        if (defaultRegionScope == null) {
            return executionAttributes;
        }

        return executionAttributes.copy()
                                  .putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE, defaultRegionScope);
    }

    private static class BuilderImpl implements Builder {
        private RegionScope defaultRegionScope;

        @Override
        public Builder defaultRegionScope(RegionScope defaultRegionScope) {
            this.defaultRegionScope = defaultRegionScope;
            return this;
        }

        @Override
        public AwsCrtV4aSigner build() {
            return new DefaultAwsCrtV4aSigner(this);
        }
    }
}
