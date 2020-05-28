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

package software.amazon.awssdk.http.apache.internal.conn;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.http.conn.HttpClientConnectionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Tests for {@link IdleConnectionReaper}.
 */
@RunWith(MockitoJUnitRunner.class)
public class IdleConnectionReaperTest {
    private static final long SLEEP_PERIOD = 250;

    private final Map<HttpClientConnectionManager, Long> connectionManagers = new HashMap<>();

    @Mock
    public ExecutorService executorService;

    @Mock
    public HttpClientConnectionManager connectionManager;

    private IdleConnectionReaper idleConnectionReaper;

    @Before
    public void methodSetup() {
        this.connectionManagers.clear();
        idleConnectionReaper = new IdleConnectionReaper(connectionManagers, () -> executorService, SLEEP_PERIOD);
    }

    @Test
    public void setsUpExecutorIfManagerNotPreviouslyRegistered() {
        idleConnectionReaper.registerConnectionManager(connectionManager, 1L);
        verify(executorService).execute(any(Runnable.class));
    }

    @Test
    public void shutsDownExecutorIfMapEmptied() {
        // use register method so it sets up the executor
        idleConnectionReaper.registerConnectionManager(connectionManager, 1L);
        idleConnectionReaper.deregisterConnectionManager(connectionManager);
        verify(executorService).shutdownNow();
    }

    @Test
    public void doesNotShutDownExecutorIfNoManagerRemoved() {
        idleConnectionReaper.registerConnectionManager(connectionManager, 1L);
        HttpClientConnectionManager someOtherConnectionManager = mock(HttpClientConnectionManager.class);
        idleConnectionReaper.deregisterConnectionManager(someOtherConnectionManager);
        verify(executorService, times(0)).shutdownNow();
    }

    @Test(timeout = 1000L)
    public void testReapsConnections() throws InterruptedException {
        IdleConnectionReaper reaper = new IdleConnectionReaper(new HashMap<>(),
                                                               Executors::newSingleThreadExecutor,
                                                               SLEEP_PERIOD);
        final long idleTime = 1L;
        reaper.registerConnectionManager(connectionManager, idleTime);
        try {
            Thread.sleep(SLEEP_PERIOD * 2);
            verify(connectionManager, atLeastOnce()).closeIdleConnections(eq(idleTime), eq(TimeUnit.MILLISECONDS));
        } finally {
            reaper.deregisterConnectionManager(connectionManager);
        }
    }
}
