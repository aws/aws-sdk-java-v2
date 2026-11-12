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

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.s3.mockserver.helpers.CaseInsensitiveMap;
import software.amazon.awssdk.s3.mockserver.helpers.CaseInsensitiveReadOnlyMap;
import software.amazon.awssdk.s3.mockserver.helpers.CaseInsensitiveTreeMap;

public final class Request implements HttpRequestHead {

    private final String method;
    private final String path;
    private final CaseInsensitiveMap<List<String>> headers;

    private Request(String method, String path, CaseInsensitiveMap<List<String>> headers) {
        this.method = method;
        this.path = path;
        this.headers = CaseInsensitiveReadOnlyMap.deepReadOnly(headers);
    }

    public static RequestBuilder builder() {
        return new RequestBuilder();
    }

    public RequestBuilder toBuilder() {
        return new RequestBuilder().method(this.method).path(this.path).headers(this.headers);
    }

    public String method() {
        return method;
    }

    public String path() {
        return path;
    }

    public CaseInsensitiveMap<List<String>> headers() {
        return headers;
    }

    public static class RequestBuilder {
        private String method;
        private String path;
        private CaseInsensitiveMap<List<String>> headers;

        RequestBuilder() {
        }

        public RequestBuilder method(String method) {
            this.method = method;
            return this;
        }

        public RequestBuilder path(String path) {
            this.path = path;
            return this;
        }

        public RequestBuilder headers(CaseInsensitiveMap<List<String>> headers) {
            this.headers = headers == null ? null : new CaseInsensitiveTreeMap<>(headers);
            return this;
        }

        public RequestBuilder addHeader(String name, String value) {
            if (this.headers == null) {
                this.headers = new CaseInsensitiveTreeMap<>();
            }
            this.headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
            return this;
        }

        public Request build() {
            return new Request(method, path, headers);
        }

        @Override
        public String toString() {
            return "Request.RequestBuilder(method=" + this.method + ", path=" + this.path + ", headers=" + this.headers + ")";
        }
    }
}
