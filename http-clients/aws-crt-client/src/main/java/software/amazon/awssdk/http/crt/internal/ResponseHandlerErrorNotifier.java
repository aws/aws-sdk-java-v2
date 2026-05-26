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

package software.amazon.awssdk.http.crt.internal;

import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.utils.Logger;

/**
 * Coordinates at-most-once delivery of {@link SdkAsyncHttpResponseHandler#onError(Throwable)} across
 * the multiple paths (response adapter, request body subscriber, executor failure paths) that may
 * all attempt to notify the same handler.
 */
@SdkInternalApi
public final class ResponseHandlerErrorNotifier {

    private static final Logger log = Logger.loggerFor(ResponseHandlerErrorNotifier.class);

    private final SdkAsyncHttpResponseHandler responseHandler;
    private final AtomicBoolean notified = new AtomicBoolean(false);

    public ResponseHandlerErrorNotifier(SdkAsyncHttpResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }

    /**
     * Atomically delivers {@code onError(t)} to the response handler at most once. Returns
     * {@code true} if this caller delivered the notification, {@code false} if another caller
     * already did. Exceptions thrown by the handler are caught and logged.
     */
    public boolean tryNotifyError(Throwable t) {
        if (!notified.compareAndSet(false, true)) {
            return false;
        }
        try {
            responseHandler.onError(t);
        } catch (Exception e) {
            log.error(() -> "SdkAsyncHttpResponseHandler " + responseHandler + " threw an exception in onError. It will be "
                            + "ignored.", e);
        }
        return true;
    }
}
