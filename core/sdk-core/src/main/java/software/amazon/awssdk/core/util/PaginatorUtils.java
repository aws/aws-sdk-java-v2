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

import java.util.Collection;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public final class PaginatorUtils {

    private PaginatorUtils() {
    }


    /**
     * Checks if the output token is available.
     *
     * @param outputToken the output token to check
     * @param <T> the type of the output token
     * @return true if the output token is non-null or non-empty if the output token is a String or map or Collection type
     */
    public static <T> boolean isOutputTokenAvailable(T outputToken) {
        if (outputToken == null) {
            return false;
        }

        if (outputToken instanceof String) {
            return !((String) outputToken).isEmpty();
        }

        if (outputToken instanceof Map) {
            return !((Map) outputToken).isEmpty();
        }

        if (outputToken instanceof Collection) {
            return !((Collection) outputToken).isEmpty();
        }

        return true;
    }
}
