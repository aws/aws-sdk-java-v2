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

package software.amazon.awssdk.services.sqs.auth.policy.resources;

import software.amazon.awssdk.auth.policy.Resource;

/**
 * AWS access control policy resource that identifies an Amazon SQS queue.
 * <p>
 * This is an older style of referencing an Amazon SQS queue. You can also use the queue's Amazon
 * Resource Name (ARN), which you can obtain by calling
 * {@link software.amazon.awssdk.services.sqs.SQSClient#getQueueAttributes(
 * software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest)}
 * and requesting the "QueueArn" attribute.
 */
public class SqsQueueResource extends Resource {

    /**
     * Constructs a new SQS queue resource for an access control policy. A policy statement using
     * this resource will allow or deny actions on the specified queue.
     *
     * @param accountId The AWS account ID of the queue owner.
     * @param queueName The name of the Amazon SQS queue.
     */
    public SqsQueueResource(String accountId, String queueName) {
        super("/" + formatAccountId(accountId) + "/" + queueName);
    }

    private static String formatAccountId(String accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }

        return accountId.trim().replaceAll("-", "");
    }
}
