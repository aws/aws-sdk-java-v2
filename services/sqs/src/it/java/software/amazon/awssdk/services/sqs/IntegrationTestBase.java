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

package software.amazon.awssdk.services.sqs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.Before;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.GetUserRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Base class for SQS integration tests. Provides convenience methods for creating test data, and
 * automatically loads AWS credentials from a properties file on disk and instantiates clients for
 * the individual tests to use.
 */
public class IntegrationTestBase extends AwsIntegrationTestBase {

    /**
     * Random number used for naming message attributes.
     */
    private static final Random random = new Random(System.currentTimeMillis());
    /**
     * The Async SQS client for all tests to use.
     */
    protected SqsAsyncClient sqsAsync;

    /**
     * The Sync SQS client for all tests to use.
     */
    protected SqsClient sqsSync;

    /**
     * Account ID of the AWS Account identified by the credentials provider setup in AWSTestBase.
     * Cached for performance
     **/
    private static String accountId;

    /**
     * Loads the AWS account info for the integration tests and creates an SQS client for tests to
     * use.
     */
    @Before
    public void setUp() {
        sqsAsync = createSqsAyncClient();
        sqsSync = createSqsSyncClient();
    }

    public static SqsAsyncClient createSqsAyncClient() {
        return SqsAsyncClient.builder()
                .credentialsProvider(getCredentialsProvider())
                .build();
    }

    public static SqsClient createSqsSyncClient() {
        return SqsClient.builder()
                .credentialsProvider(getCredentialsProvider())
                .build();
    }

    protected static MessageAttributeValue createRandomStringAttributeValue() {
        return MessageAttributeValue.builder().dataType("String").stringValue(UUID.randomUUID().toString()).build();
    }

    protected static MessageAttributeValue createRandomNumberAttributeValue() {
        return MessageAttributeValue.builder().dataType("Number").stringValue(Integer.toString(random.nextInt())).build();
    }

    protected static MessageAttributeValue createRandomBinaryAttributeValue() {
        byte[] randomBytes = new byte[10];
        random.nextBytes(randomBytes);
        return MessageAttributeValue.builder().dataType("Binary").binaryValue(SdkBytes.fromByteArray(randomBytes)).build();
    }

    protected static Map<String, MessageAttributeValue> createRandomAttributeValues(int attrNumber) {
        Map<String, MessageAttributeValue> attrs = new HashMap<String, MessageAttributeValue>();
        for (int i = 0; i < attrNumber; i++) {
            int randomeAttributeType = random.nextInt(3);
            MessageAttributeValue randomAttrValue = null;
            switch (randomeAttributeType) {
                case 0:
                    randomAttrValue = createRandomStringAttributeValue();
                    break;
                case 1:
                    randomAttrValue = createRandomNumberAttributeValue();
                    break;
                case 2:
                    randomAttrValue = createRandomBinaryAttributeValue();
                    break;
                default:
                    break;
            }
            attrs.put("attribute-" + UUID.randomUUID(), randomAttrValue);
        }
        return Collections.unmodifiableMap(attrs);
    }

    /**
     * Helper method to create a SQS queue with a unique name
     *
     * @return The queue url for the created queue
     */
    protected String createQueue(SqsAsyncClient sqsClient) {
        CreateQueueResponse res = sqsClient.createQueue(CreateQueueRequest.builder().queueName(getUniqueQueueName()).build()).join();
        return res.queueUrl();
    }

    /**
     * Generate a unique queue name to use in tests
     */
    protected String getUniqueQueueName() {
        return String.format("%s-%s", getClass().getSimpleName(), System.currentTimeMillis());
    }

    /**
     * Get the account id of the AWS account used in the tests
     */
    protected String getAccountId() {
        if (accountId == null) {
            IamClient iamClient = IamClient.builder()
                    .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                    .region(Region.AWS_GLOBAL)
                    .build();
            accountId = parseAccountIdFromArn(iamClient.getUser(GetUserRequest.builder().build()).user().arn());
        }
        return accountId;
    }

    /**
     * Parse the account ID out of the IAM user arn
     *
     * @param arn IAM user ARN
     * @return Account ID if it can be extracted
     * @throws IllegalArgumentException If ARN is not in a valid format
     */
    private String parseAccountIdFromArn(String arn) throws IllegalArgumentException {
        String[] arnComponents = arn.split(":");
        if (arnComponents.length < 5 || StringUtils.isEmpty(arnComponents[4])) {
            throw new IllegalArgumentException(String.format("%s is not a valid ARN", arn));
        }
        return arnComponents[4];
    }
}
