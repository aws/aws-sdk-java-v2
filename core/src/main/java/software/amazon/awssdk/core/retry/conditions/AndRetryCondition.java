/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.retry.conditions;

import static software.amazon.awssdk.core.util.ValidationUtils.assertNotEmpty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.retry.RetryPolicyContext;

/**
 * Composite {@link RetryCondition} that evaluates to true when all contained retry conditions evaluate to true.
 */
@SdkPublicApi
public class AndRetryCondition implements RetryCondition {

    private List<RetryCondition> conditions = new ArrayList<RetryCondition>();

    public AndRetryCondition(RetryCondition... conditions) {
        Collections.addAll(this.conditions, assertNotEmpty(conditions, "conditions"));
    }

    /**
     * @return True if all conditions are true, false otherwise.
     */
    @Override
    public boolean shouldRetry(RetryPolicyContext context) {
        for (RetryCondition retryCondition : conditions) {
            if (!retryCondition.shouldRetry(context)) {
                return false;
            }
        }
        return true;
    }
}
