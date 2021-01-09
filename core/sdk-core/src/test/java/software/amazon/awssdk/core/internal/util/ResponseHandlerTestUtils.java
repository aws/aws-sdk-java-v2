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

package software.amazon.awssdk.core.internal.util;

import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.internal.http.CombinedResponseHandler;

public final class ResponseHandlerTestUtils {
    private ResponseHandlerTestUtils() {
    }

    public static <T> HttpResponseHandler<T> noOpSyncResponseHandler() {
        return (response, executionAttributes) -> null;
    }

    public static HttpResponseHandler<SdkServiceException> superSlowResponseHandler(long sleepInMills) {
        return (response, executionAttributes) -> {
            Thread.sleep(sleepInMills);
            return null;
        };
    }

    public static <T> HttpResponseHandler<Response<T>> combinedSyncResponseHandler(
        HttpResponseHandler<T> successResponseHandler,
        HttpResponseHandler<? extends SdkException> failureResponseHandler) {

        return new CombinedResponseHandler<>(
            successResponseHandler == null ? noOpSyncResponseHandler() : successResponseHandler,
            failureResponseHandler == null ? noOpSyncResponseHandler() : failureResponseHandler);
    }
}
