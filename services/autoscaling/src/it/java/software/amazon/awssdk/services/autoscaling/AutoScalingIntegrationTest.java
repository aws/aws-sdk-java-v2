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

package software.amazon.awssdk.services.autoscaling;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkGlobalTime;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.autoscaling.model.DescribePoliciesRequest;


/**
 * Smoke tests for Autoscaling service. This class tests query protocol path.
 * Do not remove until we have generated smoke tests for this service.
 */

public class AutoScalingIntegrationTest extends IntegrationTestBase {

    @BeforeAll
    public static void beforeAll() throws IOException {
        setUp();
    }

    @Test
    public void describeAutoScalingGroups() {
        autoscaling.describeAutoScalingGroups();
        autoscalingAsync.describeAutoScalingGroups().join();
    }

    @Test
    public void describeTerminationPolicyTypes() {
        autoscaling.describeTerminationPolicyTypes();
        autoscalingAsync.describeAutoScalingGroups().join();
    }

    @Test
    public void testClockSkewAs() {
        SdkGlobalTime.setGlobalTimeOffset(3600);
        AutoScalingClient clockSkewClient = AutoScalingClient.builder()
                                                             .region(Region.US_EAST_1)
                                                             .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                                             .build();
        clockSkewClient.describePolicies(DescribePoliciesRequest.builder().build());
        assertTrue(SdkGlobalTime.getGlobalTimeOffset() < 60);
    }
}

