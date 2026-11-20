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

package software.amazon.awssdk.s3.mockserver.invariants;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.s3.mockserver.helpers.Immutable;

public final class ObservationSink {

    private static final Logger LOG = LogManager.getLogger(ObservationSink.class);

    private static final ThreadLocal<ObservationSink> ON_THREAD = new ThreadLocal<>();

    private static final Set<ObservationSink> OPEN = ConcurrentHashMap.newKeySet();

    private static final ObservationSink ANY_OPEN = new ObservationSink("<every open test>");

    private final String owner;

    private final Queue<Observation> observations = new ConcurrentLinkedQueue<>();

    private final Map<Invariant, AtomicLong> checks = new EnumMap<>(Invariant.class);

    private final Queue<Destination> destinations = new ConcurrentLinkedQueue<>();

    private final Map<String, Object> sources = new ConcurrentHashMap<>();

    private final Queue<String> harnessErrors = new ConcurrentLinkedQueue<>();

    private boolean open;

    private volatile Consumer<Destination> destinationCheck;

    private ObservationSink(String owner) {
        this.owner = owner;
        for (Invariant invariant : Invariant.values()) {
            checks.put(invariant, new AtomicLong());
        }
    }

    public static ObservationSink open(String owner) {
        ObservationSink stale = ON_THREAD.get();
        if (stale != null) {
            stale.close();
        }
        ObservationSink sink = new ObservationSink(owner);
        synchronized (sink) {
            sink.open = true;
        }
        OPEN.add(sink);
        ON_THREAD.set(sink);
        return sink;
    }

    public static ObservationSink detached(String owner) {
        ObservationSink sink = new ObservationSink(owner);
        synchronized (sink) {
            sink.open = true;
        }
        return sink;
    }

    public static ObservationSink current() {
        ObservationSink mine = ON_THREAD.get();
        if (mine != null && mine.isOpen()) {
            return mine;
        }
        List<ObservationSink> open = new ArrayList<>(OPEN);
        return open.size() == 1 ? open.get(0) : ANY_OPEN;
    }

    private synchronized boolean isOpen() {
        return open;
    }

    public void close() {
        OPEN.remove(this);
        synchronized (this) {
            open = false;
        }
        if (ON_THREAD.get() == this) {
            ON_THREAD.remove();
        }
        sources.clear();
    }

    public void record(Observation observation) {
        if (this != ANY_OPEN) {
            synchronized (this) {
                if (open) {
                    observations.add(observation);
                    return;
                }
            }
        }
        List<ObservationSink> open = new ArrayList<>(OPEN);
        if (this != ANY_OPEN) {
            LOG.warn("observed after {} finished; reporting to {} open test(s): {}", owner, open.size(),
                    observation);
        }
        if (open.isEmpty()) {
            LOG.warn("observed with no test open, so no row can fail on it: {}", observation);
        }
        for (ObservationSink sink : open) {
            if (sink != this) {
                sink.addIfOpen(observation);
            }
        }
    }

    private synchronized void addIfOpen(Observation observation) {
        if (open) {
            observations.add(observation);
        } else {
            LOG.warn("observed while {} was closing; not reported to it: {}", owner, observation);
        }
    }

    public void checked(Invariant invariant) {
        checked(invariant, 1);
    }

    public void checked(Invariant invariant, long count) {
        if (this == ANY_OPEN) {
            return;
        }
        checks.get(invariant).addAndGet(count);
    }

    public List<Observation> observations() {
        return Immutable.copyOfList(observations);
    }

    public Map<Invariant, Long> checkCounts() {
        Map<Invariant, Long> counts = new EnumMap<>(Invariant.class);
        checks.forEach((invariant, count) -> counts.put(invariant, count.get()));
        return counts;
    }

    public String owner() {
        return owner;
    }

    public static void noteSource(String key, Object source) {
        ObservationSink sink = ON_THREAD.get();
        if (sink == null || !sink.isOpen()) {
            LOG.debug("no test on {} to note source {} for", Thread.currentThread().getName(), key);
            return;
        }
        sink.sources.putIfAbsent(key, source);
    }

    public void harnessError(String why) {
        harnessErrors.add(why);
    }

    public List<String> harnessErrors() {
        return Immutable.copyOfList(harnessErrors);
    }

    public List<Object> sources() {
        return Immutable.copyOfList(sources.values());
    }

    public void destination(Path path) {
        if (this != ANY_OPEN) {
            destinations.add(Destination.of(path));
        }
    }

    public List<Destination> destinations() {
        return Immutable.copyOfList(destinations);
    }

    public void destinationCheck(Consumer<Destination> check) {
        this.destinationCheck = check;
    }

    public static void refreshDestination(Path path) {
        ObservationSink sink = ON_THREAD.get();
        if (sink == null || !sink.isOpen() || path == null) {
            return;
        }
        List<Destination> stale = sink.destinations.stream().filter(d -> d.path().equals(path)).collect(Immutable.toList());
        if (!stale.isEmpty() && sink.destinations.removeAll(stale)) {
            sink.destinations.add(Destination.of(path));
        }
    }

    public static void beforeDelete(Path path) {
        ObservationSink sink = ON_THREAD.get();
        if (sink == null || !sink.isOpen() || path == null) {
            return;
        }
        Consumer<Destination> check = sink.destinationCheck;
        if (check == null) {
            return;
        }
        List<Destination> due = sink.destinations.stream().filter(d -> d.path().equals(path)).collect(Immutable.toList());
        sink.destinations.removeAll(due);
        for (Destination destination : due) {
            try {
                check.accept(destination);
            } catch (RuntimeException e) {
                sink.harnessError(destination.path() + " could not be judged before it was deleted: " + e);
            }
        }
    }
}
