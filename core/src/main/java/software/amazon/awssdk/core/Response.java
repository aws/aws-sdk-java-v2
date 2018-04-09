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

package software.amazon.awssdk.core;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.http.HttpResponse;

/**
 * Response wrapper to provide access to not only the original AWS response
 * but also the associated http response.
 *
 * @param <T> the underlying AWS response type.
 */
public final class Response<T> {
    private final boolean isSuccess;
    private final T response;
    private final SdkException exception;
    private final HttpResponse httpResponse;

    /**
     * @deprecated Use {@link #Response(boolean, Object, SdkException, HttpResponse)}
     */
    @Deprecated
    public Response(T response, HttpResponse httpResponse) {
        this(true, response, null, httpResponse);
    }

    private Response(boolean isSuccess, T response, SdkException exception, HttpResponse httpResponse) {
        this.isSuccess = isSuccess;
        this.response = response;
        this.exception = exception;
        this.httpResponse = httpResponse;
    }

    public T getAwsResponse() {
        return response;
    }

    public SdkException getException() {
        return exception;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public boolean isSuccess() {
        return isSuccess;
    }


    public boolean isFailure() {
        return !isSuccess;
    }

    public static <T> Response<T> fromSuccess(T response, HttpResponse httpResponse) {
        return new Response<>(true, response, null, httpResponse);
    }

    public static <T> Response<T> fromFailure(SdkException exception, HttpResponse httpResponse) {
        return new Response<>(false, null, exception, httpResponse);
    }
}
