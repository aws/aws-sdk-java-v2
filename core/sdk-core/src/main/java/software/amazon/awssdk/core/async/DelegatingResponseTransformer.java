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

package software.amazon.awssdk.core.async;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class DelegatingResponseTransformer<Response, Result> implements AsyncResponseTransformer<Response, Result> {
    private final AsyncResponseTransformer<Response, Result> delegate;

    public DelegatingResponseTransformer(AsyncResponseTransformer<Response, Result> delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompletableFuture<Result> prepare() {
        return delegate.prepare();
    }

    @Override
    public void onResponse(Response response) {
        delegate.onResponse(response);
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        delegate.onStream(publisher);
    }

    @Override
    public void exceptionOccurred(Throwable error) {
        delegate.exceptionOccurred(error);
    }
}
