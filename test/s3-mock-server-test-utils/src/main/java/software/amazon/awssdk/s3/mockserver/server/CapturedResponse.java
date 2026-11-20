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

package software.amazon.awssdk.s3.mockserver.server;

import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.s3.mockserver.helpers.ByteArrays;
import software.amazon.awssdk.s3.mockserver.helpers.CaseInsensitiveMap;

public final class CapturedResponse implements HttpResponseHead {
    private final int statusCode;
    private final String statusText;
    private final CaseInsensitiveMap<List<String>> headers;
    private final byte[] body;
    private final byte[] decodedBody;
    private final long bodyTotalLength;

    public CapturedResponse(
            int statusCode,
            String statusText,
            CaseInsensitiveMap<List<String>> headers,
            byte[] body,
            byte[] decodedBody,
            long bodyTotalLength) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.headers = headers;
        this.body = body;
        this.decodedBody = decodedBody;
        this.bodyTotalLength = bodyTotalLength;
    }

    public int statusCode() {
        return statusCode;
    }

    public String statusText() {
        return statusText;
    }

    public CaseInsensitiveMap<List<String>> headers() {
        return headers;
    }

    public byte[] body() {
        return body;
    }

    public byte[] decodedBody() {
        return decodedBody;
    }

    public long bodyTotalLength() {
        return bodyTotalLength;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CapturedResponse that = (CapturedResponse) o;
        return statusCode == that.statusCode
               && Objects.equals(statusText, that.statusText)
               && Objects.equals(headers, that.headers)
               && body == that.body
               && decodedBody == that.decodedBody
               && bodyTotalLength == that.bodyTotalLength;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Integer.hashCode(statusCode);
        result = 31 * result + Objects.hashCode(statusText);
        result = 31 * result + Objects.hashCode(headers);
        result = 31 * result + Objects.hashCode(body);
        result = 31 * result + Objects.hashCode(decodedBody);
        result = 31 * result + Long.hashCode(bodyTotalLength);
        return result;
    }

    @Override
    public String toString() {
        return "CapturedResponse[statusCode=" + statusCode +
                ", statusText=" + statusText +
                ", headers=" + headers +
                ", body=" + ByteArrays.describe(body) +
                ", decodedBody=" + ByteArrays.describe(decodedBody) +
                ", bodyTotalLength=" + bodyTotalLength + "]";
    }
}
