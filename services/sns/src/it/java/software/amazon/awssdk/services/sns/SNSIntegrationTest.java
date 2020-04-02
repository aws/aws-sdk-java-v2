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

package software.amazon.awssdk.services.sns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.sns.model.AddPermissionRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicResponse;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsRequest;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsResponse;
import software.amazon.awssdk.services.sns.model.ListTopicsRequest;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.RemovePermissionRequest;
import software.amazon.awssdk.services.sns.model.SetSubscriptionAttributesRequest;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;
import software.amazon.awssdk.services.sns.model.Subscription;
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

/**
 * Integration tests for Cloudcast operations.
 */
public class SNSIntegrationTest extends IntegrationTestBase {

    private static final String DELIVERY_POLICY =
            "{ " + "  \"healthyRetryPolicy\":" + "    {" +
            "       \"minDelayTarget\": 1," + "       \"maxDelayTarget\": 1," + "       \"numRetries\": 1, " +
            "       \"numMaxDelayRetries\": 0, " + "       \"backoffFunction\": \"linear\"" + "     }" + "}";
    private final SignatureChecker signatureChecker = new SignatureChecker();
    /** The ARN of the topic created by these tests. */
    private String topicArn;
    /** The URL of the SQS queue created to receive notifications. */
    private String queueUrl;
    private String subscriptionArn;

    @Before
    public void setup() {
        topicArn = null;
        queueUrl = null;
        subscriptionArn = null;
    }

    /** Releases all resources used by this test. */
    @After
    public void tearDown() throws Exception {
        if (topicArn != null) {
            sns.deleteTopic(DeleteTopicRequest.builder().topicArn(topicArn).build());
        }
        if (queueUrl != null) {
            sqs.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
        }
        if (subscriptionArn != null) {
            sns.unsubscribe(UnsubscribeRequest.builder().subscriptionArn(subscriptionArn).build());
        }
    }

    /**
     * Tests that we can correctly handle exceptions from SNS.
     */
    @Test
    public void testCloudcastExceptionHandling() {
        try {
            sns.createTopic(CreateTopicRequest.builder().name("").build());
        } catch (AwsServiceException exception) {
            assertEquals("InvalidParameter", exception.awsErrorDetails().errorCode());
            assertTrue(exception.getMessage().length() > 5);
            assertTrue(exception.requestId().length() > 5);
            assertThat(exception.awsErrorDetails().serviceName()).isEqualTo("Sns");
            assertEquals(400, exception.statusCode());
        }
    }

    @Test
    public void testSendUnicodeMessages() throws InterruptedException {
        String unicodeMessage = "你好";
        String unicodeSubject = "主题";
        topicArn = sns.createTopic(CreateTopicRequest.builder().name("unicodeMessageTest-" + System.currentTimeMillis()).build())
                      .topicArn();
        queueUrl = sqs.createQueue(
                CreateQueueRequest.builder().queueName("unicodeMessageTest-" + System.currentTimeMillis()).build())
                      .queueUrl();

        subscriptionArn = Topics.subscribeQueue(sns, sqs, topicArn, queueUrl);
        assertNotNull(subscriptionArn);

        // Verify that the queue is receiving unicode messages
        sns.publish(PublishRequest.builder().topicArn(topicArn).message(unicodeMessage).subject(unicodeSubject).build());
        String message = receiveMessage();
        Map<String, String> messageDetails = parseJSON(message);
        assertEquals(unicodeMessage, messageDetails.get("Message"));
        assertEquals(unicodeSubject, messageDetails.get("Subject"));
        assertNotNull(messageDetails.get("MessageId"));
        assertNotNull(messageDetails.get("Signature"));

        sns.deleteTopic(DeleteTopicRequest.builder().topicArn(topicArn).build());
        topicArn = null;
        sqs.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
        queueUrl = null;
    }

    /**
     * Tests that we can invoke operations on Cloudcast and correctly interpret the responses.
     */
    @Test
    public void testCloudcastOperations() throws Exception {

        // Create Topic
        CreateTopicResponse createTopicResult = sns
                .createTopic(CreateTopicRequest.builder().name("test-topic-" + System.currentTimeMillis()).build());
        topicArn = createTopicResult.topicArn();
        assertTrue(topicArn.length() > 1);

        // List Topics
        Thread.sleep(1000 * 5);
        ListTopicsResponse listTopicsResult = sns.listTopics(ListTopicsRequest.builder().build());
        assertNotNull(listTopicsResult.topics());
        assertTopicIsPresent(listTopicsResult.topics(), topicArn);

        // Set Topic Attributes
        sns.setTopicAttributes(SetTopicAttributesRequest.builder().topicArn(topicArn).attributeName("DisplayName")
                                                        .attributeValue("MyTopicName").build());

        // Get Topic Attributes
        GetTopicAttributesResponse getTopicAttributesResult = sns
                .getTopicAttributes(GetTopicAttributesRequest.builder().topicArn(topicArn).build());
        assertEquals("MyTopicName", getTopicAttributesResult.attributes().get("DisplayName"));

        // Subscribe an SQS queue for notifications
        String queueArn = initializeReceivingQueue();
        SubscribeResponse subscribeResult = sns
                .subscribe(SubscribeRequest.builder().endpoint(queueArn).protocol("sqs").topicArn(topicArn).build());
        subscriptionArn = subscribeResult.subscriptionArn();
        assertTrue(subscriptionArn.length() > 1);

        // List Subscriptions by Topic
        Thread.sleep(1000 * 5);
        ListSubscriptionsByTopicResponse listSubscriptionsByTopicResult = sns
                .listSubscriptionsByTopic(ListSubscriptionsByTopicRequest.builder().topicArn(topicArn).build());
        assertSubscriptionIsPresent(listSubscriptionsByTopicResult.subscriptions(), subscriptionArn);

        // List Subscriptions
        List<Subscription> subscriptions = getAllSubscriptions(sns);
        assertSubscriptionIsPresent(subscriptions, subscriptionArn);

        // Get Subscription Attributes
        Map<String, String> attributes = sns
                .getSubscriptionAttributes(GetSubscriptionAttributesRequest.builder().subscriptionArn(subscriptionArn).build())
                .attributes();
        assertTrue(attributes.size() > 0);
        Entry<String, String> entry = attributes.entrySet().iterator().next();
        assertNotNull(entry.getKey());
        assertNotNull(entry.getValue());

        // Set Subscription Attributes
        sns.setSubscriptionAttributes(
                SetSubscriptionAttributesRequest.builder().subscriptionArn(subscriptionArn).attributeName("DeliveryPolicy")
                                                .attributeValue(DELIVERY_POLICY).build());

        // Publish
        sns.publish(PublishRequest.builder().topicArn(topicArn).message("Hello SNS World").subject("Subject").build());

        // Receive Published Message
        String message = receiveMessage();
        Map<String, String> messageDetails = parseJSON(message);
        assertEquals("Hello SNS World", messageDetails.get("Message"));
        assertEquals("Subject", messageDetails.get("Subject"));
        assertNotNull(messageDetails.get("MessageId"));
        assertNotNull(messageDetails.get("Signature"));

        // Verify Message Signature
        Certificate certificate = getCertificate(messageDetails.get("SigningCertURL"));

        assertTrue(signatureChecker.verifyMessageSignature(message, certificate.getPublicKey()));

        // Add/Remove Permissions
        sns.addPermission(AddPermissionRequest.builder()
                                              .topicArn(topicArn)
                                              .label("foo")
                                              .actionNames("Publish")
                                              .awsAccountIds("750203240092")
                                              .build());
        Thread.sleep(1000 * 5);
        sns.removePermission(RemovePermissionRequest.builder().topicArn(topicArn).label("foo").build());
    }

    /**
     * Get all subscriptions as a list of {@link Subscription} objects
     *
     * @param sns
     *            Client
     * @return List of all subscriptions
     */
    private List<Subscription> getAllSubscriptions(SnsClient sns) {
        ListSubscriptionsResponse result = sns.listSubscriptions(ListSubscriptionsRequest.builder().build());
        List<Subscription> subscriptions = new ArrayList<>(result.subscriptions());
        while (result.nextToken() != null) {
            result = sns.listSubscriptions(ListSubscriptionsRequest.builder().nextToken(result.nextToken()).build());
            subscriptions.addAll(result.subscriptions());
        }
        return subscriptions;
    }

    @Test
    public void testSimplifiedMethods() throws InterruptedException {
        // Create Topic
        CreateTopicResponse createTopicResult =
                sns.createTopic(CreateTopicRequest.builder().name("test-topic-" + System.currentTimeMillis()).build());
        topicArn = createTopicResult.topicArn();
        assertTrue(topicArn.length() > 1);

        // List Topics
        Thread.sleep(1000 * 5);
        ListTopicsResponse listTopicsResult = sns.listTopics(ListTopicsRequest.builder().build());
        assertNotNull(listTopicsResult.topics());
        assertTopicIsPresent(listTopicsResult.topics(), topicArn);

        // Set Topic Attributes
        sns.setTopicAttributes(
                SetTopicAttributesRequest.builder().topicArn(topicArn).attributeName("DisplayName").attributeValue("MyTopicName")
                                         .build());

        // Get Topic Attributes
        GetTopicAttributesResponse getTopicAttributesResult =
                sns.getTopicAttributes(GetTopicAttributesRequest.builder().topicArn(topicArn).build());
        assertEquals("MyTopicName", getTopicAttributesResult.attributes().get("DisplayName"));

        // Subscribe an SQS queue for notifications
        queueUrl = sqs.createQueue(CreateQueueRequest.builder().queueName("subscribeTopicTest-" + System.currentTimeMillis())
                                                     .build())
                      .queueUrl();
        String queueArn = initializeReceivingQueue();
        SubscribeResponse subscribeResult =
                sns.subscribe(SubscribeRequest.builder().topicArn(topicArn).protocol("sqs").endpoint(queueArn).build());
        String subscriptionArn = subscribeResult.subscriptionArn();
        assertTrue(subscriptionArn.length() > 1);

        // List Subscriptions by Topic
        Thread.sleep(1000 * 5);
        ListSubscriptionsByTopicResponse listSubscriptionsByTopicResult =
                sns.listSubscriptionsByTopic(ListSubscriptionsByTopicRequest.builder().topicArn(topicArn).build());
        assertSubscriptionIsPresent(listSubscriptionsByTopicResult.subscriptions(), subscriptionArn);

        // Get Subscription Attributes
        Map<String, String> attributes =
                sns.getSubscriptionAttributes(GetSubscriptionAttributesRequest.builder().subscriptionArn(subscriptionArn).build())
                   .attributes();
        assertTrue(attributes.size() > 0);
        Entry<String, String> entry = attributes.entrySet().iterator().next();
        assertNotNull(entry.getKey());
        assertNotNull(entry.getValue());

        // Set Subscription Attributes
        sns.setSubscriptionAttributes(
                SetSubscriptionAttributesRequest.builder().subscriptionArn(subscriptionArn).attributeName("DeliveryPolicy")
                                                .attributeValue(DELIVERY_POLICY).build());

        // Publish With Subject
        sns.publish(PublishRequest.builder().topicArn(topicArn).message("Hello SNS World").subject("Subject").build());

        // Receive Published Message
        String message = receiveMessage();
        Map<String, String> messageDetails = parseJSON(message);
        assertEquals("Hello SNS World", messageDetails.get("Message"));
        assertEquals("Subject", messageDetails.get("Subject"));
        assertNotNull(messageDetails.get("MessageId"));
        assertNotNull(messageDetails.get("Signature"));

        // Publish Without Subject
        sns.publish(PublishRequest.builder().topicArn(topicArn).message("Hello SNS World").build());

        // Receive Published Message
        message = receiveMessage();
        messageDetails = parseJSON(message);
        assertEquals("Hello SNS World", messageDetails.get("Message"));
        assertNotNull(messageDetails.get("MessageId"));
        assertNotNull(messageDetails.get("Signature"));

        // Add/Remove Permissions
        sns.addPermission(AddPermissionRequest.builder().topicArn(topicArn).label("foo").awsAccountIds("750203240092")
                                              .actionNames("Publish").build());
        Thread.sleep(1000 * 5);
        sns.removePermission(RemovePermissionRequest.builder().topicArn(topicArn).label("foo").build());

        // Unsubscribe
        sns.unsubscribe(UnsubscribeRequest.builder().subscriptionArn(subscriptionArn).build());

        // Delete Topic
        sns.deleteTopic(DeleteTopicRequest.builder().topicArn(topicArn).build());
        topicArn = null;
    }

    /*
     * Private Interface
     */

    /**
     * Polls the SQS queue created earlier in the test until we find our SNS notification message
     * and returns the base64 decoded message body.
     */
    private String receiveMessage() throws InterruptedException {
        int maxRetries = 15;
        while (maxRetries-- > 0) {
            Thread.sleep(1000 * 10);
            List<Message> messages = sqs.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).build()).messages();
            if (messages.size() > 0) {
                return new String(messages.get(0).body());
            }
        }

        fail("No SQS messages received after retrying " + maxRetries + "times");
        return null;
    }

    /**
     * Creates an SQS queue for this test to use when receiving SNS notifications. We need to use an
     * SQS queue because otherwise HTTP or email notifications require a confirmation token that is
     * sent via HTTP or email. Plus an SQS queue lets us test that our notification was delivered.
     */
    private String initializeReceivingQueue() throws InterruptedException {
        String queueName = "sns-integ-test-" + System.currentTimeMillis();
        this.queueUrl = sqs.createQueue(CreateQueueRequest.builder().queueName(queueName).build()).queueUrl();

        Thread.sleep(1000 * 4);
        String queueArn = sqs.getQueueAttributes(
                GetQueueAttributesRequest.builder().queueUrl(queueUrl).attributeNames(QueueAttributeName.QUEUE_ARN)
                                         .build())
                             .attributes().get(QueueAttributeName.QUEUE_ARN);
        HashMap<String, String> attributes = new HashMap<>();
        attributes.put("Policy", generateSqsPolicyForTopic(queueArn, topicArn));
        sqs.setQueueAttributes(SetQueueAttributesRequest.builder().queueUrl(queueUrl).attributesWithStrings(attributes).build());
        int policyPropagationDelayInSeconds = 60;
        System.out.println("Sleeping " + policyPropagationDelayInSeconds + " seconds to let SQS policy propagate");
        Thread.sleep(1000 * policyPropagationDelayInSeconds);
        return queueArn;
    }

    /**
     * Creates a policy to apply to our SQS queue that allows our SNS topic to deliver notifications
     * to it. Note that this policy is for the SQS queue, *not* for SNS.
     */
    private String generateSqsPolicyForTopic(String queueArn, String topicArn) {
        String policy = "{ " + "  \"Version\":\"2008-10-17\"," + "  \"Id\":\"" + queueArn + "/policyId\","
                        + "  \"Statement\": [" + "    {" + "        \"Sid\":\"" + queueArn + "/statementId\","
                        + "        \"Effect\":\"Allow\"," + "        \"Principal\":{\"AWS\":\"*\"},"
                        + "        \"Action\":\"SQS:SendMessage\"," + "        \"Resource\": \"" + queueArn + "\","
                        + "        \"Condition\":{" + "            \"StringEquals\":{\"aws:SourceArn\":\"" + topicArn + "\"}"
                        + "        }" + "    }" + "  ]" + "}";

        return policy;
    }

    private Certificate getCertificate(String certUrl) {
        try {
            return CertificateFactory.getInstance("X509").generateCertificate(getCertificateStream(certUrl));
        } catch (CertificateException e) {
            throw new RuntimeException("Unable to create certificate from " + certUrl, e);
        }
    }

    private InputStream getCertificateStream(String certUrl) {
        try {
            URL cert = new URL(certUrl);
            HttpURLConnection connection = (HttpURLConnection) cert.openConnection();
            if (connection.getResponseCode() != 200) {
                throw new RuntimeException("Received non 200 response when requesting certificate " + certUrl);
            }
            return connection.getInputStream();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to request certificate " + certUrl, e);
        }
    }
}
