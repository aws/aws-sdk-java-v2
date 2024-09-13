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

package software.amazon.awssdk.core.internal.useragent;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Represents AWS SDK user agent client values.
 */
@NotThreadSafe
@SdkProtectedApi
public final class SdkClientUserAgentProperties {

    private final Map<String, String> attributes;

    public SdkClientUserAgentProperties() {
        this.attributes = new HashMap<>(32);
    }

    public String getAttribute(String attribute) {
        return attributes.get(attribute);
    }

    public void putAttribute(String attribute, String value) {
        attributes.put(attribute, value);
    }
}
