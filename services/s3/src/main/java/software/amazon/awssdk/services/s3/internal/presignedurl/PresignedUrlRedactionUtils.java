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

package software.amazon.awssdk.services.s3.internal.presignedurl;

import java.net.URL;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Renders a presigned URL for logs and exception messages.
 */
@SdkInternalApi
public final class PresignedUrlRedactionUtils {
    private static final String SENSITIVE_DATA_REDACTED = "*** Sensitive Data Redacted ***";

    private PresignedUrlRedactionUtils() {
    }

    /**
     * Renders only the non-secret components of a presigned URL: protocol, host, port and path. The query string of a
     * presigned URL carries the signature, the credential scope and (for session credentials) the security token, so it
     * is replaced with a placeholder. The user-info and fragment components are dropped.
     *
     * @param url the presigned URL, may be null
     * @return the rendered URL, or null if {@code url} is null
     */
    public static String redactQueryString(URL url) {
        if (url == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        if (url.getProtocol() != null) {
            result.append(url.getProtocol()).append("://");
        }
        if (url.getHost() != null) {
            result.append(url.getHost());
        }
        if (url.getPort() != -1) {
            result.append(':').append(url.getPort());
        }
        if (url.getPath() != null) {
            result.append(url.getPath());
        }
        if (url.getQuery() != null) {
            result.append('?').append(SENSITIVE_DATA_REDACTED);
        }
        return result.toString();
    }
}
