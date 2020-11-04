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

package software.amazon.awssdk.services.autoscaling.waiters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.autoscaling.model.LifecycleState.IN_SERVICE;
import static software.amazon.awssdk.services.autoscaling.model.LifecycleState.PENDING;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;

public class AutoScalingWaiterTest {
    private AutoScalingClient client;

    @Before
    public void setup() {
        client = mock(AutoScalingClient.class);
    }

    @Test(timeout = 30_000)
    @SuppressWarnings("unchecked")
    public void waitUntilGroupInServiceWorks() {
        DescribeAutoScalingGroupsRequest request = DescribeAutoScalingGroupsRequest.builder().build();

        DescribeAutoScalingGroupsResponse response1 =
            DescribeAutoScalingGroupsResponse.builder()
                                             .autoScalingGroups(asg -> asg.minSize(2)
                                                                          .instances(i -> i.lifecycleState(PENDING),
                                                                                     i -> i.lifecycleState(IN_SERVICE),
                                                                                     i -> i.lifecycleState(IN_SERVICE)),
                                                                asg -> asg.minSize(2)
                                                                          .instances(i -> i.lifecycleState(PENDING),
                                                                                     i -> i.lifecycleState(PENDING),
                                                                                     i -> i.lifecycleState(IN_SERVICE)))
                                             .build();


        DescribeAutoScalingGroupsResponse response2 =
            DescribeAutoScalingGroupsResponse.builder()
                                             .autoScalingGroups(asg -> asg.minSize(2)
                                                                          .instances(i -> i.lifecycleState(PENDING),
                                                                                     i -> i.lifecycleState(IN_SERVICE),
                                                                                     i -> i.lifecycleState(IN_SERVICE)),
                                                                asg -> asg.minSize(2)
                                                                          .instances(i -> i.lifecycleState(IN_SERVICE),
                                                                                     i -> i.lifecycleState(IN_SERVICE),
                                                                                     i -> i.lifecycleState(IN_SERVICE)))
                                             .build();

        when(client.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class))).thenReturn(response1, response2);

        AutoScalingWaiter waiter = AutoScalingWaiter.builder()
                                                    .overrideConfiguration(WaiterOverrideConfiguration.builder()
                                                                                                      .maxAttempts(3)
                                                                                                      .backoffStrategy(BackoffStrategy.none())
                                                                                                      .build())
                                                    .client(client)
                                                    .build();

        WaiterResponse<DescribeAutoScalingGroupsResponse> response = waiter.waitUntilGroupInService(request);
        assertThat(response.attemptsExecuted()).isEqualTo(2);
        assertThat(response.matched().response()).hasValueSatisfying(r -> assertThat(r).isEqualTo(response2));
    }
}