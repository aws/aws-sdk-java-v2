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

import software.amazon.awssdk.endpoints.Endpoint;

public class Expect {
    private Endpoint endpoint;
    private String error;

    private Expect(Builder b) {
        this.endpoint = b.endpoint;
        this.error = b.error;
    }

    public Endpoint endpoint() {
        return endpoint;
    }

    public String error() {
        return error;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Endpoint endpoint;
        private String error;

        public Builder endpoint(Endpoint endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Expect build() {
            return new Expect(this);
        }
    }
}
