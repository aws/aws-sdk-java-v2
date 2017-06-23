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

package software.amazon.awssdk.services.applicationautoscaling;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.services.applicationautoscaling.model.DescribeScalingPoliciesRequest;
import software.amazon.awssdk.services.applicationautoscaling.model.DescribeScalingPoliciesResponse;
import software.amazon.awssdk.services.applicationautoscaling.model.ServiceNamespace;
import software.amazon.awssdk.test.AwsIntegrationTestBase;

public class ServiceIntegrationTest extends AwsIntegrationTestBase {

    private static ApplicationAutoScalingClient autoscaling;

    @BeforeClass
    public static void setUp() {
        autoscaling = ApplicationAutoScalingClient.builder()
                .credentialsProvider(new StaticCredentialsProvider(getCredentials()))
                .build();
    }

    @Test
    public void testScalingPolicy() {
        DescribeScalingPoliciesResponse res = autoscaling.describeScalingPolicies(DescribeScalingPoliciesRequest.builder()
                .serviceNamespace(ServiceNamespace.Ecs).build());
        Assert.assertNotNull(res);
        Assert.assertNotNull(res.scalingPolicies());
    }

}
