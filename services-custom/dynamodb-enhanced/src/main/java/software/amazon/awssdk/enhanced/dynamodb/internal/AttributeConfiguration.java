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

package software.amazon.awssdk.enhanced.dynamodb.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Internal configuration for attribute
 */
@SdkInternalApi
public final class AttributeConfiguration {
    private final boolean preserveEmptyObject;
    private final boolean ignoreNulls;

    public AttributeConfiguration(Builder builder) {
        this.preserveEmptyObject = builder.preserveEmptyObject;
        this.ignoreNulls = builder.ignoreNulls;
    }

    public boolean preserveEmptyObject() {
        return preserveEmptyObject;
    }

    public boolean ignoreNulls() {
        return ignoreNulls;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean preserveEmptyObject;
        private boolean ignoreNulls;

        private Builder() {
        }

        public Builder preserveEmptyObject(boolean preserveEmptyObject) {
            this.preserveEmptyObject = preserveEmptyObject;
            return this;
        }

        public Builder ignoreNulls(boolean ignoreNulls) {
            this.ignoreNulls = ignoreNulls;
            return this;
        }

        public AttributeConfiguration build() {
            return new AttributeConfiguration(this);
        }
    }
}
