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

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import software.amazon.awssdk.s3.wiretap.helpers.CaseInsensitiveMap;
import software.amazon.awssdk.s3.wiretap.helpers.CaseInsensitiveReadOnlyMap;
import software.amazon.awssdk.s3.wiretap.helpers.CaseInsensitiveTreeMap;

@Getter
@Accessors(fluent = true)
public final class Request implements HttpRequestHead {

    private final String method;
    private final String path;
    private final CaseInsensitiveMap<List<String>> headers;

    @Builder(toBuilder = true)
    private Request(String method, String path, CaseInsensitiveMap<List<String>> headers) {
        this.method = method;
        this.path = path;
        this.headers = CaseInsensitiveReadOnlyMap.deepReadOnly(headers);
    }

    public static class RequestBuilder {
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
    }
}
