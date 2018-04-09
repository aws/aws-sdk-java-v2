/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import software.amazon.awssdk.core.auth.policy.Condition;
import software.amazon.awssdk.core.auth.policy.conditions.ArnCondition.ArnComparisonType;

/**
 * Factory for creating common AWS access control policy conditions. These
 * conditions are common for AWS services and can be expected to work across any
 * service that supports AWS access control policies.
 */
public final class ConditionFactory {

    /**
     * Condition key for the current time.
     * <p>
     * This condition key should only be used with {@link DateCondition}
     * objects.
     */
    public static final String CURRENT_TIME_CONDITION_KEY = "aws:CurrentTime";

    /**
     * Condition key for the source IP from which a request originates.
     * <p>
     * This condition key should only be used with {@link IpAddressCondition}
     * objects.
     */
    public static final String SOURCE_IP_CONDITION_KEY = "aws:SourceIp";

    /**
     * Condition key for the Amazon Resource Name (ARN) of the source specified
     * in a request. The source ARN indicates which resource is affecting the
     * resource listed in your policy. For example, an SNS topic is the source
     * ARN when publishing messages from the topic to an SQS queue.
     * <p>
     * This condition key should only be used with {@link ArnCondition} objects.
     */
    public static final String SOURCE_ARN_CONDITION_KEY = "aws:SourceArn";

    private ConditionFactory() {
    }

    /**
     * Constructs a new access policy condition that compares the Amazon
     * Resource Name (ARN) of the source of an AWS resource that is modifying
     * another AWS resource with the specified pattern.
     * <p>
     * For example, the source ARN could be an Amazon SNS topic ARN that is
     * sending messages to an Amazon SQS queue. In that case, the SNS topic ARN
     * would be compared the ARN pattern specified here.
     * <p>
     * The endpoint pattern may optionally contain the multi-character wildcard
     * (*) or the single-character wildcard (?). Each of the six colon-delimited
     * components of the ARN is checked separately and each can include a
     * wildcard.
     *
     * <pre class="brush: java">
     * Policy policy = new Policy(&quot;MyQueuePolicy&quot;);
     * policy.withStatements(new Statement(&quot;AllowSNSMessages&quot;, Effect.Allow)
     *         .withPrincipals(new Principal(&quot;*&quot;)).withActions(SQSActions.SendMessage)
     *         .withResources(new Resource(myQueueArn))
     *         .withConditions(ConditionFactory.newSourceArnCondition(myTopicArn)));
     * </pre>
     *
     * @param arnPattern
     *            The ARN pattern against which the source ARN will be compared.
     *            Each of the six colon-delimited components of the ARN is
     *            checked separately and each can include a wildcard.
     *
     * @return A new access control policy condition that compares the ARN of
     *         the source specified in an incoming request with the ARN pattern
     *         specified here.
     */
    public static Condition newSourceArnCondition(String arnPattern) {
        return new ArnCondition(ArnComparisonType.ArnLike, SOURCE_ARN_CONDITION_KEY, arnPattern);
    }
}
