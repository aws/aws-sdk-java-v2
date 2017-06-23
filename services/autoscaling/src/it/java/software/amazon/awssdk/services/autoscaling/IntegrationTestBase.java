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

import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.services.autoscaling.model.CreateAutoScalingGroupRequest;
import software.amazon.awssdk.services.autoscaling.model.CreateLaunchConfigurationRequest;
import software.amazon.awssdk.services.sns.SNSClient;
import software.amazon.awssdk.test.AwsTestBase;

/**
 * Base class for AutoScaling integration tests. Provides several convenience methods for creating
 * test data, test data values, and automatically loads the AWS credentials from a properties file
 * on disk and instantiates clients for the test subclasses to use.
 *
 * @author Jason Fulghum fulghum@amazon.com
 */
public abstract class IntegrationTestBase extends AwsTestBase {

    /**
     * Region has to be us-east-1 to find AMI '{@value #AMI_ID}'.
     */
    private static final Region REGION = Region.US_EAST_1;

    /*
     * Test data values
     */
    protected static final String AVAILABILITY_ZONE = "us-east-1a";
    protected static final String AMI_ID = "ami-1ecae776";
    protected static final String INSTANCE_TYPE = "m1.small";
    /** Shared Autoscaling client for all tests to use. */
    protected static AutoScalingClient autoscaling;
    /** Shared Autoscaling async client for tests to use. */
    protected static AutoScalingAsyncClient autoscalingAsync;
    /** Shared SNS client for tests to use. */
    protected static SNSClient sns;

    /**
     * Loads the AWS account info for the integration tests and creates an AutoScaling client for
     * tests to use.
     */
    @BeforeClass
    public static void setUp() throws IOException {
        setUpCredentials();
        autoscaling = AutoScalingClient.builder()
                .credentialsProvider(new StaticCredentialsProvider(credentials))
                .region(REGION)
                .build();
        autoscalingAsync = AutoScalingAsyncClient.builder()
                .credentialsProvider(new StaticCredentialsProvider(credentials))
                .region(REGION)
                .build();
        sns = SNSClient.builder()
                .credentialsProvider(new StaticCredentialsProvider(credentials))
                .region(REGION)
                .build();
    }

    /*
     * Test Helper Methods
     */

    /**
     * Creates a launch configuration with the specified name.
     *
     * @param name
     *            The name for the new launch configuration.
     */
    protected void createLaunchConfiguration(String name) {
        CreateLaunchConfigurationRequest createRequest = CreateLaunchConfigurationRequest.builder()
                .launchConfigurationName(name).imageId(AMI_ID).instanceType(INSTANCE_TYPE).build();
        autoscaling.createLaunchConfiguration(createRequest);
    }

    /**
     * Creates an autoscaling group with the specified name and specified launch configuration.
     *
     * @param name
     *            The name of the autoscaling group to create.
     * @param launchConfigurationName
     *            The name of an existing launch configuration to use in the new autoscaling group.
     */
    protected void createAutoscalingGroup(String name, String launchConfigurationName) {
        CreateAutoScalingGroupRequest createRequest = CreateAutoScalingGroupRequest.builder()
                .autoScalingGroupName(name).launchConfigurationName(launchConfigurationName)
                .availabilityZones(AVAILABILITY_ZONE).maxSize(2).minSize(1).build();
        autoscaling.createAutoScalingGroup(createRequest);
    }

}
