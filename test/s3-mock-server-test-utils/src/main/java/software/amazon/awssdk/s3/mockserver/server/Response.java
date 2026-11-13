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

import io.netty.handler.codec.http.HttpResponseStatus;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.s3.mockserver.helpers.CaseInsensitiveMap;
import software.amazon.awssdk.s3.mockserver.helpers.CaseInsensitiveReadOnlyMap;
import software.amazon.awssdk.s3.mockserver.helpers.CaseInsensitiveTreeMap;
import software.amazon.awssdk.s3.mockserver.helpers.Immutable;

public final class Response implements HttpResponseHead {

    private final int statusCode;
    private final String statusText;
    private final CaseInsensitiveMap<List<String>> headers;
    private final byte[] body;

    private Response(int statusCode, String statusText, CaseInsensitiveMap<List<String>> headers, byte[] body) {
        this.statusCode = statusCode;
        this.statusText = statusText != null ? statusText : reasonPhrase(statusCode);
        this.headers = CaseInsensitiveReadOnlyMap.deepReadOnly(headers);
        this.body = body;
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

    public static ResponseBuilder builder() {
        return new ResponseBuilder();
    }

    public ResponseBuilder toBuilder() {
        return new ResponseBuilder().statusCode(statusCode).statusText(statusText).headers(headers).body(body);
    }

    public static class ResponseBuilder {
        private int statusCode = 200;
        private String statusText;
        private CaseInsensitiveMap<List<String>> headers;
        private byte[] body;

        ResponseBuilder() {
        }

        public ResponseBuilder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public ResponseBuilder statusText(String statusText) {
            this.statusText = statusText;
            return this;
        }

        public Response build() {
            return new Response(statusCode, statusText, headers, body);
        }

        @Override
        public String toString() {
            return "Response.ResponseBuilder(statusCode=" + statusCode + ", statusText=" + statusText + ", headers=" + headers
                    + ", body=" + Arrays.toString(body) + ")";
        }

        public ResponseBuilder headers(CaseInsensitiveMap<List<String>> headers) {
            this.headers = headers == null ? null : new CaseInsensitiveTreeMap<>(headers);
            return this;
        }

        public ResponseBuilder status(int statusCode) {
            this.statusCode = statusCode;
            this.statusText = reasonPhrase(statusCode);
            return this;
        }

        public ResponseBuilder addHeader(String name, String value) {
            if (this.headers == null) {
                this.headers = new CaseInsensitiveTreeMap<>();
            }
            this.headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
            return this;
        }

        public ResponseBuilder header(String name, String value) {
            if (this.headers == null) {
                this.headers = new CaseInsensitiveTreeMap<>();
            }
            this.headers.put(name, new ArrayList<>(Immutable.listOf(value)));
            return this;
        }

        public ResponseBuilder removeHeader(String name) {
            if (this.headers != null) {
                this.headers.remove(name);
            }
            return this;
        }

        public ResponseBuilder body(byte[] body) {
            this.body = body;
            return this;
        }

        public ResponseBuilder body(String body) {
            return body(body.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static String reasonPhrase(int code) {
        return HttpResponseStatus.valueOf(code).reasonPhrase();
    }
}
