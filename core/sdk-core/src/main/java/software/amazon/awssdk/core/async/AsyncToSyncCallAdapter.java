/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.async;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.utils.IoUtils;

@SdkProtectedApi
public final class AsyncToSyncCallAdapter<ResponseT, TransformT> {
    private static final Logger LOG = LoggerFactory.getLogger(AsyncToSyncCallAdapter.class);
    private final PublisherToInputStreamAdapter publisherAdapter = new PublisherToInputStreamAdapter();
    private final ResponseTransformer<ResponseT, TransformT> responseTransformer;
    private final EventPublishingTransformer<ResponseT> asyncTransformer;
    private final CompletableFuture<?> completableFuture;

    private ResponseT response = null;
    private TransformT transform = null;

    public AsyncToSyncCallAdapter(ResponseTransformer<ResponseT, TransformT> responseTransformer,
                                  EventPublishingTransformer<ResponseT> asyncTransformer,
                                  CompletableFuture<Void> completableFuture) {
        this.responseTransformer = responseTransformer;
        this.asyncTransformer = asyncTransformer;
        this.completableFuture = completableFuture;
    }

    public TransformT get() throws ExecutionException, InterruptedException {
        while (!completableFuture.isDone()) {
            processNextEvent();
        }

        // Call get() on the future so that any exceptions that occurred get
        // bubbled up through the calling thread.
        completableFuture.get();

        return transform;
    }

    @SuppressWarnings("unchecked")
    private void processNextEvent() throws InterruptedException {
        EventPublishingTransformer.Event ev = nextEvent();

        if (ev == null) {
            return;
        }

        if (ev instanceof EventPublishingTransformer.ResponseReceived) {
            response = ((EventPublishingTransformer.ResponseReceived<ResponseT>) ev).getResponse();
        } else if (ev instanceof EventPublishingTransformer.NewStream) {
            Publisher<ByteBuffer> p = ((EventPublishingTransformer.NewStream) ev).getStream();
            AbortableInputStream is = adaptPublisher(p);
            try {
                transform = responseTransformer.apply(response, is);
            } catch (Exception e) {
                LOG.warn("Response transformer threw exception while consuming stream", e);
            } finally {
                IoUtils.closeQuietly(is, LOG);
            }
        }
    }

    private EventPublishingTransformer.Event nextEvent() throws InterruptedException {
        try {
            // FIXME: We need to poll because there is no hook/callback when
            // a request is finished exceptionally. It's also why we need to
            // to rely on completableFuture.isDone() in get() for loop
            // termination.
            //
            // From a latency standpoint, this is not good.
            return asyncTransformer.eventQueue().poll(5, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            completableFuture.cancel(true);
            throw ie;
        }
    }

    private AbortableInputStream adaptPublisher(Publisher<ByteBuffer> publisher) {
        InputStream is = publisherAdapter.adapt(publisher);
        AbortableInputStream abortableIs = new AbortableInputStream(is, () -> invokeSafely(is::close));
        return abortableIs;
    }
}
