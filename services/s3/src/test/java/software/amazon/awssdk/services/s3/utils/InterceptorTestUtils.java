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

package software.amazon.awssdk.services.s3.utils;

import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.EmptyPublisher;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.StringInputStream;

public final class InterceptorTestUtils {

    private InterceptorTestUtils() {
    }

    public static SdkHttpFullRequest sdkHttpFullRequest() {
        return SdkHttpFullRequest.builder()
                                 .uri(URI.create("http://localhost:8080"))
                                 .method(SdkHttpMethod.GET)
                                 .build();
    }

    public static SdkHttpRequest sdkHttpRequest(URI customUri) {
        return SdkHttpFullRequest.builder()
                                 .protocol(customUri.getScheme())
                                 .host(customUri.getHost())
                                 .port(customUri.getPort())
                                 .method(SdkHttpMethod.GET)
                                 .encodedPath(customUri.getPath())
                                 .build();
    }

    public static Context.ModifyHttpResponse modifyHttpResponse(SdkRequest request, SdkHttpResponse sdkHttpResponse) {

        Publisher<ByteBuffer> publisher = new EmptyPublisher<>();
        InputStream responseBody = new StringInputStream("helloworld");

        return new Context.ModifyResponse() {
            @Override
            public SdkResponse response() {
                return null;
            }

            @Override
            public SdkHttpResponse httpResponse() {
                return sdkHttpResponse;
            }

            @Override
            public Optional<Publisher<ByteBuffer>> responsePublisher() {
                return Optional.of(publisher);
            }

            @Override
            public Optional<InputStream> responseBody() {
                return Optional.of(responseBody);
            }

            @Override
            public SdkHttpRequest httpRequest() {
                return SdkHttpRequest.builder().build();
            }

            @Override
            public Optional<RequestBody> requestBody() {
                return Optional.empty();
            }

            @Override
            public Optional<AsyncRequestBody> asyncRequestBody() {
                return Optional.empty();
            }

            @Override
            public SdkRequest request() {
                return request;
            }
        };
    }

    public static Context.ModifyHttpRequest modifyHttpRequestContext(SdkRequest request) {
        return modifyHttpRequestContext(request, sdkHttpFullRequest());
    }

    public static Context.ModifyHttpRequest modifyHttpRequestContext(SdkRequest request, SdkHttpRequest sdkHttpRequest) {
        Optional<RequestBody> requestBody = Optional.of(RequestBody.fromString("helloworld"));
        Optional<AsyncRequestBody> asyncRequestBody = Optional.of(AsyncRequestBody.fromString("helloworld"));

        return new Context.ModifyHttpRequest() {
            @Override
            public SdkHttpRequest httpRequest() {
                return sdkHttpRequest;
            }

            @Override
            public Optional<RequestBody> requestBody() {
                return requestBody;
            }

            @Override
            public Optional<AsyncRequestBody> asyncRequestBody() {
                return asyncRequestBody;
            }

            @Override
            public SdkRequest request() {
                return request;
            }
        };
    }

    public static Context.ModifyResponse modifyResponseContext(SdkRequest request, SdkResponse response, SdkHttpResponse sdkHttpResponse) {
        return new Context.ModifyResponse() {
            @Override
            public SdkResponse response() {
                return response;
            }

            @Override
            public SdkHttpResponse httpResponse() {
                return sdkHttpResponse;
            }

            @Override
            public Optional<Publisher<ByteBuffer>> responsePublisher() {
                return Optional.empty();
            }

            @Override
            public Optional<InputStream> responseBody() {
                return Optional.empty();
            }

            @Override
            public SdkHttpRequest httpRequest() {
                return null;
            }

            @Override
            public Optional<RequestBody> requestBody() {
                return Optional.empty();
            }

            @Override
            public Optional<AsyncRequestBody> asyncRequestBody() {
                return Optional.empty();
            }

            @Override
            public SdkRequest request() {
                return request;
            }
        };
    }

    public static Context.AfterUnmarshalling afterUnmarshallingContext(SdkRequest request, SdkHttpRequest sdkHttpRequest, SdkResponse response, SdkHttpResponse sdkHttpResponse) {
        return new Context.AfterUnmarshalling() {
            @Override
            public SdkResponse response() {
                return response;
            }

            @Override
            public SdkHttpResponse httpResponse() {
                return sdkHttpResponse;
            }

            @Override
            public Optional<Publisher<ByteBuffer>> responsePublisher() {
                return Optional.empty();
            }

            @Override
            public Optional<InputStream> responseBody() {
                return Optional.empty();
            }

            @Override
            public SdkHttpRequest httpRequest() {
                return sdkHttpRequest;
            }

            @Override
            public Optional<RequestBody> requestBody() {
                return Optional.empty();
            }

            @Override
            public Optional<AsyncRequestBody> asyncRequestBody() {
                return Optional.empty();
            }

            @Override
            public SdkRequest request() {
                return request;
            }
        };
    }

    public static Context.ModifyHttpResponse modifyHttpResponseContent(SdkRequest request, SdkHttpResponse sdkHttpResponse) {
        InputStream stream = new StringInputStream("hello world");

        return new Context.ModifyResponse() {
            @Override
            public SdkResponse response() {
                return null;
            }

            @Override
            public SdkHttpResponse httpResponse() {
                return sdkHttpResponse;
            }

            @Override
            public Optional<Publisher<ByteBuffer>> responsePublisher() {
                return Optional.empty();
            }

            @Override
            public Optional<InputStream> responseBody() {
                return Optional.of(stream);
            }

            @Override
            public SdkHttpRequest httpRequest() {
                return null;
            }

            @Override
            public Optional<RequestBody> requestBody() {
                return Optional.empty();
            }

            @Override
            public Optional<AsyncRequestBody> asyncRequestBody() {
                return Optional.empty();
            }

            @Override
            public SdkRequest request() {
                return request;
            }
        };
    }
}
