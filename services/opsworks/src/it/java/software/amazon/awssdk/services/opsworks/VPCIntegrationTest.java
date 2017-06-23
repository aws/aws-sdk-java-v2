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

package software.amazon.awssdk.services.opsworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.EC2Client;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSubnetRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVpcRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.iam.IAMClient;
import software.amazon.awssdk.services.iam.model.CreateInstanceProfileRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.DeleteInstanceProfileRequest;
import software.amazon.awssdk.services.iam.model.DeleteRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.DeleteRoleRequest;
import software.amazon.awssdk.services.iam.model.InstanceProfile;
import software.amazon.awssdk.services.iam.model.PutRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.opsworks.model.CreateInstanceRequest;
import software.amazon.awssdk.services.opsworks.model.CreateLayerRequest;
import software.amazon.awssdk.services.opsworks.model.CreateStackRequest;
import software.amazon.awssdk.services.opsworks.model.DeleteInstanceRequest;
import software.amazon.awssdk.services.opsworks.model.DeleteLayerRequest;
import software.amazon.awssdk.services.opsworks.model.DeleteStackRequest;
import software.amazon.awssdk.services.opsworks.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.opsworks.model.DescribeStackSummaryRequest;
import software.amazon.awssdk.services.opsworks.model.DescribeStackSummaryResponse;
import software.amazon.awssdk.services.opsworks.model.DescribeStacksRequest;
import software.amazon.awssdk.services.opsworks.model.Instance;
import software.amazon.awssdk.services.opsworks.model.Stack;
import software.amazon.awssdk.services.opsworks.model.StackConfigurationManager;

public class VPCIntegrationTest extends IntegrationTestBase {

    private static String TRUST_POLICY = "{"
                                         + "\"Version\": \"2008-10-17\","
                                         + "\"Statement\": ["
                                         + "{"
                                         + "\"Sid\": \"dotnettestProd\","
                                         + "\"Effect\": \"Allow\","
                                         + "\"Principal\": {"
                                         + "\"Service\": \"opsworks.amazonaws.com\""
                                         + "},"
                                         + "\"Action\": \"sts:AssumeRole\""
                                         + "},"
                                         + "{"
                                         + "\"Sid\": \"dotnettestAlpha\","
                                         + "\"Effect\": \"Allow\","
                                         + "\"Principal\": {"
                                         + "\"AWS\": \"arn:aws:iam::225123898355:root\""
                                         + "},"
                                         + "\"Action\": \"sts:AssumeRole\""
                                         + "}"
                                         + "]"
                                         + "}";


    private static String PERMISSIONS = "{\"Statement\": [{\"Action\": [\"ec2:*\", \"iam:PassRole\","
                                        + "\"cloudwatch:GetMetricStatistics\"],"
                                        + "\"Effect\": \"Allow\","
                                        + "\"Resource\": [\"*\"] }]}";

    private static String stackName = "java-sdk--test-stack-name" + System.currentTimeMillis();

    private String vpcId;

    private String subId;

    private Role role;

    private EC2Client ec2;

    private IAMClient iam;

    private String profileName;

    private String roleName;

    private InstanceProfile instanceProfile;

    @Test
    public void testVpc() throws InterruptedException {
        initialize();

        Thread.sleep(1000 * 30);

        try {
            String stackId = opsWorks.createStack(CreateStackRequest.builder()
                                                                    .name(stackName)
                                                                    .region("us-east-1")
                                                                    .vpcId(vpcId)
                                                                    .defaultSubnetId(subId)
                                                                    .serviceRoleArn(role.arn())
                                                                    .defaultInstanceProfileArn(instanceProfile.arn())
                                                                    .configurationManager(
                                                                            StackConfigurationManager.builder().name("Chef")
                                                                                                     .version("0.9").build())
                                                                    .build()
            ).stackId();


            Stack stack = opsWorks.describeStacks(DescribeStacksRequest.builder().stackIds(stackId).build()).stacks().get(0);

            assertEquals(vpcId, stack.vpcId());
            assertEquals(subId, stack.defaultSubnetId());


            String layerId = opsWorks.createLayer(CreateLayerRequest.builder()
                                                                    .name("foo")
                                                                    .shortname("fo")
                                                                    .stackId(stackId)
                                                                    .type("custom").build()).layerId();


            String instanceId = opsWorks.createInstance(CreateInstanceRequest.builder()
                                                                             .stackId(stackId)
                                                                             .layerIds(layerId)
                                                                             .subnetId(subId)
                                                                             .instanceType("m1.small").build()
            ).instanceId();


            Instance instance = opsWorks.describeInstances(DescribeInstancesRequest.builder()
                                                                                   .instanceIds(instanceId).build()).instances()
                                        .get(0);

            assertEquals(subId, instance.subnetId());

            DescribeStackSummaryResponse describeStackSummaryResult =
                    opsWorks.describeStackSummary(DescribeStackSummaryRequest.builder().stackId(stackId).build());

            assertEquals(stackId, describeStackSummaryResult.stackSummary().stackId());
            assertEquals(stackName, describeStackSummaryResult.stackSummary().name());
            assertEquals(new Integer(1), describeStackSummaryResult.stackSummary().layersCount());
            assertNotNull(describeStackSummaryResult.stackSummary().instancesCount());
            assertEquals(new Integer(0), describeStackSummaryResult.stackSummary().appsCount());

            opsWorks.deleteInstance(DeleteInstanceRequest.builder().instanceId(instanceId).build());

            opsWorks.deleteLayer(DeleteLayerRequest.builder().layerId(layerId).build());
            opsWorks.deleteStack(DeleteStackRequest.builder().stackId(stackId).build());
        } finally {
            try {
                ec2.deleteSubnet(DeleteSubnetRequest.builder().subnetId(subId).build());

                ec2.deleteVpc(DeleteVpcRequest.builder().vpcId(vpcId).build());

                iam.deleteInstanceProfile(DeleteInstanceProfileRequest.builder().instanceProfileName(profileName).build());
                iam.deleteRolePolicy(DeleteRolePolicyRequest.builder().roleName(roleName).policyName("TestPolicy").build());
                iam.deleteRole(DeleteRoleRequest.builder().roleName(roleName).build());
            } catch (Exception e) {
                // Ignored or expected.
            }

        }
    }

    private void initialize() throws InterruptedException {
        iam = IAMClient.builder()
                       .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                       .region(Region.AWS_GLOBAL)
                       .build();
        ec2 = EC2Client.builder()
                       .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                       .region(Region.US_EAST_1)
                       .build();
        roleName = "java-test-role" + System.currentTimeMillis();
        profileName = "java-profile" + System.currentTimeMillis();
        role = iam.createRole(CreateRoleRequest.builder().roleName(roleName).assumeRolePolicyDocument(TRUST_POLICY).build())
                  .role();

        iam.putRolePolicy(
                PutRolePolicyRequest.builder().policyName("TestPolicy").roleName(roleName).policyDocument(PERMISSIONS).build());

        instanceProfile = iam
                .createInstanceProfile(CreateInstanceProfileRequest.builder().instanceProfileName(profileName).build())
                .instanceProfile();
        vpcId = ec2.createVpc(CreateVpcRequest.builder().cidrBlock("10.2.0.0/16").build()).vpc().vpcId();

        do {
            Thread.sleep(1000 * 2);
        } while (!ec2.describeVpcs(DescribeVpcsRequest.builder().vpcIds(vpcId).build()).vpcs().get(0).state()
                     .equals("available"));

        subId = ec2.createSubnet(CreateSubnetRequest.builder().vpcId(vpcId).cidrBlock("10.2.0.0/24").build()).subnet().subnetId();

        do {
            Thread.sleep(1000 * 2);
        }
        while (!ec2.describeSubnets(DescribeSubnetsRequest.builder().subnetIds(subId).build()).subnets().get(0).state()
                   .equals("available"));
    }

}
