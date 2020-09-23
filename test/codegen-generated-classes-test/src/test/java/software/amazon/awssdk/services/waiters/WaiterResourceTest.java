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

package software.amazon.awssdk.services.waiters;

import java.util.concurrent.ScheduledExecutorService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.services.restjsonwithwaiters.RestJsonWithWaitersAsyncClient;
import software.amazon.awssdk.services.restjsonwithwaiters.RestJsonWithWaitersClient;
import software.amazon.awssdk.services.restjsonwithwaiters.waiters.RestJsonWithWaitersAsyncWaiter;
import software.amazon.awssdk.services.restjsonwithwaiters.waiters.RestJsonWithWaitersWaiter;

@RunWith(MockitoJUnitRunner.class)
public class WaiterResourceTest {
    @Mock
    private RestJsonWithWaitersClient client;

    @Mock
    private RestJsonWithWaitersAsyncClient asyncClient;

    @Mock
    private ScheduledExecutorService executorService;

    @Test
    public void closeSyncWaiter_customizedClientProvided_shouldNotCloseClient() {
        RestJsonWithWaitersWaiter waiter = RestJsonWithWaitersWaiter.builder()
                                                                    .client(client)
                                                                    .build();

        waiter.close();
        Mockito.verify(client, Mockito.never()).close();
    }

    @Test
    public void closeAsyncWaiter_customizedClientAndExecutorServiceProvided_shouldNotClose() {
        RestJsonWithWaitersAsyncWaiter waiter = RestJsonWithWaitersAsyncWaiter.builder()
                                                                              .client(asyncClient)
                                                                              .scheduledExecutorService(executorService)
                                                                              .build();

        waiter.close();
        Mockito.verify(client, Mockito.never()).close();
        Mockito.verify(executorService, Mockito.never()).shutdown();
    }
}
