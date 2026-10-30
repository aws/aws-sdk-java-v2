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

package software.amazon.awssdk.s3.wiretap.server;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.s3.wiretap.helpers.CaseInsensitiveMap;

public record CapturedRequest(
    String method,
    String path,
    Map<String, List<String>> queryParameters,
    CaseInsensitiveMap<List<String>> headers,
    byte[] rawBody,
    byte[] decodedBody,
    long fullBodyLength,
    byte[] fullBodyDigest,
    CaseInsensitiveMap<List<String>> trailingHeaders,
    String connectHost
) implements HttpRequestHead {
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        String query = "";
        if (queryParameters != null && !queryParameters.isEmpty()) {
            StringBuilder qs = new StringBuilder();
            for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
                for (String value : entry.getValue()) {
                    if (!qs.isEmpty()) {
                        qs.append('&');
                    }
                    qs.append(entry.getKey()).append('=').append(value);
                }
            }
            query = "?" + qs;
        }
        sb.append(method).append(' ').append(path).append(query).append(" HTTP/1.1\r\n");

        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                for (String value : entry.getValue()) {
                    sb.append(entry.getKey()).append(": ").append(value).append("\r\n");
                }
            }
        }
        sb.append("\r\n");

        if (rawBody != null && rawBody.length > 0) {
            for (byte b : rawBody) {
                if (b >= 0x20 && b <= 0x7E) {
                    sb.append((char) b);
                } else if (b == '\r') {
                    sb.append("\\r");
                } else if (b == '\n') {
                    sb.append("\\n\n");
                } else if (b == '\t') {
                    sb.append("\\t");
                } else {
                    sb.append(String.format("[0x%02x]", b & 0xFF));
                }
            }
        }

        return sb.toString();
    }

    public String queryParameter(String name) {
        List<String> v = queryParameters.get(name);
        if (v == null || v.isEmpty()) {
            throw new IllegalArgumentException("No query parameter named " + name);
        }
        if (v.size() > 1) {
            throw new IllegalArgumentException("Multiple query parameters named " + name + ": " + v);
        }
        return v.getFirst();
    }
}
