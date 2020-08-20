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

import java.util.Objects;
import java.util.concurrent.Future;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.nio.AsyncPushConsumer;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.HandlerFactory;
import org.apache.hc.core5.http.protocol.HttpContext;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * An instance of {@link ConnectionManagerAwareHttpClient} that delegates all the requests to the given http client.
 */
@SdkInternalApi
class ApacheSdkHttpClient implements ConnectionManagerAwareHttpClient {
    private final CloseableHttpAsyncClient delegate;
    private final PoolingAsyncClientConnectionManager cm;

    ApacheSdkHttpClient(CloseableHttpAsyncClient delegate, PoolingAsyncClientConnectionManager cm) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.cm = Objects.requireNonNull(cm, "cm");
    }

    @Override
    public PoolingAsyncClientConnectionManager getAsyncClientConnectionManager() {
        return cm;
    }

    @Override
    public <T> Future<T> execute(
        AsyncRequestProducer requestProducer,
        AsyncResponseConsumer<T> responseConsumer,
        HandlerFactory<AsyncPushConsumer> pushHandlerFactory,
        HttpContext context,
        FutureCallback<T> callback
    ) {
        return delegate.execute(requestProducer, responseConsumer, pushHandlerFactory, context, callback);
    }
}
