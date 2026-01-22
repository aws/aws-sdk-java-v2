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

import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.internal.useragent.DefaultSystemUserAgent;

/**
 * Common system level user agent properties that can either be accessed as a string or as individual values.
 * The former is useful when making generic calls, for instance to local endpoints when resolving identity, while
 * the latter is when incorporating this information into a user agent header in an SDK request.
 */
@ThreadSafe
@SdkProtectedApi
public interface SystemUserAgent {

    static SystemUserAgent getOrCreate() {
        return DefaultSystemUserAgent.getOrCreate();
    }

    /**
     * A generic user agent string to be used when communicating with backend services.
     * This string contains Java, OS and region information but does not contain client and request
     * specific values.
     */
    String userAgentString();

    String sdkVersion();

    String osMetadata();

    String langMetadata();

    String envMetadata();

    String vmMetadata();

    String vendorMetadata();

    Optional<String> languageTagMetadata();

    List<String> additionalJvmLanguages();
}
