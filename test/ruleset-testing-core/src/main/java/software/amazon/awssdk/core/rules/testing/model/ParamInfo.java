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

public class ParamInfo {
    private String builtIn;

    private ParamInfo(Builder b) {
        this.builtIn = b.builtIn;
    }

    public String builtIn() {
        return builtIn;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String builtIn;

        public Builder builtIn(String builtIn) {
            this.builtIn = builtIn;
            return this;
        }

        public ParamInfo build() {
            return new ParamInfo(this);
        }
    }
}
