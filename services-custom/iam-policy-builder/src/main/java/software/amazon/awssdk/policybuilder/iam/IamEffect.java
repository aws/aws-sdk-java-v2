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
import software.amazon.awssdk.policybuilder.iam.internal.DefaultIamEffect;

/**
 * The {@code Effect} element of a {@link IamStatement}, specifying whether the statement should ALLOW or DENY certain actions.
 *
 * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_effect.html">Effect user guide</a>
 */
@SdkPublicApi
@ThreadSafe
public interface IamEffect extends IamValue {
    /**
     * The {@link IamStatement} to which this effect is attached should ALLOW the actions described in the policy, and DENY
     * everything else.
     */
    IamEffect ALLOW = create("Allow");

    /**
     * The {@link IamStatement} to which this effect is attached should DENY the actions described in the policy. This takes
     * precedence over any other ALLOW statements. See the
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_evaluation-logic.html">policy evaluation
     * logic guide</a> for more information on how to use the DENY effect.
     */
    IamEffect DENY = create("Deny");

    /**
     * Create a new {@code IamEffect} element with the provided {@link #value()}.
     */
    static IamEffect create(String value) {
        return new DefaultIamEffect(value);
    }
}
