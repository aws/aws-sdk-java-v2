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

package software.amazon.awssdk.http.crt.internal.request;

import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.utils.Validate;

/**
 * Implements the CrtHttpStreamHandler API and converts CRT callbacks into calls to SDK AsyncExecuteRequest methods
 */
@SdkInternalApi
final class CrtRequestBodyAdapter implements HttpRequestBodyStream {
    private final int windowSize;
    private final CrtRequestBodySubscriber requestBodySubscriber;

    CrtRequestBodyAdapter(SdkHttpContentPublisher requestPublisher, int windowSize) {
        this.windowSize = Validate.isPositive(windowSize, "windowSize is <= 0");
        this.requestBodySubscriber = new CrtRequestBodySubscriber(windowSize);
        requestPublisher.subscribe(requestBodySubscriber);
    }

    @Override
    public boolean sendRequestBody(ByteBuffer bodyBytesOut) {
        return requestBodySubscriber.transferRequestBody(bodyBytesOut);
    }
}
