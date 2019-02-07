/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.utils.ToString;

/**
 * Composite retry condition that evaluates to true if any containing condition evaluates to true.
 */
@SdkPublicApi
public final class OrRetryCondition implements RetryCondition {

    private Set<RetryCondition> conditions = new HashSet<>();

    private OrRetryCondition(RetryCondition... conditions) {
        Collections.addAll(this.conditions, conditions);
    }

    /**
     * @return True if any condition returns true. False otherwise.
     */
    @Override
    public boolean shouldRetry(RetryPolicyContext context) {
        return conditions.stream().anyMatch(r -> r.shouldRetry(context));
    }

    public static OrRetryCondition create(RetryCondition... conditions) {
        return new OrRetryCondition(conditions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OrRetryCondition that = (OrRetryCondition) o;

        return conditions.equals(that.conditions);
    }

    @Override
    public int hashCode() {
        return conditions.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder("OrRetryCondition")
                       .add("conditions", conditions)
                       .build();
    }
}
