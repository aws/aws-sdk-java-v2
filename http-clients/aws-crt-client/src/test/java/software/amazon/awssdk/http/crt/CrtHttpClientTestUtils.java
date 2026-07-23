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

package software.amazon.awssdk.http.crt;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.io.EventLoopGroup;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyMap;

public class CrtHttpClientTestUtils {

    private static final String EVENT_LOOP_GROUP = EventLoopGroup.class.getCanonicalName();

    /**
     * The {@link EventLoopGroup} native resources created since {@code before} was captured, by identity. Groups are only ever
     * created synchronously while a client is constructed, so the difference is the exact set of groups the client under test
     * created, immune to groups from other tests still draining asynchronously in the reused fork. Relies on
     * {@code aws.crt.debugnative} being set.
     */
    static Set<CrtResource> newEventLoopGroups(Set<CrtResource> before) {
        Set<CrtResource> created = liveEventLoopGroups();
        created.removeAll(before);
        return created;
    }

    static Set<CrtResource> liveEventLoopGroups() {
        Set<CrtResource> groups = Collections.newSetFromMap(new IdentityHashMap<>());
        CrtResource.collectNativeResource(resource -> {
            if (EVENT_LOOP_GROUP.equals(resource.canonicalName)) {
                groups.add(resource.getWrapper());
            }
        });
        return groups;
    }

    /**
     * Blocks until none of {@code groups} are in the live event-loop-group set, or the timeout elapses. Event-loop groups are
     * released asynchronously (the bootstrap holds a native reference that drops on its own shutdown callback), so a released
     * group leaves the live set shortly after {@code close()}. Unlike {@code CrtResource.waitForNoResources()}, this only waits
     * on the specific groups passed in, so it does not tear down shared static defaults or depend on other tests' resources
     * having drained.
     *
     * @return {@code true} if all groups were released before the timeout.
     */
    static boolean waitForEventLoopGroupsReleased(Set<CrtResource> groups, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            Set<CrtResource> stillLive = liveEventLoopGroups();
            stillLive.retainAll(groups);
            if (stillLive.isEmpty()) {
                return true;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        Set<CrtResource> stillLive = liveEventLoopGroups();
        stillLive.retainAll(groups);
        return stillLive.isEmpty();
    }

    static Subscriber<ByteBuffer> createDummySubscriber() {
        return new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        };
    }

    static SdkAsyncHttpResponseHandler createTestResponseHandler(AtomicReference<SdkHttpResponse> response,
                                                                 CompletableFuture<Boolean> streamReceived,
                                                                 AtomicReference<Throwable> error,
                                                                 Subscriber<ByteBuffer> subscriber) {
        return new SdkAsyncHttpResponseHandler() {
            @Override
            public void onHeaders(SdkHttpResponse headers) {
                response.compareAndSet(null, headers);
            }
            @Override
            public void onStream(Publisher<ByteBuffer> stream) {
                stream.subscribe(subscriber);
                streamReceived.complete(true);
            }

            @Override
            public void onError(Throwable t) {
                error.compareAndSet(null, t);
            }
        };
    }

    public static SdkHttpFullRequest createRequest(URI endpoint) {
        return createRequest(endpoint, "/", null, SdkHttpMethod.GET, emptyMap());
    }

    static SdkHttpFullRequest createRequest(URI endpoint,
                                             String resourcePath,
                                             byte[] body,
                                             SdkHttpMethod method,
                                             Map<String, String> params) {

        String contentLength = (body == null) ? null : String.valueOf(body.length);
        return SdkHttpFullRequest.builder()
                .uri(endpoint)
                .method(method)
                .encodedPath(resourcePath)
                .applyMutation(b -> params.forEach(b::putRawQueryParameter))
                .applyMutation(b -> {
                    b.putHeader("Host", endpoint.getHost());
                    if (contentLength != null) {
                        b.putHeader("Content-Length", contentLength);
                    }
                }).build();
    }
}
