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

package software.amazon.awssdk.http;

import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * The set of HTTP status families defined by the standard. A code can be converted to its family with {@link #of(int)}.
 */
@SdkProtectedApi
public enum HttpStatusFamily {
    /**
     * 1xx response family.
     */
    INFORMATIONAL,

    /**
     * 2xx response family.
     */
    SUCCESSFUL,

    /**
     * 3xx response family.
     */
    REDIRECTION,

    /**
     * 4xx response family.
     */
    CLIENT_ERROR,

    /**
     * 5xx response family.
     */
    SERVER_ERROR,

    /**
     * The family for any status code outside of the range 100-599.
     */
    OTHER;

    /**
     * Retrieve the HTTP status family for the given HTTP status code.
     */
    public static HttpStatusFamily of(int httpStatusCode) {
        switch (httpStatusCode / 100) {
            case 1: return INFORMATIONAL;
            case 2: return SUCCESSFUL;
            case 3: return REDIRECTION;
            case 4: return CLIENT_ERROR;
            case 5: return SERVER_ERROR;
            default: return OTHER;
        }
    }

    /**
     * Determine whether this HTTP status family is in the list of provided families.
     *
     * @param families The list of families to check against this family.
     * @return True if any of the families in the list match this one.
     */
    public boolean isOneOf(HttpStatusFamily... families) {
        return families != null && Stream.of(families).anyMatch(family -> family == this);
    }
}
