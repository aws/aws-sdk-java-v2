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
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.sync.RequestBody;

import java.nio.ByteBuffer;

@SdkProtectedApi
public final class RequestBodyToAsyncAdapter {

    public AsyncRequestBody adapt(RequestBody requestBody) {
        return new AsyncRequestBodyImpl(requestBody);
    }

    private static class AsyncRequestBodyImpl implements AsyncRequestBody {
        private final InputStreamToPublisherAdapter adapter = new InputStreamToPublisherAdapter();
        private final long contentLength;
        private final Publisher<ByteBuffer> publisher;

        private AsyncRequestBodyImpl(RequestBody syncRequestBody) {
            this.contentLength = syncRequestBody.contentLength();
            this.publisher = adapter.adapt(syncRequestBody.asStream());
        }

        @Override
        public long contentLength() {
            return contentLength;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            publisher.subscribe(subscriber);
        }
    }
}
