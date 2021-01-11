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

package software.amazon.awssdk.protocols.json.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.protocol.MarshallLocation;

@SdkInternalApi
public final class MarshallerUtil {
    private MarshallerUtil() {
    }

    /**
     * @return true if the location is in the URI, false otherwise.
     */
    public static boolean locationInUri(MarshallLocation location) {
        switch (location) {
            case PATH:
            case QUERY_PARAM:
                return true;
            default:
                return false;
        }
    }
}
