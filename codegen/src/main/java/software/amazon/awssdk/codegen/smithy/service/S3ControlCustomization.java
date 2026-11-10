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

package software.amazon.awssdk.codegen.smithy.service;

import java.util.Collections;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.AuthType;

/**
 * S3 Control signs with the s3v4 variant of SigV4, and its endpoint rules already put the account ID in the host.
 * Replaces C2J's {@code signatureVersion} {@code s3v4} and {@code S3ControlRemoveAccountIdHostPrefixProcessor}.
 */
@SdkInternalApi
final class S3ControlCustomization implements SmithyServiceCustomization {
    private static final String ACCOUNT_ID_PREFIX = "{AccountId}.";

    @Override
    public Set<String> sdkIds() {
        return Collections.singleton("S3 Control");
    }

    @Override
    public void postprocess(IntermediateModel model) {
        model.getMetadata().setAuthType(AuthType.S3V4);
        model.getOperations().values().stream()
             .filter(op -> op.getEndpointTrait() != null)
             .filter(op -> ACCOUNT_ID_PREFIX.equals(op.getEndpointTrait().getHostPrefix()))
             .forEach(op -> op.getEndpointTrait().setHostPrefix(null));
    }
}
