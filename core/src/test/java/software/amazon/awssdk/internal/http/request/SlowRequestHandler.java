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

package software.amazon.awssdk.internal.http.request;

import software.amazon.awssdk.Response;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Implementation of {@link RequestHandler} with configurable wait times
 */
public class SlowRequestHandler extends RequestHandler {

    private int beforeRequestWait;
    private int afterResponseWait;
    private int afterErrorWait;

    @Override
    public SdkHttpFullRequest beforeRequest(SdkHttpFullRequest request) {
        wait(beforeRequestWait);
        return request;
    }

    @Override
    public void afterResponse(SdkHttpFullRequest request, Response<?> response) {
        wait(afterResponseWait);
    }

    @Override
    public void afterError(SdkHttpFullRequest request, Response<?> response, Exception e) {
        wait(afterErrorWait);
    }

    private void wait(int secondsToWait) {
        if (secondsToWait > 0) {
            try {
                Thread.sleep(secondsToWait * 1000);
            } catch (InterruptedException e) {
                // Be a good citizen an re-interrupt the current thread
                Thread.currentThread().interrupt();
            }
        }
    }

    public SlowRequestHandler withBeforeRequestWaitInSeconds(int beforeRequestWait) {
        this.beforeRequestWait = beforeRequestWait;
        return this;
    }

    public SlowRequestHandler withAfterResponseWaitInSeconds(int afterResponseWait) {
        this.afterResponseWait = afterResponseWait;
        return this;
    }

    public SlowRequestHandler withAfterErrorWaitInSeconds(int afterErrorWait) {
        this.afterErrorWait = afterErrorWait;
        return this;
    }

}
