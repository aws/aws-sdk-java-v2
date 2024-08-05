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
import software.amazon.awssdk.policybuilder.iam.internal.DefaultIamConditionOperator;

/**
 * The {@code IamConditionOperator} specifies the operator that should be applied to compare the {@link IamConditionKey} to an
 * expected value in an {@link IamCondition}.
 *
 * @see IamCondition
 * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">Condition
 * user guide</a>
 */
@SdkPublicApi
@ThreadSafe
public interface IamConditionOperator extends IamValue {
    /**
     * A string comparison of the {@link IamCondition#key()} and {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_String">
     * String conditions</a>
     */
    IamConditionOperator STRING_EQUALS = create("StringEquals");

    /**
     * A negated string comparison of the {@link IamCondition#key()} and {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_String">
     * String conditions</a>
     */
    IamConditionOperator STRING_NOT_EQUALS = create("StringNotEquals");

    /**
     * A string comparison, ignoring casing, of the {@link IamCondition#key()} and {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_String">
     * String conditions</a>
     */
    IamConditionOperator STRING_EQUALS_IGNORE_CASE = create("StringEqualsIgnoreCase");

    /**
     * A negated string comparison, ignoring casing, of the {@link IamCondition#key()} and {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_String">
     * String conditions</a>
     */
    IamConditionOperator STRING_NOT_EQUALS_IGNORE_CASE = create("StringNotEqualsIgnoreCase");

    /**
     * A case-sensitive pattern match between the {@link IamCondition#key()} and {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_String">
     * String conditions</a>
     */
    IamConditionOperator STRING_LIKE = create("StringLike");

    /**
     * A negated case-sensitive pattern match between the {@link IamCondition#key()} and {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_String">
     * String conditions</a>
     */
    IamConditionOperator STRING_NOT_LIKE = create("StringNotLike");

    /**
     * A numeric comparison of the {@link IamCondition#key()} and {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_Numeric">
     * Numeric conditions</a>
     */
    IamConditionOperator NUMERIC_EQUALS = create("NumericEquals");

    /**
     * A negated numeric comparison of the {@link IamCondition#key()} and {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_Numeric">
     * Numeric conditions</a>
     */
    IamConditionOperator NUMERIC_NOT_EQUALS = create("NumericNotEquals");

    /**
     * A numeric comparison of whether the {@link IamCondition#key()} is "less than" the {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_Numeric">
     * Numeric conditions</a>
     */
    IamConditionOperator NUMERIC_LESS_THAN = create("NumericLessThan");

    /**
     * A numeric comparison of whether the {@link IamCondition#key()} is "less than or equal to" the {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_Numeric">
     * Numeric conditions</a>
     */
    IamConditionOperator NUMERIC_LESS_THAN_EQUALS = create("NumericLessThanEquals");

    /**
     * A numeric comparison of whether the {@link IamCondition#key()} is "greater than" the {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_Numeric">
     * Numeric conditions</a>
     */
    IamConditionOperator NUMERIC_GREATER_THAN = create("NumericGreaterThan");

    /**
     * A numeric comparison of whether the {@link IamCondition#key()} is "greater than or equal to" the
     * {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_Numeric">
     * Numeric conditions</a>
     */
    IamConditionOperator NUMERIC_GREATER_THAN_EQUALS = create("NumericGreaterThanEquals");

    /**
     * A date comparison of the {@link IamCondition#key()} and {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_Date">
     * Date conditions</a>
     */
    IamConditionOperator DATE_EQUALS = create("DateEquals");

    /**
     * A negated date comparison of the {@link IamCondition#key()} and {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_Date">
     * Date conditions</a>
     */
    IamConditionOperator DATE_NOT_EQUALS = create("DateNotEquals");

    /**
     * A date comparison of whether the {@link IamCondition#key()} "is earlier than" the {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_Date">
     * Date conditions</a>
     */
    IamConditionOperator DATE_LESS_THAN = create("DateLessThan");

    /**
     * A date comparison of whether the {@link IamCondition#key()} "is earlier than or the same date as" the
     * {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_Date">
     * Date conditions</a>
     */
    IamConditionOperator DATE_LESS_THAN_EQUALS = create("DateLessThanEquals");

    /**
     * A date comparison of whether the {@link IamCondition#key()} "is later than" the {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_Date">
     * Date conditions</a>
     */
    IamConditionOperator DATE_GREATER_THAN = create("DateGreaterThan");

    /**
     * A date comparison of whether the {@link IamCondition#key()} "is later than or the same date as" the
     * {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_Date">
     * Date conditions</a>
     */
    IamConditionOperator DATE_GREATER_THAN_EQUALS = create("DateGreaterThanEquals");

    /**
     * A boolean comparison of the {@link IamCondition#key()} and the {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_Boolean">
     * Boolean conditions</a>
     */
    IamConditionOperator BOOL = create("Bool");

    /**
     * A binary comparison of the {@link IamCondition#key()} and the {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_BinaryEquals">
     * Binary conditions</a>
     */
    IamConditionOperator BINARY_EQUALS = create("BinaryEquals");

    /**
     * An IP address comparison of the {@link IamCondition#key()} and the {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_IPAddress">
     * IP Address conditions</a>
     */
    IamConditionOperator IP_ADDRESS = create("IpAddress");

    /**
     * A negated IP address comparison of the {@link IamCondition#key()} and the {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_IPAddress">
     * IP Address conditions</a>
     */
    IamConditionOperator NOT_IP_ADDRESS = create("NotIpAddress");

    /**
     * An Amazon Resource Name (ARN) comparison of the {@link IamCondition#key()} and the {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_ARN">
     * ARN conditions</a>
     */
    IamConditionOperator ARN_EQUALS = create("ArnEquals");

    /**
     * A negated Amazon Resource Name (ARN) comparison of the {@link IamCondition#key()} and the {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_ARN">
     * ARN conditions</a>
     */
    IamConditionOperator ARN_NOT_EQUALS = create("ArnNotEquals");

    /**
     * A pattern match of the Amazon Resource Names (ARNs) in the {@link IamCondition#key()} and the {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_ARN">
     * ARN conditions</a>
     */
    IamConditionOperator ARN_LIKE = create("ArnLike");

    /**
     * A negated pattern match of the Amazon Resource Names (ARNs) in the {@link IamCondition#key()} and the
     * {@link IamCondition#value()}.
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_ARN">
     * ARN conditions</a>
     */
    IamConditionOperator ARN_NOT_LIKE = create("ArnNotLike");

    /**
     * A check to determine whether the {@link IamCondition#key()} is present (use "false" in the {@link IamCondition#value()})
     * or not present (use "true" in the {@link IamCondition#value()}).
     *
     * @see
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_Null">
     * ARN conditions</a>
     */
    IamConditionOperator NULL = create("Null");

    /**
     * Create a new {@link IamConditionOperator} with the provided string added as a prefix.
     * <p>
     * This is useful when adding
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_multi-value-conditions.html">the
     * "ForAllValues:" or "ForAnyValues:" prefixes</a> to an operator.
     */
    IamConditionOperator addPrefix(String prefix);

    /**
     * Create a new {@link IamConditionOperator} with the provided string added as a suffix.
     * <p>
     * This is useful when adding
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html#Conditions_IfExists">
     * the "IfExists" suffix</a> to an operator.
     */
    IamConditionOperator addSuffix(String suffix);

    /**
     * Create a new {@code IamConditionOperator} element with the provided {@link #value()}.
     */
    static IamConditionOperator create(String value) {
        return new DefaultIamConditionOperator(value);
    }
}