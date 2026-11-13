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

package software.amazon.awssdk.s3.mockserver.testarchive;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.s3.mockserver.helpers.Immutable;
import software.amazon.awssdk.s3.mockserver.invariants.Invariant;
import software.amazon.awssdk.s3.mockserver.invariants.Observation;
import software.amazon.awssdk.s3.mockserver.invariants.ObservationSink;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedDownload;
import software.amazon.awssdk.transfer.s3.model.Transfer;

public final class CallerFutures {

    private static final Object EXCEPTIONAL = new Object();

    private static final int PUBLISH_YIELDS = 10_000;

    private final List<Recorded> futures = new ArrayList<>();

    private final Runnable yield;
    private String unrecorded;

    public CallerFutures() {
        this(Thread::yield);
    }

    CallerFutures(Runnable yield) {
        this.yield = yield;
    }

    private static final class Recorded {
        private final String operation;
        private final CompletableFuture<?> future;
        private boolean completed;
        private long completedNanos;
        private Object value;
        private Throwable error;

        Recorded(String operation, CompletableFuture<?> future) {
            this.operation = operation;
            this.future = future;
        }
    }

    private static final class Handler implements InvocationHandler {
        private static final java.util.Set<String> PLAIN_RESULTS =
                Immutable.setOf("serviceName", "serviceClientConfiguration", "toString", "hashCode", "equals");

        private final Object target;
        private final CallerFutures into;

        Handler(Object target, CallerFutures into) {
            this.target = target;
            this.into = into;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object result;
            try {
                result = method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
            if (result instanceof CompletableFuture<?>) {
                into.add(method.getName(), (CompletableFuture<?>) result);
            } else if (result instanceof Transfer) {
                Transfer transfer = (Transfer) result;
                into.add(method.getName(), transfer.completionFuture());
            } else if (result != null && !method.getReturnType().isPrimitive()
                    && !PLAIN_RESULTS.contains(method.getName())) {
                into.unrecorded(method.getName() + "() returned an object that makes its own calls on the"
                        + " client, which are not recorded");
            }
            return result;
        }
    }

    public static S3AsyncClient record(S3AsyncClient client, CallerFutures into) {
        return (S3AsyncClient) Proxy.newProxyInstance(S3AsyncClient.class.getClassLoader(),
                new Class<?>[] {S3AsyncClient.class}, new Handler(client, into));
    }

    public static S3TransferManager recordTransfers(S3AsyncClient client, S3TransferManager transferManager) {
        if (client != null && Proxy.isProxyClass(client.getClass())
                && Proxy.getInvocationHandler(client) instanceof Handler) {
            Handler handler = (Handler) Proxy.getInvocationHandler(client);
            return (S3TransferManager) Proxy.newProxyInstance(S3TransferManager.class.getClassLoader(),
                    new Class<?>[] {S3TransferManager.class}, new Handler(transferManager, handler.into));
        }
        return transferManager;
    }

    public static S3AsyncClient unwrap(S3AsyncClient client) {
        if (client != null && Proxy.isProxyClass(client.getClass())
                && Proxy.getInvocationHandler(client) instanceof Handler) {
            return (S3AsyncClient) ((Handler) Proxy.getInvocationHandler(client)).target;
        }
        return client;
    }

    public synchronized void unrecorded(String why) {
        if (unrecorded == null) {
            unrecorded = why;
        }
    }

    public synchronized String unrecordedReason() {
        return unrecorded;
    }

    private void add(String operation, CompletableFuture<?> future) {
        Recorded recorded = new Recorded(operation, future);
        synchronized (this) {
            futures.add(recorded);
        }
        future.whenComplete((value, error) -> {
            long now = System.nanoTime();
            if (error == null) {
                markIfStreaming(value);
            }
            synchronized (this) {
                recorded.completed = true;
                recorded.completedNanos = now;
                recorded.value = value;
                recorded.error = error;
            }
        });
    }

    public synchronized OptionalLong lastCompletionNanos() {
        return futures.stream().filter(r -> r.completed).mapToLong(r -> r.completedNanos).max();
    }

    public void check(ObservationSink sink) {
        List<Recorded> snapshot;
        synchronized (this) {
            snapshot = Immutable.copyOfList(futures);
        }
        for (Recorded r : snapshot) {
            sink.checked(Invariant.CALLER_FUTURE_COMPLETES_WITH_A_VALUE);
            String problem = problem(r);
            if (problem != null) {
                sink.record(new Observation(Invariant.CALLER_FUTURE_COMPLETES_WITH_A_VALUE,
                        "the future " + r.operation + "(...) returned " + problem));
            }
        }
    }

    private static boolean streaming(Object value) {
        return value instanceof java.io.InputStream || value instanceof org.reactivestreams.Publisher<?>;
    }

    private static Object valueNow(CompletableFuture<?> future) {
        try {
            return future.getNow(null);
        } catch (java.util.concurrent.CompletionException | java.util.concurrent.CancellationException e) {
            return EXCEPTIONAL;
        }
    }

    private boolean awaitPublished(Recorded r) {
        for (int i = 0; i < PUBLISH_YIELDS; i++) {
            synchronized (this) {
                if (r.completed) {
                    return true;
                }
            }
            yield.run();
        }
        synchronized (this) {
            return r.completed;
        }
    }

    private void markIfStreaming(Object value) {
        if (streaming(value) || value instanceof CompletedDownload<?> && streaming(((CompletedDownload<?>) value).result())) {
            unrecorded("a future completed with " + value.getClass().getSimpleName() + ", a stream the caller"
                    + " drains itself, so later requests are its own demand rather than work after the"
                    + " outcome");
        }
    }

    private String problem(Recorded r) {
        if (!r.future.isDone()) {
            return "was not complete when the test returned";
        }
        boolean completed = awaitPublished(r);
        Object value;
        Throwable error;
        synchronized (this) {
            value = r.value;
            error = r.error;
        }
        if (!completed) {
            Object done = valueNow(r.future);
            if (done != EXCEPTIONAL) {
                markIfStreaming(done);
            }
            unrecorded("the completion callback of " + r.operation + "(...)'s future had not run when the test"
                    + " returned, so when it completed is not known");
            return done == null ? "completed normally with null" : null;
        }
        if (error == null && value == null) {
            return "completed normally with null";
        }
        Object held = valueNow(r.future);
        boolean nowExceptional = held == EXCEPTIONAL;
        Object now = nowExceptional ? null : held;
        if (nowExceptional != (error != null) || (!nowExceptional && !Objects.equals(now, value))) {
            return "completed with " + (error != null ? error : value) + " and later held "
                    + (nowExceptional ? "an exception" : now);
        }
        return null;
    }
}
