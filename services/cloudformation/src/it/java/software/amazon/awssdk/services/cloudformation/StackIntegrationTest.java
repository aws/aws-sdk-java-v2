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

package software.amazon.awssdk.services.cloudformation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkGlobalTime;
import software.amazon.awssdk.core.auth.policy.Action;
import software.amazon.awssdk.core.auth.policy.Policy;
import software.amazon.awssdk.core.auth.policy.Resource;
import software.amazon.awssdk.core.auth.policy.Statement;
import software.amazon.awssdk.core.auth.policy.Statement.Effect;
import software.amazon.awssdk.services.cloudformation.model.AlreadyExistsException;
import software.amazon.awssdk.services.cloudformation.model.CancelUpdateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.CreateStackResponse;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourceRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourceResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.GetStackPolicyRequest;
import software.amazon.awssdk.services.cloudformation.model.GetStackPolicyResponse;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateRequest;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateResponse;
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.SetStackPolicyRequest;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;
import software.amazon.awssdk.services.cloudformation.model.StackResource;
import software.amazon.awssdk.services.cloudformation.model.StackResourceDetail;
import software.amazon.awssdk.services.cloudformation.model.StackResourceSummary;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.awssdk.services.cloudformation.model.StackSummary;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackResponse;
import software.amazon.awssdk.testutils.Waiter;

/**
 * TODO Remove Ignore
 * Tests of the Stack APIs : CloudFormation
 */
@Ignore
public class StackIntegrationTest extends CloudFormationIntegrationTestBase {

    private static final String STACK_NAME_PREFIX = StackIntegrationTest.class.getName().replace('.', '-');

    /** The initial stack policy which allows access to all resources. */
    private static final Policy INIT_STACK_POLICY;
    private static Logger LOG = LoggerFactory.getLogger(StackIntegrationTest.class);
    private static final int PAGINATION_THRESHOLD = 120;
    private static String testStackName;
    private static String testStackId;

    static {
        INIT_STACK_POLICY = new Policy().withStatements(new Statement(Effect.Allow).withActions(
                new Action("Update:*")).withResources(new Resource("*")));
    }

    // Create a stack to be used by the other tests.
    // Many of the tests ought to be able to run successfully while the stack is
    // being built.
    @BeforeClass
    public static void createTestStacks() throws Exception {
        testStackName = uniqueName();
        CreateStackResponse response = cf.createStack(CreateStackRequest.builder()
                                                                      .templateURL(templateUrlForStackIntegrationTests)
                                                                      .stackName(testStackName)
                                                                      .stackPolicyBody(INIT_STACK_POLICY.toJson()).build());
        testStackId = response.stackId();
    }

    @AfterClass
    public static void tearDown() {
        CloudFormationIntegrationTestBase.tearDown();
        try {
            cf.deleteStack(DeleteStackRequest.builder().stackName(testStackName).build());
        } catch (Exception e) {
            // do not do any thing here.
        }
    }

    /** assertEquals between two policy objects (assuming the Statements are in the same order) */
    private static void assertPolicyEquals(Policy expected, Policy actual) {
        assertEquals(expected.getStatements().size(), actual.getStatements().size());

        Iterator<Statement> iter1 = expected.getStatements().iterator();
        Iterator<Statement> iter2 = actual.getStatements().iterator();

        while (iter1.hasNext() && iter2.hasNext()) {
            Statement s1 = iter1.next();
            Statement s2 = iter2.next();
            assertEquals(s1.getEffect(), s2.getEffect());
            assertEquals(s1.getActions().size(), s2.getActions().size());
            for (int i = 0; i < s1.getActions().size(); i++) {
                assertTrue(s1.getActions().get(i).getActionName()
                             .equalsIgnoreCase(s2.getActions().get(i).getActionName()));
            }
            assertEquals(s1.getResources().size(), s2.getResources().size());
            for (int i = 0; i < s1.getResources().size(); i++) {
                assertTrue(s1.getResources().get(i).getId().equalsIgnoreCase(s2.getResources().get(i).getId()));
            }

        }
        // Unnecessary... but just to be safe
        if (iter1.hasNext() || iter2.hasNext()) {
            fail("The two policy have difference number of Statments.");
        }
    }

    private static String uniqueName() {
        return STACK_NAME_PREFIX + "-" + System.currentTimeMillis();
    }

    @Test
    public void testDescribeStacks() throws Exception {
        DescribeStacksResponse response = cf.describeStacks(DescribeStacksRequest.builder().stackName(testStackName).build());

        assertEquals(1, response.stacks().size());
        assertEquals(testStackId, response.stacks().get(0).stackId());

        response = cf.describeStacks(DescribeStacksRequest.builder().build());
        assertTrue(response.stacks().size() >= 1);
    }

    @Test
    public void testDescribeStackResources() throws Exception {

        DescribeStackResourcesResponse response = null;

        int attempt = 0;
        while (attempt++ < 60 && (response == null || response.stackResources().size() == 0)) {
            Thread.sleep(1000);
            response = cf.describeStackResources(DescribeStackResourcesRequest.builder().stackName(testStackName).build());
        }

        assertTrue(response.stackResources().size() > 0);
        for (StackResource sr : response.stackResources()) {
            assertEquals(testStackId, sr.stackId());
            assertEquals(testStackName, sr.stackName());
            assertNotNull(sr.logicalResourceId());
            assertNotNull(sr.resourceStatus());
            assertNotNull(sr.resourceType());
            assertNotNull(sr.timestamp());
        }
    }

    @Test
    public void testDescribeStackResource() throws Exception {
        DescribeStackResourcesResponse response = null;

        int attempt = 0;
        while (attempt++ < 60 && (response == null || response.stackResources().size() == 0)) {
            Thread.sleep(1000);
            response = cf.describeStackResources(DescribeStackResourcesRequest.builder().stackName(testStackName).build());
        }

        assertTrue(response.stackResources().size() > 0);
        for (StackResource sr : response.stackResources()) {
            assertEquals(testStackId, sr.stackId());
            assertEquals(testStackName, sr.stackName());

            DescribeStackResourceResponse describeStackResource = cf
                    .describeStackResource(DescribeStackResourceRequest.builder()
                                                                       .stackName(testStackName)
                                                                       .logicalResourceId(sr.logicalResourceId())
                                                                       .build());
            StackResourceDetail detail = describeStackResource.stackResourceDetail();
            assertNotNull(detail.lastUpdatedTimestamp());
            assertEquals(sr.logicalResourceId(), detail.logicalResourceId());
            assertEquals(sr.physicalResourceId(), detail.physicalResourceId());
            assertNotNull(detail.resourceStatus());
            assertNotNull(detail.resourceType());
            assertEquals(testStackId, detail.stackId());
            assertEquals(testStackName, detail.stackName());
        }
    }

    @Test
    public void testListStackResources() throws Exception {
        waitForStackToChangeStatus(StackStatus.CREATE_IN_PROGRESS);
        List<StackResourceSummary> stackResourceSummaries = cf.listStackResources(
                ListStackResourcesRequest.builder().stackName(testStackName).build()).stackResourceSummaries();
        for (StackResourceSummary sr : stackResourceSummaries) {
            System.out.println(sr.physicalResourceId());
            assertNotNull(sr.logicalResourceId());
            assertNotNull(sr.physicalResourceId());
            assertNotNull(sr.resourceStatus());
            assertNotNull(sr.resourceType());
        }
    }

    @Test
    public void testGetStackPolicy() {
        GetStackPolicyResponse getStackPolicyResult = cf.getStackPolicy(GetStackPolicyRequest.builder()
                                                                                           .stackName(testStackName).build());
        Policy returnedPolicy = Policy.fromJson(getStackPolicyResult.stackPolicyBody());
        assertPolicyEquals(INIT_STACK_POLICY, returnedPolicy);
    }

    // TODO: Fix test
    @Ignore
    @Test
    public void testSetStackPolicy() throws Exception {
        waitForStackToChangeStatus(StackStatus.CREATE_IN_PROGRESS);

        Policy DENY_ALL_POLICY = new Policy().withStatements(new Statement(Effect.Deny).withActions(
                new Action("Update:*")).withResources(new Resource("*")));
        cf.setStackPolicy(SetStackPolicyRequest.builder().stackName(testStackName).stackPolicyBody(
                DENY_ALL_POLICY.toJson()).build());

        // Compares the policy from GetStackPolicy operation
        GetStackPolicyResponse getStackPolicyResult = cf.getStackPolicy(GetStackPolicyRequest.builder()
                                                                                           .stackName(testStackName).build());
        Policy returnedPolicy = Policy.fromJson(getStackPolicyResult.stackPolicyBody());
        assertPolicyEquals(DENY_ALL_POLICY, returnedPolicy);
    }

    @Test
    public void testDescribeStackEvents() throws Exception {

        DescribeStackEventsResponse response = null;
        int attempt = 0;
        while (attempt++ < 60 && (response == null || response.stackEvents().size() == 0)) {
            Thread.sleep(1000);
            response = cf.describeStackEvents(DescribeStackEventsRequest.builder().stackName(testStackName).build());
        }

        assertTrue(response.stackEvents().size() > 0);

        for (StackEvent e : response.stackEvents()) {
            assertEquals(testStackId, e.stackId());
            assertEquals(testStackName, e.stackName());
            assertNotNull(e.eventId());
            assertNotNull(e.logicalResourceId());
            assertNotNull(e.physicalResourceId());
            assertNotNull(e.resourceStatus());
            assertNotNull(e.resourceType());
            assertNotNull(e.timestamp());
        }
    }

    @Test
    public void testListStacks() throws Exception {
        ListStacksResponse listStacksResult = cf.listStacks(ListStacksRequest.builder().build());
        assertNotNull(listStacksResult);
        assertNotNull(listStacksResult.stackSummaries());
        // There should be some deleted stacks, since we deleted at the start of this test
        assertFalse(listStacksResult.stackSummaries().isEmpty());
        for (StackSummary summary : listStacksResult.stackSummaries()) {
            assertNotNull(summary);
            assertNotNull(summary.stackStatus());
            assertNotNull(summary.creationTime());
            if (summary.stackStatusAsString().contains("DELETE")) {
                assertNotNull(summary.deletionTime());
            }
            assertNotNull(summary.stackId());
            assertNotNull(summary.stackName());
            assertNotNull(summary.templateDescription());
        }

        String nextToken = listStacksResult.nextToken();
        listStacksResult = cf.listStacks(ListStacksRequest.builder().nextToken(nextToken).build());

        assertNotNull(listStacksResult);
        assertNotNull(listStacksResult.stackSummaries());
        // There should be some deleted stacks, since we deleted at the start of this test
        assertFalse(listStacksResult.stackSummaries().isEmpty());
        for (StackSummary summary : listStacksResult.stackSummaries()) {
            assertNotNull(summary);
            assertNotNull(summary.stackStatus());
            assertNotNull(summary.creationTime());
            if (summary.stackStatusAsString().contains("DELETE")) {
                assertNotNull(summary.deletionTime());
            }
            assertNotNull(summary.stackId());
            assertNotNull(summary.stackName());
            assertNotNull(summary.templateDescription());
        }
    }

    // TODO: Fix test
    @Ignore
    @Test
    public void testListStacksFilter() throws Exception {
        ListStacksResponse listStacksResult = cf.listStacks(ListStacksRequest.builder().stackStatusFiltersWithStrings(
                "CREATE_COMPLETE", "DELETE_COMPLETE").build());
        assertNotNull(listStacksResult);
        assertNotNull(listStacksResult.stackSummaries());

        // There should be some deleted stacks, since we deleted at the start of this test
        assertFalse(listStacksResult.stackSummaries().isEmpty());
        for (StackSummary summary : listStacksResult.stackSummaries()) {
            assertNotNull(summary);
            assertNotNull(summary.stackStatus());
            assertTrue(summary.stackStatusAsString().equals("CREATE_COMPLETE")
                       || summary.stackStatusAsString().equals("DELETE_COMPLETE"));
            assertNotNull(summary.creationTime());
            if (summary.stackStatusAsString().contains("DELETE")) {
                assertNotNull(summary.deletionTime());
            }
            assertNotNull(summary.stackId());
            assertNotNull(summary.stackName());
            assertNotNull(summary.templateDescription());
        }
    }

    @Test
    public void testGetTemplate() {
        GetTemplateResponse response = cf.getTemplate(GetTemplateRequest.builder().stackName(testStackName).build());

        assertNotNull(response.templateBody());
        assertTrue(response.templateBody().length() > 0);
    }

    @Test
    public void testCancelUpdateStack() throws Exception {
        waitForStackToChangeStatus(StackStatus.CREATE_IN_PROGRESS);

        List<Stack> stacks = cf.describeStacks(DescribeStacksRequest.builder().stackName(testStackName).build()).stacks();
        assertEquals(1, stacks.size());

        UpdateStackResponse updateStack = cf.updateStack(UpdateStackRequest.builder().stackName(testStackName)
                                                                         .templateURL(
                                                                                 templateUrlForCloudFormationIntegrationTests)
                                                                         .build());
        assertEquals(testStackId, updateStack.stackId());

        cf.cancelUpdateStack(CancelUpdateStackRequest.builder().stackName(testStackName).build());
        waitForStackToChangeStatus(StackStatus.UPDATE_ROLLBACK_IN_PROGRESS);
    }

    @Test
    public void testUpdateStack() throws Exception {
        List<Stack> stacks = cf.describeStacks(DescribeStacksRequest.builder().stackName(testStackName).build()).stacks();
        assertEquals(1, stacks.size());

        UpdateStackResponse updateStack = cf.updateStack(UpdateStackRequest.builder().stackName(testStackName)
                                                                         .templateURL(
                                                                                 templateUrlForCloudFormationIntegrationTests)
                                                                         .stackPolicyBody(INIT_STACK_POLICY.toJson()).build());
        assertEquals(testStackId, updateStack.stackId());
        waitForStackToChangeStatus(StackStatus.UPDATE_IN_PROGRESS);
    }

    @Test
    public void testAlreadyExistsException() {
        try {
            cf.createStack(CreateStackRequest.builder().templateURL(templateUrlForStackIntegrationTests).stackName(
                    testStackName).build());
            fail("Should have thrown an Exception");
        } catch (AlreadyExistsException aex) {
            assertEquals("AlreadyExistsException", aex.awsErrorDetails().errorCode());
        } catch (Exception e) {
            fail("Should have thrown an AlreadyExists Exception.");
        }
    }

    /**
     * Waits up to 15 minutes for the test stack to transition out of the specified status.
     *
     * @param oldStatus
     *            The expected current status of the test stack; this method will return as soon as
     *            the test stack has a status other than this.
     */
    private void waitForStackToChangeStatus(StackStatus oldStatus) throws Exception {
        Waiter.run(() -> cf.describeStacks(d -> d.stackName(testStackName)))
              .until(r -> r.stacks().size() == 1 && r.stacks().get(0).stackStatus() != oldStatus)
              .orFailAfter(Duration.ofMinutes(2));
    }

    /**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */
    @Test
    public void testClockSkew() {
        SdkGlobalTime.setGlobalTimeOffset(3600);
        // Need to create a new client to have the time offset take affect
        CloudFormationClient clockSkewClient = CloudFormationClient.builder()
                                                                   .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
        clockSkewClient.describeStacks(DescribeStacksRequest.builder().build());
        assertTrue(SdkGlobalTime.getGlobalTimeOffset() < 60);
    }
}
