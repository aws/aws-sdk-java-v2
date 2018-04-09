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

package software.amazon.awssdk.http.async;

import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.http.ConfigurationProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.utils.SdkAutoCloseable;

public interface SdkAsyncHttpClient extends SdkAutoCloseable, ConfigurationProvider {

    /**
     * Create an {@link AbortableRunnable} that can be used to execute the HTTP request.
     *
     * @param request         HTTP request (without content).
     * @param context         Request context containing additional dependencies.
     * @param requestProvider Representation of an HTTP requestProvider.
     * @param handler         The handler that will be called when data is received.
     * @return Task that can execute an HTTP requestProvider and can be aborted.
     */
    @ReviewBeforeRelease("Should we wrap this in a container for more flexibility?")
    AbortableRunnable prepareRequest(SdkHttpRequest request,
                                     SdkRequestContext context,
                                     SdkHttpRequestProvider requestProvider,
                                     SdkHttpResponseHandler handler);
}
