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

package software.amazon.awssdk.retries.internal.ratelimiter;

import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public interface RateLimiterClock {
    /**
     * Returns the current time in seconds, and should include sub second resolution. This class needs not to be related to the
     * actual wall clock-time or system as it's only used to measure elapsed time.
     *
     * <p>For instance, it the current time is <pre>PT2M8.067S</pre>, i.e., 2 minutes with 8 seconds and 67 milliseconds, this
     * method will return <pre>128.067</pre>.
     *
     * @return the current time in seconds, and should include sub second resolution
     */
    double time();
}
