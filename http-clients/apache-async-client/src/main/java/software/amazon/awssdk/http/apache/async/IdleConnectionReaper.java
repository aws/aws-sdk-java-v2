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

package software.amazon.awssdk.http.apache.async;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;

/**
 * Manages the reaping of idle connections.
 */
@SdkInternalApi
final class IdleConnectionReaper {
    private static final Logger log = LoggerFactory.getLogger(IdleConnectionReaper.class);
    private static final IdleConnectionReaper INSTANCE = new IdleConnectionReaper();

    private final Map<PoolingAsyncClientConnectionManager, Long> connectionManagers;
    private final Supplier<ExecutorService> executorServiceSupplier;
    private final long sleepPeriod;
    private volatile ExecutorService exec;
    private volatile ReaperTask reaperTask;

    private IdleConnectionReaper() {
        this.connectionManagers = new ConcurrentHashMap<>();

        this.executorServiceSupplier = () -> {
            ExecutorService e = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "aws-sdk-java-v2-idle-connection-reaper");
                t.setDaemon(true);
                return t;
            });
            return e;
        };

        this.sleepPeriod = Duration.ofMinutes(1).toMillis();
    }

    @SdkTestInternalApi
    IdleConnectionReaper(
        Map<PoolingAsyncClientConnectionManager, Long> connectionManagers,
        Supplier<ExecutorService> executorServiceSupplier,
        long sleepPeriod
    ) {
        this.connectionManagers = connectionManagers;
        this.executorServiceSupplier = executorServiceSupplier;
        this.sleepPeriod = sleepPeriod;
    }

    /**
     * Register the connection manager with this reaper.
     *
     * @param manager     The connection manager.
     * @param maxIdleTime The maximum time connections in the connection manager are to remain idle before being reaped.
     * @return {@code true} if the connection manager was not previously registered with this reaper, {@code false}
     * otherwise.
     */
    public synchronized boolean registerConnectionManager(PoolingAsyncClientConnectionManager manager, long maxIdleTime) {
        boolean notPreviouslyRegistered = connectionManagers.put(manager, maxIdleTime) == null;
        setupExecutorIfNecessary();
        return notPreviouslyRegistered;
    }

    /**
     * Deregister this connection manager with this reaper.
     *
     * @param manager The connection manager.
     * @return {@code true} If this connection manager was previously registered with this reaper and it was removed, {@code
     * false} otherwise.
     */
    public synchronized boolean deregisterConnectionManager(PoolingAsyncClientConnectionManager manager) {
        boolean wasRemoved = connectionManagers.remove(manager) != null;
        cleanupExecutorIfNecessary();
        return wasRemoved;
    }

    /**
     * @return The singleton instance of this class.
     */
    public static IdleConnectionReaper getInstance() {
        return INSTANCE;
    }

    private void setupExecutorIfNecessary() {
        if (exec != null) {
            return;
        }

        ExecutorService e = executorServiceSupplier.get();
        this.reaperTask = new ReaperTask(connectionManagers, sleepPeriod);
        e.execute(this.reaperTask);
        exec = e;
    }

    private void cleanupExecutorIfNecessary() {
        if (exec == null || !connectionManagers.isEmpty()) {
            return;
        }

        reaperTask.stop();
        reaperTask = null;
        exec.shutdownNow();
        exec = null;
    }

    private static final class ReaperTask implements Runnable {
        private final Map<PoolingAsyncClientConnectionManager, Long> connectionManagers;
        private final long sleepPeriod;
        private volatile boolean stopping = false;

        private ReaperTask(Map<PoolingAsyncClientConnectionManager, Long> connectionManagers,
                           long sleepPeriod) {
            this.connectionManagers = connectionManagers;
            this.sleepPeriod = sleepPeriod;
        }

        @Override
        public void run() {
            while (!stopping) {
                try {
                    Thread.sleep(sleepPeriod);

                    for (Map.Entry<PoolingAsyncClientConnectionManager, Long> entry : connectionManagers.entrySet()) {
                        PoolingAsyncClientConnectionManager connectionManager = entry.getKey();
                        try {
                            connectionManager.closeIdle(TimeValue.ofMilliseconds(entry.getValue()));
                            connectionManager.closeExpired();
                        } catch (Exception t) {
                            log.warn("Unable to close idle and expired connections", t);
                        }
                    }
                } catch (Throwable t) {
                    log.debug("Reaper thread: ", t);
                }
            }
            log.debug("Shutting down reaper thread.");
        }

        private void stop() {
            stopping = true;
        }
    }
}
