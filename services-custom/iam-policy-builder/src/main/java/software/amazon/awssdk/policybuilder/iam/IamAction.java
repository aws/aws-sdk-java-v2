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
import software.amazon.awssdk.policybuilder.iam.internal.DefaultIamAction;

/**
 * The {@code Action} element of a {@link IamStatement}, specifying which service actions the statement applies to.
 *
 * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_action.html">Action user guide</a>
 */
@SdkPublicApi
@ThreadSafe
public interface IamAction extends IamValue {
    /**
     * An {@link IamAction} representing ALL actions. When used on a statement, it means the policy should apply to
     * every action.
     */
    IamAction ALL = create("*");

    /**
     * Create a new {@code IamAction} element with the provided {@link #value()}.
     */
    static IamAction create(String value) {
        return new DefaultIamAction(value);
    }
}
