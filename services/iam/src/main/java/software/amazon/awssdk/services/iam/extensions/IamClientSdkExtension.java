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

package software.amazon.awssdk.services.iam.extensions;

import software.amazon.awssdk.annotations.SdkExtensionMethod;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.auth.policy.SdkIamPolicy;
import software.amazon.awssdk.services.iam.internal.extensions.DefaultIamClientSdkExtension;
import software.amazon.awssdk.services.iam.model.CreatePolicyRequest;
import software.amazon.awssdk.services.iam.model.CreatePolicyResponse;

/**
 * Extension methods for the {@link IamClient} interface.
 */
@SdkPublicApi
public interface IamClientSdkExtension {

    /**
     * Create an IAM Policy.
     */
    @SdkExtensionMethod
    default CreatePolicyResponse createPolicy(SdkIamPolicy policy, CreatePolicyRequest request) {
        return new DefaultIamClientSdkExtension((IamClient) this).createPolicy(policy, request);
    }

}
