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

package software.amazon.awssdk.services.autoscaling;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException.ErrorType;
import software.amazon.awssdk.SdkGlobalTime;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.services.autoscaling.model.AlreadyExistsException;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.BlockDeviceMapping;
import software.amazon.awssdk.services.autoscaling.model.CreateAutoScalingGroupRequest;
import software.amazon.awssdk.services.autoscaling.model.CreateLaunchConfigurationRequest;
import software.amazon.awssdk.services.autoscaling.model.CreateOrUpdateTagsRequest;
import software.amazon.awssdk.services.autoscaling.model.DeleteAutoScalingGroupRequest;
import software.amazon.awssdk.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import software.amazon.awssdk.services.autoscaling.model.DeleteNotificationConfigurationRequest;
import software.amazon.awssdk.services.autoscaling.model.DeletePolicyRequest;
import software.amazon.awssdk.services.autoscaling.model.DeleteScheduledActionRequest;
import software.amazon.awssdk.services.autoscaling.model.DeleteTagsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingInstancesRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingInstancesResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingNotificationTypesRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeLaunchConfigurationsResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeNotificationConfigurationsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribePoliciesRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribePoliciesResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeScalingActivitiesRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeScalingActivitiesResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeScheduledActionsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeScheduledActionsResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeTagsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeTagsResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeTerminationPolicyTypesRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeTerminationPolicyTypesResponse;
import software.amazon.awssdk.services.autoscaling.model.Ebs;
import software.amazon.awssdk.services.autoscaling.model.ExecutePolicyRequest;
import software.amazon.awssdk.services.autoscaling.model.Filter;
import software.amazon.awssdk.services.autoscaling.model.LaunchConfiguration;
import software.amazon.awssdk.services.autoscaling.model.NotificationConfiguration;
import software.amazon.awssdk.services.autoscaling.model.PutNotificationConfigurationRequest;
import software.amazon.awssdk.services.autoscaling.model.PutScalingPolicyRequest;
import software.amazon.awssdk.services.autoscaling.model.PutScalingPolicyResponse;
import software.amazon.awssdk.services.autoscaling.model.PutScheduledUpdateGroupActionRequest;
import software.amazon.awssdk.services.autoscaling.model.ResumeProcessesRequest;
import software.amazon.awssdk.services.autoscaling.model.ScalingPolicy;
import software.amazon.awssdk.services.autoscaling.model.ScheduledUpdateGroupAction;
import software.amazon.awssdk.services.autoscaling.model.SetDesiredCapacityRequest;
import software.amazon.awssdk.services.autoscaling.model.SuspendProcessesRequest;
import software.amazon.awssdk.services.autoscaling.model.Tag;
import software.amazon.awssdk.services.autoscaling.model.TagDescription;
import software.amazon.awssdk.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;


/**
 * Integration tests for the Autoscaling service. TODO: No tests for
 * TerminateInstanceInAutoScalingGroup
 */

public class AutoScalingIntegrationTest extends IntegrationTestBase {

    protected static final String TEST_POLICY_NAME = "TestPolicyName";
    protected static final String TEST_ACTION_NAME = "TestActionName";
    protected static final String TEST_ADJUSTMENT_TYPE = "PercentChangeInCapacity";

    protected static final Integer TEST_SCALING_ADJUSTMENT = new Integer(7);
    protected static final Integer TEST_COOLDOWN = new Integer(3);
    protected static final Integer TEST_MIN_SIZE = new Integer(1);
    protected static final Integer TEST_MAX_SIZE = new Integer(10);
    protected static final Integer TEST_DESIRED_CAPACITY = new Integer(5);
    protected static final List<String> TERMINATION_POLICIES;
    protected static final String TERMINATION_POLICY = "ClosestToNextInstanceHour";

    protected static final Date TEST_ACTION_TIME = new Date(new Date().getTime() + 100000);

    static {
        TERMINATION_POLICIES = new LinkedList<String>();
        TERMINATION_POLICIES.add("ClosestToNextInstanceHour");
        TERMINATION_POLICIES.add("Default");
        TERMINATION_POLICIES.add("NewestInstance");
        TERMINATION_POLICIES.add("OldestInstance");
        TERMINATION_POLICIES.add("OldestLaunchConfiguration");
    }

    /**
     * The name of the launch configuration created by these tests
     */

    protected String launchConfigurationName;
    /**
     * The name of the autoscaling group created by these tests
     */

    protected String autoScalingGroupName;
    /**
     * The name of the SNS topic created by the notification operation tests
     */

    private String topicARN;

    /**
     * Releases any resources created by these tests
     */

    @After
    public void tearDown() {
        if (autoScalingGroupName != null) {
            try {
                autoscaling.deleteAutoScalingGroup(DeleteAutoScalingGroupRequest.builder().autoScalingGroupName(
                        autoScalingGroupName).forceDelete(true).build());
            } catch (Exception e) {
                // Ignored.
            }
        }
        if (launchConfigurationName != null) {
            try {
                autoscaling.deleteLaunchConfiguration(DeleteLaunchConfigurationRequest.builder()
                                                              .launchConfigurationName(launchConfigurationName).build());
            } catch (Exception e) {
                // Ignored.
            }
        }
        if (topicARN != null) {
            try {
                sns.deleteTopic(DeleteTopicRequest.builder().topicArn(topicARN).build());
            } catch (Exception e) {
                // Ignored.
            }
        }
    }


    /*
     * Test Methods
     */


    /**
     * Tests that exceptions are properly thrown from the AutoScaling client when the service
     * returns an error.
     */

    @Test
    public void testExceptionHandling() throws Exception {
        try {
            launchConfigurationName = "integ-test-launch-configuration-" + new Date().getTime();
            createLaunchConfiguration(launchConfigurationName);
            createLaunchConfiguration(launchConfigurationName);
        } catch (AlreadyExistsException aee) {
            assertEquals(400, aee.getStatusCode());
            assertEquals("AlreadyExists", aee.getErrorCode());
            assertEquals(ErrorType.Client, aee.getErrorType());
            assertNotEmpty(aee.getMessage());
            assertNotEmpty(aee.getRequestId());
            assertNotEmpty(aee.getServiceName());
        }
    }


    /**
     * Tests that we can create, describe and delete a launch configuration group.
     */

    @Test
    public void testLaunchConfigurationOperations() throws Exception {
        launchConfigurationName = "java-integ-test-config-" + new Date().getTime();
        String userData = "12345678901234567890123456789012345678901234567890"
                          + "12345678901234567890123456789012345678901234567890"
                          + "12345678901234567890123456789012345678901234567890";

        String encodedUserData = new String(Base64.encodeBase64(userData.getBytes()));

        BlockDeviceMapping blockDeviceMapping1 = BlockDeviceMapping.builder()
            .deviceName("xvdh")
            .noDevice(true)
            .build();


        Ebs ebs = Ebs.builder()
            .deleteOnTermination(true)
            .volumeType("io1")
            .volumeSize(10)
            .iops(100)
            .build();

        BlockDeviceMapping blockDeviceMapping2 = BlockDeviceMapping.builder()
            .ebs(ebs)
            .deviceName("/dev/sdh")
            .build();

        // Create a LaunchConfigurationGroup
        CreateLaunchConfigurationRequest createRequest = CreateLaunchConfigurationRequest.builder()
                .launchConfigurationName(launchConfigurationName).imageId(AMI_ID)
                .instanceType(INSTANCE_TYPE).securityGroups("default").userData(encodedUserData)
                .associatePublicIpAddress(false)
                .blockDeviceMappings(Arrays.asList(blockDeviceMapping1, blockDeviceMapping2))
                .build();

        autoscaling.createLaunchConfiguration(createRequest);

        // Describe it
        DescribeLaunchConfigurationsRequest describeRequest = DescribeLaunchConfigurationsRequest.builder()
                .launchConfigurationNames(launchConfigurationName).build();
        
        DescribeLaunchConfigurationsResponse result = autoscaling.describeLaunchConfigurations(describeRequest);
        List<LaunchConfiguration> launchConfigurations = result.launchConfigurations();
        assertEquals(1, launchConfigurations.size());
        LaunchConfiguration launchConfiguration = launchConfigurations.get(0);
        assertNotNull(launchConfiguration.createdTime());
        assertEquals(AMI_ID, launchConfiguration.imageId());
        assertEquals(INSTANCE_TYPE, launchConfiguration.instanceType());
        assertEquals(launchConfigurationName, launchConfiguration.launchConfigurationName());
        assertEquals("default", launchConfiguration.securityGroups().get(0));
        assertEquals(encodedUserData, launchConfiguration.userData());
        assertEquals(false, launchConfiguration.associatePublicIpAddress());
        assertThat(result.launchConfigurations().get(0).blockDeviceMappings(),
                   containsInAnyOrder(blockDeviceMapping1, blockDeviceMapping2));

        // Delete it
        autoscaling.deleteLaunchConfiguration(DeleteLaunchConfigurationRequest.builder()
                                                      .launchConfigurationName(launchConfigurationName).build());
    }


    /**
     * Tests that we can create, describe, set desired capacity and delete autoscaling groups.
     */

    @Test
    public void testAutoScalingGroupOperations() throws Exception {
        autoScalingGroupName = "java-integ-test-scaling-group-" + new Date().getTime();
        launchConfigurationName = "java-integ-test-launch-configuration-" + new Date().getTime();
        createLaunchConfiguration(launchConfigurationName);

        // Create an AutoScalingGroup
        CreateAutoScalingGroupRequest createRequest = CreateAutoScalingGroupRequest.builder()
                .autoScalingGroupName(autoScalingGroupName).launchConfigurationName(launchConfigurationName)
                .availabilityZones(AVAILABILITY_ZONE).maxSize(2).minSize(1).build();
        autoscaling.createAutoScalingGroup(createRequest);

        // Set desired capacity
        autoscaling.setDesiredCapacity(SetDesiredCapacityRequest.builder().autoScalingGroupName(autoScalingGroupName)
                                                                      .desiredCapacity(1).build());

        // Describe
        DescribeAutoScalingGroupsResponse result = autoscaling
                .describeAutoScalingGroups(DescribeAutoScalingGroupsRequest.builder()
                                                   .autoScalingGroupNames(autoScalingGroupName).build());
        List<AutoScalingGroup> autoScalingGroups = result.autoScalingGroups();
        assertEquals(1, autoScalingGroups.size());
        AutoScalingGroup group = autoScalingGroups.get(0);
        // TODO: Commenting out until AutoScaling is ready to release their next API update
        // assertEquals("Active", group.scalingMode());
        assertEquals(1, group.availabilityZones().size());
        assertEquals(AVAILABILITY_ZONE, group.availabilityZones().get(0));
        assertEquals(autoScalingGroupName, group.autoScalingGroupName());
        assertNotNull(group.createdTime());
        assertEquals(1, group.desiredCapacity().intValue());
        assertEquals(0, group.instances().size());
        assertEquals(launchConfigurationName, group.launchConfigurationName());
        assertEquals(0, group.loadBalancerNames().size());
        assertEquals(2, group.maxSize().intValue());
        assertEquals(1, group.minSize().intValue());
        assertEquals(1, group.terminationPolicies().size());
        assertEquals("Default", group.terminationPolicies().get(0));

        // Update
        UpdateAutoScalingGroupRequest updateRequest = UpdateAutoScalingGroupRequest.builder()
                .autoScalingGroupName(autoScalingGroupName).maxSize(3).defaultCooldown(3).minSize(2)
                .terminationPolicies(TERMINATION_POLICY).build();
        autoscaling.updateAutoScalingGroup(updateRequest);

        // Check our updates
        result = autoscaling.describeAutoScalingGroups(DescribeAutoScalingGroupsRequest.builder()
                                                               .autoScalingGroupNames(autoScalingGroupName).build());
        autoScalingGroups = result.autoScalingGroups();
        assertEquals(1, autoScalingGroups.size());
        group = autoScalingGroups.get(0);
        assertEquals((double) 3, (double) group.maxSize(), 0.0);
        assertEquals((double) 3, (double) group.defaultCooldown(), 0.0);
        assertEquals((double) 2, (double) group.minSize(), 0.0);
        assertEquals(1, group.terminationPolicies().size());
        assertEquals(TERMINATION_POLICY, group.terminationPolicies().get(0));

        // Describe the scaling activity of our group
        DescribeScalingActivitiesRequest describeActivityRequest = DescribeScalingActivitiesRequest.builder()
                .autoScalingGroupName(autoScalingGroupName).maxRecords(20).build();
        DescribeScalingActivitiesResponse describeScalingActivitiesResult = autoscaling
                .describeScalingActivities(describeActivityRequest);
        describeScalingActivitiesResult.activities();
    }


    /**
     * Tests that we can create, describe and delete a policies.
     */

    @Test
    public void testPolicies() {
        autoScalingGroupName = "java-integ-test-scaling-group-" + new Date().getTime();
        launchConfigurationName = "java-integ-test-launch-configuration-" + new Date().getTime();
        createLaunchConfiguration(launchConfigurationName);

        // Create an AutoScalingGroup
        CreateAutoScalingGroupRequest createRequest = CreateAutoScalingGroupRequest.builder()
                .autoScalingGroupName(autoScalingGroupName).launchConfigurationName(launchConfigurationName)
                .availabilityZones(AVAILABILITY_ZONE).maxSize(2).minSize(1).build();
        autoscaling.createAutoScalingGroup(createRequest);

        // Put a Policy
        PutScalingPolicyRequest putScalingPolicyRequest = PutScalingPolicyRequest.builder()
            .autoScalingGroupName(autoScalingGroupName)
            .policyName(TEST_POLICY_NAME)
            .scalingAdjustment(TEST_SCALING_ADJUSTMENT)
            .adjustmentType(TEST_ADJUSTMENT_TYPE)
            .cooldown(TEST_COOLDOWN)
            .build();

        PutScalingPolicyResponse putScalingPolicyResult = autoscaling.putScalingPolicy(putScalingPolicyRequest);
        assertNotNull(putScalingPolicyResult);
        assertNotNull(putScalingPolicyResult.policyARN());

        // Describe the Policy
        DescribePoliciesRequest describePoliciesRequest = DescribePoliciesRequest.builder()
            .autoScalingGroupName(autoScalingGroupName)
            .build();

        DescribePoliciesResponse describePoliciesResult = autoscaling.describePolicies(describePoliciesRequest);
        assertNotNull(describePoliciesResult);
        assertEquals(1, describePoliciesResult.scalingPolicies().size());

        ScalingPolicy policy = describePoliciesResult.scalingPolicies().get(0);
        assertEquals(autoScalingGroupName, policy.autoScalingGroupName());
        assertEquals(TEST_POLICY_NAME, policy.policyName());
        assertEquals(TEST_ADJUSTMENT_TYPE, policy.adjustmentType());
        assertEquals(TEST_SCALING_ADJUSTMENT, policy.scalingAdjustment());
        assertEquals(TEST_COOLDOWN, policy.cooldown());
        assertEquals(0, policy.alarms().size());
        assertNotNull(policy.policyARN());

        // Execute the Policy
        ExecutePolicyRequest executePolicyRequest = ExecutePolicyRequest.builder()
            .autoScalingGroupName(autoScalingGroupName)
            .policyName(TEST_POLICY_NAME)
            .build();

        autoscaling.executePolicy(executePolicyRequest);

        // Delete the Policy
        DeletePolicyRequest deletePolicyRequest = DeletePolicyRequest.builder()
            .autoScalingGroupName(autoScalingGroupName)
            .policyName(TEST_POLICY_NAME)
            .build();

        autoscaling.deletePolicy(deletePolicyRequest);

        describePoliciesRequest = DescribePoliciesRequest.builder()
            .autoScalingGroupName(autoScalingGroupName)
            .build();

        describePoliciesResult = autoscaling.describePolicies(describePoliciesRequest);
        assertNotNull(describePoliciesResult);
        assertEquals(0, describePoliciesResult.scalingPolicies().size());
    }


    /**
     * Tests that we can create, describe and delete an action.
     */

    @Test
    public void testActions() {
        autoScalingGroupName = "java-integ-test-scaling-group-" + new Date().getTime();
        launchConfigurationName = "java-integ-test-launch-configuration-" + new Date().getTime();
        createLaunchConfiguration(launchConfigurationName);

        // Create an AutoScalingGroup
        CreateAutoScalingGroupRequest createRequest = CreateAutoScalingGroupRequest.builder()
                .autoScalingGroupName(autoScalingGroupName).launchConfigurationName(launchConfigurationName)
                .availabilityZones(AVAILABILITY_ZONE).maxSize(2).minSize(1)
                .build();
        autoscaling.createAutoScalingGroup(createRequest);

        // Describe the Action
        PutScheduledUpdateGroupActionRequest putScheduledUpdateGroupActionRequest = PutScheduledUpdateGroupActionRequest.builder()
            .autoScalingGroupName(autoScalingGroupName)
            .scheduledActionName(TEST_ACTION_NAME)
            .startTime(TEST_ACTION_TIME)
            .minSize(TEST_MIN_SIZE)
            .maxSize(TEST_MAX_SIZE)
            .desiredCapacity(TEST_DESIRED_CAPACITY)
            .build();

        autoscaling.putScheduledUpdateGroupAction(putScheduledUpdateGroupActionRequest);

        // Describe the Action
        DescribeScheduledActionsRequest describeScheduledActionsRequest = DescribeScheduledActionsRequest.builder()
            .autoScalingGroupName(autoScalingGroupName)
            .build();

        DescribeScheduledActionsResponse describeScheduledActionsResult = autoscaling
                .describeScheduledActions(describeScheduledActionsRequest);
        assertNotNull(describeScheduledActionsResult);
        assertEquals(1, describeScheduledActionsResult.scheduledUpdateGroupActions().size());

        ScheduledUpdateGroupAction scheduledUpdateGroupAction = describeScheduledActionsResult
                .scheduledUpdateGroupActions().get(0);
        assertEquals(autoScalingGroupName, scheduledUpdateGroupAction.autoScalingGroupName());
        assertEquals(TEST_ACTION_NAME, scheduledUpdateGroupAction.scheduledActionName());
        assertEquals(TEST_MIN_SIZE, scheduledUpdateGroupAction.minSize());
        assertEquals(TEST_MAX_SIZE, scheduledUpdateGroupAction.maxSize());
        assertEquals(TEST_DESIRED_CAPACITY, scheduledUpdateGroupAction.desiredCapacity());
        // assertEquals( TEST_ACTION_TIME.getTime(), scheduledUpdateGroupAction.getTime().getTime()
        // );

        // Delete the Action
        DeleteScheduledActionRequest deleteScheduledActionRequest = DeleteScheduledActionRequest.builder()
            .autoScalingGroupName(autoScalingGroupName)
            .scheduledActionName(TEST_ACTION_NAME)
            .build();

        autoscaling.deleteScheduledAction(deleteScheduledActionRequest);

        describeScheduledActionsRequest = DescribeScheduledActionsRequest.builder()
            .autoScalingGroupName(autoScalingGroupName)
            .build();

        describeScheduledActionsResult = autoscaling.describeScheduledActions(describeScheduledActionsRequest);
        assertNotNull(describeScheduledActionsResult);
        assertEquals(0, describeScheduledActionsResult.scheduledUpdateGroupActions().size());
    }


    /**
     * Tests that we can create, describe and delete an action.
     */

    @Test
    public void testInstancesAndProcesses() {
        autoScalingGroupName = "java-integ-test-scaling-group-" + new Date().getTime();
        launchConfigurationName = "java-integ-test-launch-configuration-" + new Date().getTime();
        createLaunchConfiguration(launchConfigurationName);

        // Create an AutoScalingGroup
        CreateAutoScalingGroupRequest createRequest = CreateAutoScalingGroupRequest.builder()
                .autoScalingGroupName(autoScalingGroupName).launchConfigurationName(launchConfigurationName)
                .availabilityZones(AVAILABILITY_ZONE).maxSize(2).minSize(1).build();
        autoscaling.createAutoScalingGroup(createRequest);

        // Describe Instances
        DescribeAutoScalingInstancesResponse describeAutoScalingInstancesResult = autoscaling
                .describeAutoScalingInstances(DescribeAutoScalingInstancesRequest.builder().build());
        assertNotNull(describeAutoScalingInstancesResult);

        // Suspend Processes
        SuspendProcessesRequest suspendProcessesRequest = SuspendProcessesRequest.builder()
            .autoScalingGroupName(autoScalingGroupName)
            .build();

        autoscaling.suspendProcesses(suspendProcessesRequest);

        // Resume Processes
        ResumeProcessesRequest resumeProcessesRequest = ResumeProcessesRequest.builder()
            .autoScalingGroupName(autoScalingGroupName)
            .build();

        autoscaling.resumeProcesses(resumeProcessesRequest);
    }


    /**
     * Tests that we can invoke the notificaiton related operations correctly.
     */

    @Test
    public void testNotificationOperations() throws Exception {
        autoScalingGroupName = "java-integ-test-scaling-group-" + new Date().getTime();
        launchConfigurationName = "java-integ-test-launch-configuration-" + new Date().getTime();
        createLaunchConfiguration(launchConfigurationName);

        // Create an AutoScalingGroup
        CreateAutoScalingGroupRequest createRequest = CreateAutoScalingGroupRequest.builder()
                .autoScalingGroupName(autoScalingGroupName).launchConfigurationName(launchConfigurationName)
                .availabilityZones(AVAILABILITY_ZONE).maxSize(2).minSize(1)
                .build();
        autoscaling.createAutoScalingGroup(createRequest);

        // Describe Notification Types
        List<String> notificationTypes = autoscaling.describeAutoScalingNotificationTypes(DescribeAutoScalingNotificationTypesRequest.builder().build())
                                                    .autoScalingNotificationTypes();
        assertTrue(notificationTypes.size() > 1);
        String notificationType = notificationTypes.get(0);
        assertNotEmpty(notificationType);

        // PutNotificationConfiguration
        topicARN = sns.createTopic(CreateTopicRequest.builder().name("java-sdk-autoscaling-integ-test-" + System.currentTimeMillis()).build()).topicArn();
        PutNotificationConfigurationRequest putRequest = PutNotificationConfigurationRequest.builder()
                .autoScalingGroupName(autoScalingGroupName).notificationTypes(notificationType)
                .topicARN(topicARN).build();
        autoscaling.putNotificationConfiguration(putRequest);

        // DescribeNotificationConfiguration
        DescribeNotificationConfigurationsRequest describeRequest = DescribeNotificationConfigurationsRequest.builder()
                .autoScalingGroupNames(autoScalingGroupName).build();
        List<NotificationConfiguration> notificationConfigurations = autoscaling.describeNotificationConfigurations(
                describeRequest).notificationConfigurations();
        assertEquals(1, notificationConfigurations.size());
        assertEquals(autoScalingGroupName, notificationConfigurations.get(0).autoScalingGroupName());
        assertEquals(notificationType, notificationConfigurations.get(0).notificationType());
        assertEquals(topicARN, notificationConfigurations.get(0).topicARN());

        // DeleteNotificationConfiguration
        autoscaling.deleteNotificationConfiguration(DeleteNotificationConfigurationRequest.builder()
                                                            .autoScalingGroupName(autoScalingGroupName)
                                                            .topicARN(topicARN).build());
        assertEquals(0, autoscaling.describeNotificationConfigurations(describeRequest).notificationConfigurations()
                                   .size());
    }


    /**
     * Tests that we can invoke the tagging operations.
     */

    @Test
    public void testTagging() {
        autoScalingGroupName = "java-integ-test-scaling-group-" + new Date().getTime();
        launchConfigurationName = "java-integ-test-launch-configuration-" + new Date().getTime();
        createLaunchConfiguration(launchConfigurationName);

        // Create an AutoScalingGroup
        CreateAutoScalingGroupRequest createRequest = CreateAutoScalingGroupRequest.builder()
                .autoScalingGroupName(autoScalingGroupName).launchConfigurationName(launchConfigurationName)
                .availabilityZones(AVAILABILITY_ZONE).maxSize(2).minSize(1).build();
        autoscaling.createAutoScalingGroup(createRequest);

        Map<String, String> tags = new HashMap<String, String>();
        tags.put("tag1", "value1");
        tags.put("tag2", "value2");
        // The CreateOrUpdateTags API will auto-construct an empty value for the
        // tag, even when we don't provide the "Tags.member.n.Value" parameter at
        // all (which is the case when we set the tag value as null in the
        // request). So if we do `tags.put("tag3", null)`, the test would fail.
        tags.put("tag3", "");
        autoscaling.createOrUpdateTags(CreateOrUpdateTagsRequest.builder().tags(convertTagList(tags,
                                                                                               autoScalingGroupName)).build());

        Filter filter = Filter.builder().name("auto-scaling-group").values(autoScalingGroupName).build();

        DescribeTagsResponse describeTags = autoscaling.describeTags(DescribeTagsRequest.builder().filters(filter).build());
        assertEquals(3, describeTags.tags().size());
        for (TagDescription tag : describeTags.tags()) {
            assertEquals(autoScalingGroupName, tag.resourceId());
            assertEquals(tags.get(tag.key()), tag.value());
            assertTrue(tag.propagateAtLaunch());
        }

        // Now delete the tags
        autoscaling.deleteTags(DeleteTagsRequest.builder().tags(convertTagList(tags, autoScalingGroupName)).build());

        describeTags = autoscaling.describeTags(DescribeTagsRequest.builder().filters(filter).build());
        assertEquals(0, describeTags.tags().size());
    }

    @Test
    public void testDescribeTerminationPolicyTypes() {
        DescribeTerminationPolicyTypesResponse describeAdjustmentTypesResult = autoscaling
                .describeTerminationPolicyTypes(DescribeTerminationPolicyTypesRequest.builder().build());
        assertEquals(TERMINATION_POLICIES.toArray(), describeAdjustmentTypesResult.terminationPolicyTypes()
                                                                                  .toArray());
    }

    private Collection<Tag> convertTagList(Map<String, String> tags, String groupName) {
        Collection<Tag> converted = new LinkedList<Tag>();
        for (String key : tags.keySet()) {
            Tag tag = Tag.builder().key(key).value(tags.get(key)).resourceType("auto-scaling-group")
                               .resourceId(groupName).propagateAtLaunch(true).build();
            converted.add(tag);
        }
        return converted;
    }


    /**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */

    @Test
    public void testClockSkewAs() {
        SdkGlobalTime.setGlobalTimeOffset(3600);
        AutoScalingClient clockSkewClient = AutoScalingClient.builder().credentialsProvider(new StaticCredentialsProvider(credentials)).build();
        clockSkewClient.describePolicies(DescribePoliciesRequest.builder().build());
        assertTrue(SdkGlobalTime.getGlobalTimeOffset() < 60);
    }
}

