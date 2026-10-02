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

package software.amazon.awssdk.benchmark.dynamodb.mock;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;

/**
 * Adapter over {@link MockAsyncHttpClient} that:
 * <ul>
 *   <li>stubs each execute with {@link Duration#ZERO} (no 50&nbsp;ms artificial delay),</li>
 *   <li>supplies a fresh response body stream every call (the underlying mock drains streams).</li>
 * </ul>
 * Completion still runs on {@link MockAsyncHttpClient}'s executor via {@code runAsync}.
 */
final class ReplayingMockAsyncHttpClient implements SdkAsyncHttpClient {

    private final MockAsyncHttpClient delegate = new MockAsyncHttpClient();
    private final byte[] successBody;

    ReplayingMockAsyncHttpClient(byte[] successBody) {
        this.successBody = successBody.clone();
    }

    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
        delegate.stubNextResponse(successResponse(), Duration.ZERO);
        return delegate.execute(request);
    }

    @Override
    public void close() {
        delegate.close();
    }

    private HttpExecuteResponse successResponse() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .responseBody(AbortableInputStream.create(new ByteArrayInputStream(successBody)))
                                  .build();
    }
}
