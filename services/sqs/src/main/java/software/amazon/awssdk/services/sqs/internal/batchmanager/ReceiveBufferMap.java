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

package software.amazon.awssdk.services.sqs.internal.batchmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * Tracks one {@link ReceiveBatchManager} per queue URL, up to a limit. At the limit, adding a queue evicts the buffer idle
 * the longest, or fails if every buffer is in use. All state is guarded by {@code lock}.
 */
@SdkInternalApi
final class ReceiveBufferMap {

    private static final Logger log = Logger.loggerFor(ReceiveBufferMap.class);

    private final int maxBuffers;
    private final Object lock = new Object();
    private final Map<String, Entry> buffers = new HashMap<>();
    // entries with inUse == 0, oldest first
    private final Set<Entry> idle = new LinkedHashSet<>();
    private volatile boolean closed;

    ReceiveBufferMap(int maxBuffers) {
        this.maxBuffers = maxBuffers;
    }

    /**
     * Returns the queue's entry marked in use, or null. Call {@link #release} once when the request completes.
     */
    Entry acquire(String queueUrl) {
        synchronized (lock) {
            Entry entry = buffers.get(queueUrl);
            if (entry != null) {
                markInUse(entry);
            }
            return entry;
        }
    }

    /**
     * Adds the candidate, or returns the queue's existing entry, marked in use. Call {@link #release} once when the request
     * completes. Returns null if the candidate was already evicted, so the caller loads the queue again. Throws
     * {@link IllegalStateException} if the map is closed or every buffer is in use at the limit.
     */
    Entry add(Entry candidate) {
        Entry entry;
        Entry evicted = null;
        synchronized (lock) {
            if (closed) {
                throw new IllegalStateException("The client has been shut down.");
            }
            entry = buffers.get(candidate.queueUrl);
            if (entry == null) {
                if (candidate.evicted) {
                    return null;
                }
                if (buffers.size() >= maxBuffers) {
                    evicted = removeOldestIdle();
                    if (evicted == null) {
                        throw new IllegalStateException(
                            "Reached the maximum of " + maxBuffers + " receive buffers, and every buffer is in use.");
                    }
                }
                entry = candidate;
                buffers.put(entry.queueUrl, entry);
            }
            markInUse(entry);
        }
        if (evicted != null) {
            IoUtils.closeQuietlyV2(evicted.manager, log);
        }
        return entry;
    }

    void release(Entry entry) {
        synchronized (lock) {
            entry.inUse--;
            if (entry.inUse == 0 && buffers.get(entry.queueUrl) == entry) {
                idle.add(entry);
            }
        }
    }

    boolean isClosed() {
        return closed;
    }

    void close() {
        List<Entry> all;
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            all = new ArrayList<>(buffers.values());
            buffers.clear();
            idle.clear();
        }
        all.forEach(e -> IoUtils.closeQuietlyV2(e.manager, log));
    }

    int size() {
        synchronized (lock) {
            return buffers.size();
        }
    }

    int idleCount() {
        synchronized (lock) {
            return idle.size();
        }
    }

    private void markInUse(Entry entry) {
        idle.remove(entry);
        entry.inUse++;
    }

    private Entry removeOldestIdle() {
        Iterator<Entry> it = idle.iterator();
        if (!it.hasNext()) {
            return null;
        }
        Entry oldest = it.next();
        it.remove();
        buffers.remove(oldest.queueUrl);
        oldest.evicted = true;
        return oldest;
    }

    /**
     * No equals or hashCode, because the idle set relies on identity.
     */
    static final class Entry {
        private final String queueUrl;
        private final ReceiveBatchManager manager;
        private int inUse;
        private boolean evicted;

        Entry(String queueUrl, ReceiveBatchManager manager) {
            this.queueUrl = queueUrl;
            this.manager = manager;
        }

        ReceiveBatchManager manager() {
            return manager;
        }
    }
}
