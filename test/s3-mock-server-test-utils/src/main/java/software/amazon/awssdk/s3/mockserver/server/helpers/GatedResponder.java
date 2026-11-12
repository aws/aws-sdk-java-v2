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

package software.amazon.awssdk.s3.mockserver.server.helpers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import software.amazon.awssdk.s3.mockserver.helpers.Immutable;
import software.amazon.awssdk.s3.mockserver.server.CapturedBody;
import software.amazon.awssdk.s3.mockserver.server.MockResponder;
import software.amazon.awssdk.s3.mockserver.server.Request;
import software.amazon.awssdk.s3.mockserver.server.Response;
import software.amazon.awssdk.s3.mockserver.server.S3Wiretap;
import software.amazon.awssdk.s3.mockserver.server.ServedVersion;
import software.amazon.awssdk.utils.SdkAutoCloseable;

public final class GatedResponder implements MockResponder, SdkAutoCloseable {

    private final Predicate<Request> held;
    private final MockResponder answer;

    private final Object lock = new Object();
    private final List<Request> heldSoFar = new ArrayList<>();
    private boolean released;
    private boolean closed;

    private GatedResponder(Predicate<Request> held, MockResponder answer) {
        this.held = held;
        this.answer = answer;
    }

    public static GatedResponder holding(Predicate<Request> held, MockResponder answer) {
        return new GatedResponder(held, answer);
    }

    public static GatedResponder register(S3Wiretap wiretap, Predicate<Request> held, MockResponder answer) {
        if (wiretap.mode() != S3Wiretap.Mode.MOCK) {
            throw new IllegalStateException("a GatedResponder needs a MOCK wiretap; in " + wiretap.mode()
                    + " mode no responder is consulted, so nothing would ever be held");
        }
        GatedResponder gate = holding(held, answer);
        wiretap.enqueueResponder(gate);
        return gate;
    }

    @Override
    public boolean matches(Request request) {
        synchronized (lock) {
            if (closed) {
                return false;
            }
        }
        return held.test(request) && answer.matches(request);
    }

    @Override
    public Response respond(Request request, byte[] body) {
        return respond(request, CapturedBody.of(body));
    }

    @Override
    public List<ServedVersion> servedVersions() {
        return answer.servedVersions();
    }

    @Override
    public Response respond(Request request, CapturedBody body) {
        synchronized (lock) {
            if (!released) {
                heldSoFar.add(request);
                lock.notifyAll();
            }
            while (!released) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("interrupted while holding " + request.method() + " "
                            + request.path(), e);
                }
            }
        }
        return answer.respond(request, body);
    }

    public boolean awaitHeld(int count, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        synchronized (lock) {
            while (heldSoFar.size() < count) {
                long remainingMillis = (deadline - System.nanoTime()) / 1_000_000L;
                if (remainingMillis <= 0) {
                    return false;
                }
                try {
                    lock.wait(remainingMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("interrupted while waiting for " + count + " held request(s)", e);
                }
            }
            return true;
        }
    }

    public List<Request> heldRequests() {
        synchronized (lock) {
            return Immutable.copyOfList(heldSoFar);
        }
    }

    public void release() {
        synchronized (lock) {
            released = true;
            lock.notifyAll();
        }
    }

    public boolean isReleased() {
        synchronized (lock) {
            return released;
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            closed = true;
        }
        release();
    }
}
