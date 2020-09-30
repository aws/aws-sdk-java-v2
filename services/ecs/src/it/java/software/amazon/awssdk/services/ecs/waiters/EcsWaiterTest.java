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

package software.amazon.awssdk.services.ecs.waiters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.Deployment;
import software.amazon.awssdk.services.ecs.model.DescribeServicesRequest;
import software.amazon.awssdk.services.ecs.model.DescribeServicesResponse;

public class EcsWaiterTest {
    private EcsClient client;

    @Before
    public void setup() {
        client = mock(EcsClient.class);
    }

    @Test(timeout = 30_000)
    @SuppressWarnings("unchecked")
    public void waitUntilServicesStableWorks() {
        DescribeServicesRequest request = DescribeServicesRequest.builder().build();

        DescribeServicesResponse response1 =
            DescribeServicesResponse.builder()
                                    .services(s -> s.deployments(Deployment.builder().build())
                                                    .desiredCount(2)
                                                    .runningCount(1))
                                    .build();


        DescribeServicesResponse response2 =
            DescribeServicesResponse.builder()
                                    .services(s -> s.deployments(Deployment.builder().build())
                                                    .desiredCount(2)
                                                    .runningCount(2))
                                    .build();

        when(client.describeServices(any(DescribeServicesRequest.class))).thenReturn(response1, response2);

        EcsWaiter waiter = EcsWaiter.builder()
                                    .overrideConfiguration(WaiterOverrideConfiguration.builder()
                                                                                .maxAttempts(3)
                                                                                .backoffStrategy(BackoffStrategy.none())
                                                                                .build())
                                    .client(client)
                                    .build();

        WaiterResponse<DescribeServicesResponse> response = waiter.waitUntilServicesStable(request);
        assertThat(response.attemptsExecuted()).isEqualTo(2);
        assertThat(response.matched().response()).hasValueSatisfying(r -> assertThat(r).isEqualTo(response2));
    }
}