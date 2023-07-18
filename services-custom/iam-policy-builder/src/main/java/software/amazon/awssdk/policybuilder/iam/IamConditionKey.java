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

package software.amazon.awssdk.policybuilder.iam;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.policybuilder.iam.internal.DefaultIamConditionKey;

/**
 * The {@code IamConditionKey} specifies the "left hand side" of an {@link IamCondition}.
 *
 * @see IamCondition
 * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">Condition
 * user guide</a>
 */
@SdkPublicApi
@ThreadSafe
public interface IamConditionKey extends IamValue {
    /**
     * Create a new {@code IamConditionKey} element with the provided {@link #value()}.
     */
    static IamConditionKey create(String value) {
        return new DefaultIamConditionKey(value);
    }
}
