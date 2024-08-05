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

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.policybuilder.iam.internal.DefaultIamCondition;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The {@code Condition} element of a {@link IamStatement}, specifying the conditions in which the statement is in effect.
 *
 * @see
 * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">Condition user guide</a>
 */
@SdkPublicApi
@ThreadSafe
public interface IamCondition extends ToCopyableBuilder<IamCondition.Builder, IamCondition> {
    /**
     * Create an {@link IamCondition} of the supplied operator, key and value (see
     * {@link Builder#operator(IamConditionOperator)}}, {@link Builder#key(IamConditionKey)} and {@link Builder#value(String)}).
     * <p>
     * All of operator, key and value are required. This is equivalent to {@code IamCondition.builder().operator(operator)
     * .key(key).value(value).build()}.
     */
    static IamCondition create(IamConditionOperator operator, IamConditionKey key, String value) {
        return builder().operator(operator).key(key).value(value).build();
    }

    /**
     * Create an {@link IamCondition} of the supplied operator, key and value (see
     * {@link Builder#operator(IamConditionOperator)}}, {@link Builder#key(String)} and {@link Builder#value(String)}).
     * <p>
     * All of operator, key and value are required. This is equivalent to {@code IamCondition.builder().operator(operator)
     * .key(key).value(value).build()}.
     */
    static IamCondition create(IamConditionOperator operator, String key, String value) {
        return builder().operator(operator).key(key).value(value).build();
    }

    /**
     * Create an {@link IamCondition} of the supplied operator, key and value (see
     * {@link Builder#operator(String)}}, {@link Builder#key(String)} and {@link Builder#value(String)}).
     * <p>
     * All of operator, key and value are required. This is equivalent to {@code IamCondition.builder().operator(operator)
     * .key(key).value(value).build()}.
     */
    static IamCondition create(String operator, String key, String value) {
        return builder().operator(operator).key(key).value(value).build();
    }

    /**
     * Create multiple {@link IamCondition}s with the same {@link IamConditionOperator} and {@link IamConditionKey}, but
     * different values (see {@link Builder#operator(IamConditionOperator)}}, {@link Builder#key(IamConditionKey)} and
     * {@link Builder#value(String)}).
     * <p>
     * Operator and key are required, and the values in the value list must not be null. This is equivalent to calling
     * {@link #create(IamConditionOperator, IamConditionKey, String)} multiple times and collecting the results into a list.
     */
    static List<IamCondition> createAll(IamConditionOperator operator, IamConditionKey key, Collection<String> values) {
        if (values == null) {
            return emptyList();
        }
        return values.stream().map(value -> create(operator, key, value)).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Create multiple {@link IamCondition}s with the same {@link IamConditionOperator} and {@link IamConditionKey}, but
     * different values (see {@link Builder#operator(IamConditionOperator)}}, {@link Builder#key(String)} and
     * {@link Builder#value(String)}).
     * <p>
     * Operator and key are required, and the values in the value list must not be null. This is equivalent to calling
     * {@link #create(IamConditionOperator, String, String)} multiple times and collecting the results into a list.
     */
    static List<IamCondition> createAll(IamConditionOperator operator, String key, Collection<String> values) {
        if (values == null) {
            return emptyList();
        }
        return values.stream().map(value -> create(operator, key, value)).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Create multiple {@link IamCondition}s with the same {@link IamConditionOperator} and {@link IamConditionKey}, but
     * different values (see {@link Builder#operator(String)}}, {@link Builder#key(String)} and {@link Builder#value(String)}).
     * <p>
     * Operator and key are required, and the values in the value list must not be null. This is equivalent to calling
     * {@link #create(String, String, String)} multiple times and collecting the results into a list.
     */
    static List<IamCondition> createAll(String operator, String key, Collection<String> values) {
        if (values == null) {
            return emptyList();
        }

        return values.stream().map(value -> create(operator, key, value)).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Create a {@link Builder} for an {@code IamCondition}.
     */
    static Builder builder() {
        return DefaultIamCondition.builder();
    }


    /**
     * Retrieve the value set by {@link Builder#operator(IamConditionOperator)}.
     */
    IamConditionOperator operator();

    /**
     * Retrieve the value set by {@link Builder#key(IamConditionKey)}.
     */
    IamConditionKey key();

    /**
     * Retrieve the value set by {@link Builder#value(String)}.
     */
    String value();

    /**
     * @see #builder()
     */
    interface Builder extends CopyableBuilder<Builder, IamCondition> {
        /**
         * Set the {@link IamConditionOperator} of this condition.
         * <p>
         * This value is required.
         *
         * @see IamConditionOperator
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">Condition
         * user guide</a>
         */
        Builder operator(IamConditionOperator operator);

        /**
         * Set the {@link IamConditionOperator} of this condition.
         * <p>
         * This is the same as {@link #operator(IamConditionOperator)}, except you do not need to call
         * {@code IamConditionOperator.create()}. This value is required.
         *
         * @see IamConditionOperator
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">Condition
         * user guide</a>
         */
        Builder operator(String operator);

        /**
         * Set the {@link IamConditionKey} of this condition.
         * <p>
         * This value is required.
         *
         * @see IamConditionKey
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">Condition
         * user guide</a>
         */
        Builder key(IamConditionKey key);

        /**
         * Set the {@link IamConditionKey} of this condition.
         * <p>
         * This is the same as {@link #key(IamConditionKey)}, except you do not need to call
         * {@code IamConditionKey.create()}. This value is required.
         *
         * @see IamConditionKey
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">Condition
         * user guide</a>
         */
        Builder key(String key);

        /**
         * Set the "right hand side" value of this condition.
         *
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">Condition
         * user guide</a>
         */
        Builder value(String value);
    }
}
