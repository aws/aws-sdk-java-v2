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

package software.amazon.awssdk.core.rules.testing.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Endpoint {
    private URI url;
    private Map<String, List<String>> headers;

    private Endpoint(Builder b) {
        this.url = b.url;
        this.headers = b.headers;
    }

    public URI url() {
        return url;
    }

    public Map<String, List<String>> headers() {
        return headers;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private URI url;
        private Map<String, List<String>> headers = new HashMap<>();

        public Builder url(URI url) {
            this.url = url;
            return this;
        }

        public Builder putHeader(String name, String value) {
            List<String> values = headers.computeIfAbsent(name, n -> new ArrayList<>());
            values.add(value);
            return this;
        }

        public Endpoint build() {
            return new Endpoint(this);
        }
    }
}
