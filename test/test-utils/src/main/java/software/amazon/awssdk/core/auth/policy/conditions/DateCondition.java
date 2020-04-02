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

package software.amazon.awssdk.core.auth.policy.conditions;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

import java.util.Collections;
import java.util.Date;
import software.amazon.awssdk.core.auth.policy.Condition;

/**
 * AWS access control policy condition that allows an access control statement
 * to be conditionally applied based on the comparison of the current time at
 * which a request is received, and a specific date.
 */
public class DateCondition extends Condition {

    /**
     * Constructs a new access policy condition that compares the current time
     * (on the AWS servers) to the specified date.
     *
     * @param type
     *            The type of comparison to perform. For example,
     *            {@link DateComparisonType#DateLessThan} will cause this policy
     *            condition to evaluate to true if the current date is less than
     *            the date specified in the second argument.
     * @param date
     *            The date to compare against.
     */
    public DateCondition(DateComparisonType type, Date date) {
        super.type = type.toString();
        super.conditionKey = ConditionFactory.CURRENT_TIME_CONDITION_KEY;
        super.values = Collections.singletonList(ISO_INSTANT.format(date.toInstant()));
    }

    ;

    /**
     * Enumeration of the supported ways a date comparison can be evaluated.
     */
    public enum DateComparisonType {
        DateEquals,
        DateGreaterThan,
        DateGreaterThanEquals,
        DateLessThan,
        DateLessThanEquals,
        DateNotEquals;
    }

}
