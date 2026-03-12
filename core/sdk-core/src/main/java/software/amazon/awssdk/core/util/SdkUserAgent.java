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

package software.amazon.awssdk.core.util;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * Utility class for accessing AWS SDK versioning information. Deprecated in favor of {@link SystemUserAgent}.
 */
@ThreadSafe
@SdkProtectedApi
@Deprecated
public final class SdkUserAgent {

    private static volatile SdkUserAgent instance;

    private String userAgent;

    private SdkUserAgent() {
        initializeUserAgent();
    }

    public static SdkUserAgent create() {
        if (instance == null) {
            synchronized (SdkUserAgent.class) {
                if (instance == null) {
                    instance = new SdkUserAgent();
                }
            }
        }

        return instance;
    }

    /**
     * @return Returns the User Agent string to be used when communicating with
     *     the AWS services.  The User Agent encapsulates SDK, Java, OS and
     *     region information.
     */
    public String userAgent() {
        return userAgent;
    }

    /**
     * Initializes the user agent string by loading a template from
     * {@code InternalConfig} and filling in the detected version/platform
     * info.
     */
    private void initializeUserAgent() {
        userAgent = getUserAgent();
    }

    @SdkTestInternalApi
    String getUserAgent() {
        return SystemUserAgent.getOrCreate().userAgentString();
    }
}
