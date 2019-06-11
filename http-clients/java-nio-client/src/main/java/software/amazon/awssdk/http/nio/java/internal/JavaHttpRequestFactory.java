/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.java.internal;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.Flow;
import org.reactivestreams.FlowAdapters;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;

@SdkInternalApi
final class JavaHttpRequestFactory {

    private final AsyncExecuteRequest asyncExecuteRequest;
    private final SdkHttpRequest request;

    JavaHttpRequestFactory(AsyncExecuteRequest asyncExecuteRequest) {
        this.asyncExecuteRequest = asyncExecuteRequest;
        this.request = asyncExecuteRequest.request();
    }

    /**
     * @return The AsyncExecuteRequest.
     */
    public AsyncExecuteRequest getAsyncExecuteRequest() {
        return asyncExecuteRequest;
    }

    /**
     * @return The SdkHttpRequest
     */
    public SdkHttpRequest getSdkHttpRequest() {
        return request;
    }


    private String getRequestMethod(SdkHttpMethod sdkhttpmethod) {
        return sdkhttpmethod.name();
    }

    private HttpRequest.BodyPublisher createBodyPublisher(SdkHttpContentPublisher sdkHttpContentPublisher) {
        Optional<Long> contentLength = sdkHttpContentPublisher.contentLength();
        Flow.Publisher<ByteBuffer> flowPublisher = FlowAdapters.toFlowPublisher(sdkHttpContentPublisher);
        return contentLength.map(aLong -> BodyPublishers.fromPublisher(flowPublisher,
                aLong)).orElseGet(() -> BodyPublishers.fromPublisher(flowPublisher));
    }


    /**
     * Creates the Java 11 HttpRequest with HttpRequest.Builder according to
     * the configurations in the AsyncExecuteRequest
     * @return HttpRequest object
     */
    HttpRequest createJavaHttpRequest() {
        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder();
        httpRequestBuilder.uri(request.getUri());
        httpRequestBuilder.version(HttpClient.Version.valueOf(request.protocol()));
        httpRequestBuilder.method(getRequestMethod(request.method()),
                createBodyPublisher(asyncExecuteRequest.requestContentPublisher()));
        request.headers().forEach((name, values) -> httpRequestBuilder.header(name, String.join(",", values)));

        return httpRequestBuilder.build();
    }
}
