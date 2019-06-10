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

import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Flow;
import org.reactivestreams.FlowAdapters;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.utils.AttributeMap;

@SdkInternalApi
final class JavaHttpRequestFactory {

    private Duration responseTimeout;

    JavaHttpRequestFactory(AttributeMap serviceDefaultsMap) {
        responseTimeout = serviceDefaultsMap.get(SdkHttpConfigurationOption.RESPONSE_TIMEOUT);
    }

    private String getRequestMethod(SdkHttpMethod sdkhttpmethod) {
        return sdkhttpmethod.name();
    }

    private HttpRequest.BodyPublisher createBodyPublisher(SdkHttpContentPublisher sdkHttpContentPublisher) {
        // TODO: Address the issue of actual content is longer than the content length
        Optional<Long> contentlength = sdkHttpContentPublisher.contentLength();
        Flow.Publisher<ByteBuffer> flowPublisher = FlowAdapters.toFlowPublisher(sdkHttpContentPublisher);
        if (contentlength.isEmpty() || contentlength.get() == 0) {
            HttpRequest.BodyPublisher bodyPublisher = BodyPublishers.noBody();
            return bodyPublisher;
        } else {
            return contentlength.map(aLong -> BodyPublishers.fromPublisher(flowPublisher,
                aLong)).orElseGet(() -> BodyPublishers.fromPublisher(flowPublisher));
        }
    }


    /**
     * Creates the Java 11 HttpRequest with HttpRequest.Builder according to
     * the configurations in the AsyncExecuteRequest
     * @return HttpRequest object
     */
    HttpRequest createJavaHttpRequest(AsyncExecuteRequest asyncExecuteRequest) {
        SdkHttpRequest request = asyncExecuteRequest.request();
        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder();
        httpRequestBuilder.uri(request.getUri());
        httpRequestBuilder.method(getRequestMethod(request.method()),
                createBodyPublisher(asyncExecuteRequest.requestContentPublisher()));
        // TODO: Check the restricted types of headers
        // In Jdk 11, these headers filtered below are restricted and not allowed to be customized
        request.headers().forEach((name, values) -> {
            if (!name.equals("Host") && !name.equals("Content-Length") && !name.equals("Expect")) {
                httpRequestBuilder.setHeader(name, String.join(",", values));
            }
        });
        httpRequestBuilder.timeout(responseTimeout);

        return httpRequestBuilder.build();
    }
}
