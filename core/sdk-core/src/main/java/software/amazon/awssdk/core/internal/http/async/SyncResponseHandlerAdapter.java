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

package software.amazon.awssdk.core.internal.http.async;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.util.ThrowableUtils;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.http.async.SimpleSubscriber;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Adapts an {@link HttpResponseHandler} to the asynchronous {@link SdkHttpResponseHandler}. Buffers
 * all content into a {@link ByteArrayInputStream} then invokes the {@link HttpResponseHandler#handle}
 * method.
 *
 * @param <T> Type that the response handler produces.
 */
@SdkInternalApi
public final class SyncResponseHandlerAdapter<T> implements SdkHttpResponseHandler<T> {

    private final HttpResponseHandler<T> responseHandler;
    private ByteArrayOutputStream baos;
    private final ExecutionAttributes executionAttributes;
    private SdkHttpFullResponse.Builder httpResponse;
    private final Function<SdkHttpFullResponse, SdkHttpFullResponse> crc32Validator;

    public SyncResponseHandlerAdapter(HttpResponseHandler<T> responseHandler,
                                      Function<SdkHttpFullResponse, SdkHttpFullResponse> crc32Validator,
                                      ExecutionAttributes executionAttributes) {
        this.responseHandler = responseHandler;
        this.executionAttributes = executionAttributes;
        this.crc32Validator = crc32Validator;
    }

    @Override
    public void headersReceived(SdkHttpResponse response) {
        this.httpResponse = ((SdkHttpFullResponse) response).toBuilder();
    }

    @Override
    public void onStream(Publisher<ByteBuffer> publisher) {
        baos = new ByteArrayOutputStream();
        publisher.subscribe(new SimpleSubscriber(b -> {
            try {
                baos.write(BinaryUtils.copyBytesFrom(b));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }));
    }

    @Override
    public void exceptionOccurred(Throwable throwable) {
    }

    @Override
    public T complete() {
        try {
            // Once we've buffered all the content we can invoke the response handler
            if (baos != null) {
                // Ignore aborts - we already have all of the content.
                ByteArrayInputStream content = new ByteArrayInputStream(baos.toByteArray());
                AbortableInputStream abortableContent = AbortableInputStream.create(content);
                httpResponse.content(abortableContent);
            }
            return responseHandler.handle(crc32Validator.apply(httpResponse.build()), executionAttributes);
        } catch (Exception e) {
            throw ThrowableUtils.failure(e);
        }
    }
}
