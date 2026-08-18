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

package software.amazon.awssdk.core.internal.http.loader;

import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.FEATURE_METADATA;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.appendSpaceAndField;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.useragent.SdkUserAgentBuilder;
import software.amazon.awssdk.core.util.SystemUserAgent;

/**
 * Warms the sync or async HTTP clients on the classpath for CRaC warm-up.
 */
@SdkInternalApi
public interface HttpClientWarmer {

    String HEADER_USER_AGENT = "User-Agent";

    String WARM_UP_FEATURE_ID = "warmup";

    /**
     * Warms every HTTP client found on the classpath. Best-effort; never throws.
     */
    void warmAll();

    /**
     * Builds the {@code User-Agent} header value for warm-up requests: the system user agent (SDK version, Java version, OS,
     * etc.) plus the {@link #WARM_UP_FEATURE_ID} feature marker.
     */
    static String warmUpUserAgent() {
        StringBuilder uaString =
            new StringBuilder(SdkUserAgentBuilder.buildSystemUserAgentString(SystemUserAgent.getOrCreate()));
        appendSpaceAndField(uaString, FEATURE_METADATA, WARM_UP_FEATURE_ID);
        return uaString.toString();
    }
}
