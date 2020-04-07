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

package software.amazon.awssdk.core.retry.conditions;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Composite {@link RetryCondition} that evaluates to true when all contained retry conditions evaluate to true.
 */
@SdkPublicApi
public final class AndRetryCondition implements RetryCondition {

    private final Set<RetryCondition> conditions = new LinkedHashSet<>();

    private AndRetryCondition(RetryCondition... conditions) {
        Collections.addAll(this.conditions, Validate.notEmpty(conditions, "%s cannot be empty.", "conditions"));
    }

    public static AndRetryCondition create(RetryCondition... conditions) {
        return new AndRetryCondition(conditions);
    }

    /**
     * @return True if all conditions are true, false otherwise.
     */
    @Override
    public boolean shouldRetry(RetryPolicyContext context) {
        return conditions.stream().allMatch(r -> r.shouldRetry(context));
    }

    @Override
    public void requestWillNotBeRetried(RetryPolicyContext context) {
        conditions.forEach(c -> c.requestWillNotBeRetried(context));
    }

    @Override
    public void requestSucceeded(RetryPolicyContext context) {
        conditions.forEach(c -> c.requestSucceeded(context));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AndRetryCondition that = (AndRetryCondition) o;

        return conditions.equals(that.conditions);
    }

    @Override
    public int hashCode() {
        return conditions.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder("AndRetryCondition")
                       .add("conditions", conditions)
                       .build();
    }
}
