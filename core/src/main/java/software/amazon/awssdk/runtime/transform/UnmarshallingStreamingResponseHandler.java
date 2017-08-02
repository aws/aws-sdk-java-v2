/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.runtime.transform;

import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.interceptor.ExecutionAttributes;
import software.amazon.awssdk.sync.StreamingResponseHandler;
import software.amazon.awssdk.utils.FunctionalUtils.UnsafeFunction;

/**
 * Delegates to a {@link software.amazon.awssdk.sync.StreamingResponseHandler} to handle processing a streamed response from a
 * service.
 *
 * @param <ResponseT> Type of unmarshalled POJO.
 * @param <ReturnT>   Return type of {@link software.amazon.awssdk.sync.StreamingResponseHandler}.
 */
public class UnmarshallingStreamingResponseHandler<ResponseT, ReturnT> implements HttpResponseHandler<ReturnT> {

    private final UnsafeFunction<HttpResponse, ResponseT> unmarshaller;
    private final StreamingResponseHandler<ResponseT, ReturnT> streamHandler;

    public UnmarshallingStreamingResponseHandler(StreamingResponseHandler<ResponseT, ReturnT> streamHandler,
                                                 UnsafeFunction<HttpResponse, ResponseT> unmarshaller) {
        this.unmarshaller = unmarshaller;
        this.streamHandler = streamHandler;
    }

    @Override
    public ReturnT handle(HttpResponse response, ExecutionAttributes executionAttributes) throws Exception {
        ResponseT unmarshalled = unmarshaller.apply(response);
        ReturnT toReturn = streamHandler.apply(unmarshalled, new AbortableInputStream(response.getContent(), response));
        response.getContent().close();
        return toReturn;
    }

    @Override
    public boolean needsConnectionLeftOpen() {
        return false;
    }
}
