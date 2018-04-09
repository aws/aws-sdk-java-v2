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

package software.amazon.awssdk.services.sqs.auth.policy;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.core.auth.policy.Action;
import software.amazon.awssdk.core.auth.policy.Policy;
import software.amazon.awssdk.core.auth.policy.Principal;
import software.amazon.awssdk.core.auth.policy.Statement;
import software.amazon.awssdk.core.auth.policy.Statement.Effect;
import software.amazon.awssdk.core.auth.policy.conditions.DateCondition;
import software.amazon.awssdk.core.auth.policy.conditions.DateCondition.DateComparisonType;
import software.amazon.awssdk.services.sqs.IntegrationTestBase;
import software.amazon.awssdk.services.sqs.SqsQueueResource;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

/**
 * Integration tests for the service specific access control policy code provided by the SQS client.
 */
public class SqsPolicyIntegrationTest extends IntegrationTestBase {

    /**
     * Doesn't have to be a valid account id, just has to have a value
     **/
    private static final String ACCOUNT_ID = "123456789";
    private String queueUrl;

    /**
     * Releases all test resources
     */
    @After
    public void tearDown() throws Exception {
        sqsSync.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
    }

    /**
     * Tests that the SQS specific access control policy code works as expected.
     */
    @Test
    public void testPolicies() throws Exception {
        String queueName = getUniqueQueueName();
        queueUrl = sqsSync.createQueue(CreateQueueRequest.builder().queueName(queueName).build()).queueUrl();

        Policy policy = new Policy().withStatements(new Statement(Effect.Allow).withPrincipals(Principal.ALL_USERS)
                                                                               .withActions(new Action("sqs:SendMessage"), new Action("sqs:ReceiveMessage"))
                                                                               .withResources(new SqsQueueResource(ACCOUNT_ID, queueName))
                                                                               .withConditions(new DateCondition(DateComparisonType.DateLessThan,
                        new Date())));
        setQueuePolicy(policy);
    }

    private void setQueuePolicy(Policy policy) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("Policy", policy.toJson());

        sqsSync.setQueueAttributes(SetQueueAttributesRequest.builder().queueUrl(queueUrl).attributes(attributes).build());
    }
}
