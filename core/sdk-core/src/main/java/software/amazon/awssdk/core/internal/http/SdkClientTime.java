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

package software.amazon.awssdk.core.internal.http;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkGlobalTime;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;

/**
 * Used for clock skew adjustment between the client JVM where the SDK is run, and the server side. This class mirrors
 * {@link SdkGlobalTime} but it's local to the current client and only accessed internally by {@link HttpClientDependencies}.
 */
@SdkInternalApi
public final class SdkClientTime {
    /**
     * Time offset may be mutated by {@link RequestPipeline} implementations if a clock skew is detected.
     */
    private volatile int timeOffset = SdkGlobalTime.getGlobalTimeOffset();

    /**
     * Gets the latest recorded time offset.
     */
    public int getTimeOffset() {
        return timeOffset;
    }

    /**
     * Sets the latest recorded time offset.
     */
    public void setTimeOffset(int timeOffset) {
        this.timeOffset = timeOffset;
        SdkGlobalTime.setGlobalTimeOffset(timeOffset);
    }
}
