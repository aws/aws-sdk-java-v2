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

import static org.junit.Assert.fail;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.model.Subscription;
import software.amazon.awssdk.services.sns.model.Topic;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

/**
 * Base class for SNS integration tests; responsible for loading AWS account info for running the
 * tests, and instantiating clients, etc.
 */
public abstract class IntegrationTestBase extends AwsIntegrationTestBase {

    protected static SnsClient sns;
    protected static SqsClient sqs;

    /**
     * Loads the AWS account info for the integration tests and creates SNS and SQS clients for
     * tests to use.
     */
    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        sns = SnsClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(getCredentials()))
                .region(Region.US_WEST_2)
                .build();

        sqs = SqsClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(getCredentials()))
                .region(Region.US_WEST_2)
                .build();
    }

    /**
     * Asserts that the list of topics contains one with the specified ARN, otherwise fails the
     * current test.
     */
    protected void assertTopicIsPresent(List<Topic> topics, String topicArn) {
        for (Topic topic : topics) {
            if (topic.topicArn().equals(topicArn)) {
                return;
            }
        }

        fail("Topic '" + topicArn + "' was not present in specified list of topics.");
    }

    /**
     * Asserts that the list of subscriptions contains one with the specified ARN, otherwise fails
     * the current test.
     */
    protected void assertSubscriptionIsPresent(List<Subscription> subscriptions, String subscriptionArn) {
        for (Subscription subscription : subscriptions) {
            if (subscription.subscriptionArn().equals(subscriptionArn)) {
                return;
            }
        }

        fail("Subscription '" + subscriptionArn + "' was not present in specified list of subscriptions.");
    }

    /**
     * Turns a one level deep JSON string into a Map.
     */
    protected Map<String, String> parseJSON(String jsonmessage) {
        Map<String, String> parsed = new HashMap<String, String>();
        JsonFactory jf = new JsonFactory();
        try {
            JsonParser parser = jf.createJsonParser(jsonmessage);
            parser.nextToken(); // shift past the START_OBJECT that begins the JSON
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String fieldname = parser.getCurrentName();
                parser.nextToken(); // move to value, or START_OBJECT/START_ARRAY
                String value = parser.getText();
                parsed.put(fieldname, value);
            }
        } catch (JsonParseException e) {
            // JSON could not be parsed
            e.printStackTrace();
        } catch (IOException e) {
            // Rare exception
        }
        return parsed;
    }

}
