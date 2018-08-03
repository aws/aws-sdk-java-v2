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

import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkProtectedApi;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@SdkProtectedApi
public final class EventPublishingTransformer<ResponseT> implements AsyncResponseTransformer<ResponseT, Void> {
    private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();

    @Override
    public void responseReceived(ResponseT response) {
        eventQueue.add(new ResponseReceived<>(response));
    }

    @Override
    public void onStream(Publisher<ByteBuffer> publisher) {
        eventQueue.add(new NewStream(publisher));
    }

    @Override
    public void exceptionOccurred(Throwable throwable) {
        eventQueue.add(new ExceptionOccurred(throwable));
    }

    @Override
    public Void complete() {
        return null;
    }

    public BlockingQueue<Event> eventQueue() {
        return eventQueue;
    }

    public interface Event {
    }

    public static class ResponseReceived<ResponseT> implements Event {
        private final ResponseT response;

        public ResponseReceived(ResponseT response) {
            this.response = response;
        }

        public ResponseT getResponse() {
            return response;
        }
    }

    public static class NewStream implements Event {
        private final Publisher<ByteBuffer> stream;

        public NewStream(Publisher<ByteBuffer> stream) {
            this.stream = stream;
        }

        public Publisher<ByteBuffer> getStream() {
            return stream;
        }
    }

    public static class ExceptionOccurred implements Event {
        private final Throwable exception;

        public ExceptionOccurred(Throwable exception) {
            this.exception = exception;
        }

        public Throwable getException() {
            return exception;
        }
    }
}
