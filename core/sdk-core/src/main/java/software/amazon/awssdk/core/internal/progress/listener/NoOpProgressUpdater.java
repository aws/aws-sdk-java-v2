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

package software.amazon.awssdk.core.internal.progress.listener;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.http.SdkHttpRequest;

@SdkInternalApi
public class NoOpProgressUpdater implements ProgressUpdater {

    @Override
    public void requestPrepared(SdkHttpRequest httpRequest) {
    }

    @Override
    public void responseHeaderReceived() {
    }

    @Override
    public void executionSuccess(SdkResponse response) {
    }

    @Override
    public void executionFailure(Throwable t) {
    }

    @Override
    public void attemptFailure(Throwable t) {
    }
}

