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

package software.amazon.awssdk.core.internal.util;

import org.reactivestreams.Publisher;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;

public class AsyncResponseHandlerTestUtils {
    private AsyncResponseHandlerTestUtils() {
    }

    public static SdkHttpResponseHandler noOpResponseHandler() {
        return new SdkHttpResponseHandler() {

            @Override
            public void headersReceived(SdkHttpResponse response) {

            }

            @Override
            public void exceptionOccurred(Throwable throwable) {

            }

            @Override
            public Object complete() {
                return null;
            }

            @Override
            public void onStream(Publisher publisher) {

            }
        };
    }

    public static SdkHttpResponseHandler<SdkServiceException> superSlowResponseHandler(long sleepInMills) {

        return new SdkHttpResponseHandler<SdkServiceException>() {
            @Override
            public void headersReceived(SdkHttpResponse response) {

            }

            @Override
            public void onStream(Publisher publisher) {

            }

            @Override
            public void exceptionOccurred(Throwable throwable) {

            }

            @Override
            public SdkServiceException complete() {
                try {
                    Thread.sleep(sleepInMills);
                } catch (InterruptedException e) {
                    // ignore
                }
                return null;
            }
        };
    }
}
