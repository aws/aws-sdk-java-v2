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

package software.amazon.awssdk.transfer.s3.internal;

import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZ_ID_2_HEADER;

import com.amazonaws.s3.OperationHandler;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkStandardLogger;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.http.HttpStatusFamily;
import software.amazon.awssdk.http.SdkHttpResponse;

@SdkInternalApi
public final class ResponseHeadersHandler implements OperationHandler {
    private static final String REQUEST_ID = "x-amz-request-id";
    private final SdkHttpResponse.Builder responseBuilder;
    private final CompletableFuture<SdkHttpResponse> responseFuture;

    public ResponseHeadersHandler() {
        responseBuilder = SdkHttpResponse.builder();
        responseFuture = new CompletableFuture<>();
    }

    @Override
    public void onResponseHeaders(int statusCode, HttpHeader[] headers) {
        if (HttpStatusFamily.of(statusCode) == HttpStatusFamily.SUCCESSFUL) {
            SdkStandardLogger.REQUEST_LOGGER.debug(() -> "Received successful response: " + statusCode);
        } else {
            SdkStandardLogger.REQUEST_LOGGER.debug(() -> "Received error response: " + statusCode);
        }

        for (HttpHeader header : headers) {
            responseBuilder.appendHeader(header.getName(), header.getValue());
        }
        responseBuilder.statusCode(statusCode);
        SdkStandardLogger.REQUEST_ID_LOGGER.debug(() -> REQUEST_ID + " : " +
                                                        responseBuilder.firstMatchingHeader(REQUEST_ID)
                                                                       .orElse("not available"));
        SdkStandardLogger.REQUEST_ID_LOGGER.debug(() -> X_AMZ_ID_2_HEADER + " : " +
                                                        responseBuilder.firstMatchingHeader(X_AMZ_ID_2_HEADER)
                                                                       .orElse("not available"));
        responseFuture.complete(responseBuilder.build());
    }

    public CompletableFuture<SdkHttpResponse> sdkHttpResponseFuture() {
        return responseFuture;
    }
}
