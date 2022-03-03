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

package software.amazon.awssdk.services.iam.internal.extensions;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.auth.policy.SdkIamPolicy;
import software.amazon.awssdk.services.iam.extensions.IamClientSdkExtension;
import software.amazon.awssdk.services.iam.model.CreatePolicyRequest;
import software.amazon.awssdk.services.iam.model.CreatePolicyResponse;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public class DefaultIamClientSdkExtension implements IamClientSdkExtension {

    private final IamClient iam;

    public DefaultIamClientSdkExtension(IamClient iam) {
        this.iam = Validate.notNull(iam, "iam");
    }

    @Override
    public CreatePolicyResponse createPolicy(SdkIamPolicy policy, CreatePolicyRequest request) {
        Validate.notNull(policy, "policy");
        CreatePolicyRequest.Builder changedRequest = request.toBuilder().policyDocument(policy.toJsonDocument());
        return iam.createPolicy(changedRequest.build());
    }
}
