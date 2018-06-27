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

package software.amazon.awssdk.services.sns;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.core.auth.policy.Action;
import software.amazon.awssdk.core.auth.policy.Policy;
import software.amazon.awssdk.core.auth.policy.Principal;
import software.amazon.awssdk.core.auth.policy.Resource;
import software.amazon.awssdk.core.auth.policy.Statement;
import software.amazon.awssdk.core.auth.policy.Statement.Effect;
import software.amazon.awssdk.core.auth.policy.conditions.ConditionFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

/**
 * Set of utility methods for working with Amazon SNS topics.
 */
public class Topics {

    /**
     * Subscribes an existing Amazon SQS queue to an existing Amazon SNS topic.
     * <p>
     * The specified Amazon SNS client will be used to send the subscription
     * request, and the Amazon SQS client will be used to modify the policy on
     * the queue to allow it to receive messages from the SNS topic.
     * <p>
     * The policy applied to the SQS queue is similar to this:
     * <pre>
     * {
     *    "Version" : "2008-10-17",
     *    "Statement" : [{
     *       "Sid" : "topic-subscription-arn:aws:sns:us-west-2:599109622955:myTopic",
     *       "Effect" : "Allow",
     *       "Principal" : {
     *          "AWS":["*"]
     *       },
     *       "Action" : ["sqs:SendMessage"],
     *       "Resource":["arn:aws:sqs:us-west-2:599109622955:myQueue"],
     *       "Condition" : {
     *          "ArnLike":{
     *             "aws:SourceArn":["arn:aws:sns:us-west-2:599109622955:myTopic"]
     *          }
     *       }
     *    }]
     * }
     * </pre>
     * <p>
     * <b>IMPORTANT</b>: If there is already an existing policy set for the
     * specified SQS queue, this operation will overwrite it with a new policy
     * that allows the SNS topic to deliver messages to the queue.
     * <p>
     * <b>IMPORTANT</b>: There might be a small time period immediately after
     * subscribing the SQS queue to the SNS topic and updating the SQS queue's
     * policy, where messages are not able to be delivered to the queue. After a
     * moment, the new queue policy will propagate and the queue will be able to
     * receive messages. This delay only occurs immediately after initially
     * subscribing the queue.
     * <p>
     * <b>IMPORTANT</b>: The specified queue and topic (as well as the SNS and
     * SQS client) should both be located in the same AWS region.
     *
     * @param sns
     *            The Amazon SNS client to use when subscribing the queue to the
     *            topic.
     * @param sqs
     *            The Amazon SQS client to use when applying the policy to allow
     *            subscribing to the topic.
     * @param snsTopicArn
     *            The Amazon Resource Name (ARN) uniquely identifying the Amazon
     *            SNS topic. This value is returned form Amazon SNS when
     *            creating the topic.
     * @param sqsQueueUrl
     *            The URL uniquely identifying the Amazon SQS queue. This value
     *            is returned from Amazon SQS when creating the queue.
     *
     * @return The subscription ARN as returned by Amazon SNS when the queue is
     *         successfully subscribed to the topic.
     *
     * @throws SdkClientException
     *             If any internal errors are encountered inside the client
     *             while attempting to make the request or handle the response.
     *             For example if a network connection is not available.
     * @throws SdkServiceException
     *             If an error response is returned by SnsClient indicating
     *             either a problem with the data in the request, or a server
     *             side issue.
     */
    public static String subscribeQueue(SnsClient sns, SqsClient sqs, String snsTopicArn, String sqsQueueUrl)
            throws SdkClientException, SdkServiceException {
        return Topics.subscribeQueue(sns, sqs, snsTopicArn, sqsQueueUrl, false);
    }

    /**
     * Subscribes an existing Amazon SQS queue to an existing Amazon SNS topic.
     * <p>
     * The specified Amazon SNS client will be used to send the subscription
     * request, and the Amazon SQS client will be used to modify the policy on
     * the queue to allow it to receive messages from the SNS topic.
     * <p>
     * The policy applied to the SQS queue is similar to this:
     * <pre>
     * {
     *    "Version" : "2008-10-17",
     *    "Statement" : [{
     *       "Sid" : "topic-subscription-arn:aws:sns:us-west-2:599109622955:myTopic",
     *       "Effect" : "Allow",
     *       "Principal" : {
     *          "AWS":["*"]
     *       },
     *       "Action" : ["sqs:SendMessage"],
     *       "Resource":["arn:aws:sqs:us-west-2:599109622955:myQueue"],
     *       "Condition" : {
     *          "ArnLike":{
     *             "aws:SourceArn":["arn:aws:sns:us-west-2:599109622955:myTopic"]
     *          }
     *       }
     *    }]
     * }
     * </pre>
     * <p>
     * <b>IMPORTANT</b>: There might be a small time period immediately after
     * subscribing the SQS queue to the SNS topic and updating the SQS queue's
     * policy, where messages are not able to be delivered to the queue. After a
     * moment, the new queue policy will propagate and the queue will be able to
     * receive messages. This delay only occurs immediately after initially
     * subscribing the queue.
     * <p>
     * <b>IMPORTANT</b>: The specified queue and topic (as well as the SNS and
     * SQS client) should both be located in the same AWS region.
     *
     * @param sns
     *            The Amazon SNS client to use when subscribing the queue to the
     *            topic.
     * @param sqs
     *            The Amazon SQS client to use when applying the policy to allow
     *            subscribing to the topic.
     * @param snsTopicArn
     *            The Amazon Resource Name (ARN) uniquely identifying the Amazon
     *            SNS topic. This value is returned form Amazon SNS when
     *            creating the topic.
     * @param sqsQueueUrl
     *            The URL uniquely identifying the Amazon SQS queue. This value
     *            is returned from Amazon SQS when creating the queue.
     * @param extendPolicy
     *            Decides behavior to overwrite the existing policy or extend it.
     *
     * @return The subscription ARN as returned by Amazon SNS when the queue is
     *         successfully subscribed to the topic.
     *
     * @throws SdkClientException
     *             If any internal errors are encountered inside the client
     *             while attempting to make the request or handle the response.
     *             For example if a network connection is not available.
     * @throws SdkServiceException
     *             If an error response is returned by SnsClient indicating
     *             either a problem with the data in the request, or a server
     *             side issue.
     */
    public static String subscribeQueue(SnsClient sns, SqsClient sqs, String snsTopicArn, String sqsQueueUrl,
                                        boolean extendPolicy)
            throws SdkClientException, SdkServiceException {
        List<String> sqsAttrNames = Arrays.asList(QueueAttributeName.QUEUE_ARN.toString(),
                                                  QueueAttributeName.POLICY.toString());
        Map<String, String> sqsAttrs =
                sqs.getQueueAttributes(GetQueueAttributesRequest.builder()
                        .queueUrl(sqsQueueUrl)
                        .attributeNamesWithStrings(sqsAttrNames)
                        .build())
                        .attributesAsStrings();
        String sqsQueueArn = sqsAttrs.get(QueueAttributeName.QUEUE_ARN.toString());

        String policyJson = sqsAttrs.get(QueueAttributeName.POLICY.toString());
        Policy policy = extendPolicy && policyJson != null && policyJson.length() > 0
                        ? Policy.fromJson(policyJson) : new Policy();
        policy.getStatements().add(new Statement(Effect.Allow)
                                           .withId("topic-subscription-" + snsTopicArn)
                                           .withPrincipals(Principal.ALL_USERS)
                                           .withActions(new Action("sqs:SendMessage"))
                                           .withResources(new Resource(sqsQueueArn))
                                           .withConditions(ConditionFactory.newSourceArnCondition(snsTopicArn)));

        Map<String, String> newAttrs = new HashMap<String, String>();
        newAttrs.put(QueueAttributeName.POLICY.toString(), policy.toJson());
        sqs.setQueueAttributes(SetQueueAttributesRequest.builder().queueUrl(sqsQueueUrl).attributesWithStrings(newAttrs).build());

        SubscribeResponse subscribeResult = sns.subscribe(SubscribeRequest.builder()
                .topicArn(snsTopicArn).protocol("sqs")
                .endpoint(sqsQueueArn)
                .build());
        return subscribeResult.subscriptionArn();
    }
}
