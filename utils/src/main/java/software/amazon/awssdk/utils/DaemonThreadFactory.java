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

package software.amazon.awssdk.utils;

import java.util.concurrent.ThreadFactory;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * An decorator for {@link ThreadFactory} that sets all threads it creates to be daemon threads.
 */
@SdkProtectedApi
public class DaemonThreadFactory implements ThreadFactory {
    private final ThreadFactory delegate;

    public DaemonThreadFactory(ThreadFactory delegate) {
        this.delegate = Validate.notNull(delegate, "delegate must not be null");
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = delegate.newThread(runnable);
        thread.setDaemon(true);
        return thread;
    }
}
