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

package software.amazon.awssdk.services.cloudwatchevents;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.cloudwatchevents.model.DeleteRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.DescribeRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.DescribeRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.DisableRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.EnableRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.PutRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.RuleState;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class CloudWatchEventsIntegrationTest extends AwsIntegrationTestBase {

    private static final String RULE_NAME = "rule";
    private static final String RULE_DESCRIPTION = "ruleDescription";
    private static final String EVENT_PATTERN = "{ \"source\": [\"aws.ec2\"] }";

    private static CloudWatchEventsClient events;

    @BeforeClass
    public static void setUpClient() throws Exception {
        events = CloudWatchEventsClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();

        events.putRule(PutRuleRequest.builder()
                .name(RULE_NAME)
                .description(RULE_DESCRIPTION)
                .eventPattern(EVENT_PATTERN)
                .build()
        );

        // By default, a newly created rule is enabled
        Assert.assertEquals(RuleState.ENABLED,
                            events.describeRule(DescribeRuleRequest.builder().name(RULE_NAME).build())
                                  .state());
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        events.deleteRule(DeleteRuleRequest.builder().name(RULE_NAME).build());
    }

    @Test
    public void basicTest() {

        events.enableRule(EnableRuleRequest.builder().name(RULE_NAME).build());

        DescribeRuleResponse describeRuleResult = events.describeRule(DescribeRuleRequest.builder()
                                                                            .name(RULE_NAME).build());

        Assert.assertEquals(RULE_NAME, describeRuleResult.name());
        Assert.assertEquals(RULE_DESCRIPTION, describeRuleResult.description());
        Assert.assertEquals(RuleState.ENABLED,
                            describeRuleResult.state());

        events.disableRule(DisableRuleRequest.builder().name(RULE_NAME).build());

        Assert.assertEquals(RuleState.DISABLED,
                            events.describeRule(DescribeRuleRequest.builder().name(RULE_NAME).build())
                                  .state());

    }

}
