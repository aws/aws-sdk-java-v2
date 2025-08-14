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

package software.amazon.awssdk.core.useragent;

import static software.amazon.awssdk.utils.Validate.notNull;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.internal.useragent.UserAgentConstant;

/**
 * Represents UserAgent additional metadata following the format: md/[name]#[value]
 */
@SdkProtectedApi
public final class AdditionalMetadata {
    private final String name;
    private final String value;

    private AdditionalMetadata(BuilderImpl b) {
        this.name = notNull(b.name, "name must not be null");
        this.value = notNull(b.value, "value must not be null");
    }

    public String name() {
        return name;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        // Format "md/{name}#{value}"
        return UserAgentConstant.field(
            UserAgentConstant.METADATA,
            UserAgentConstant.uaPair(name, value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AdditionalMetadata that = (AdditionalMetadata) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder {
        /**
         * Set the name of the additional metadata.
         *
         * @param name The name.
         * @return This object for method chaining.
         */
        Builder name(String name);

        /**
         * Set the value of the additional metadata.
         *
         * @param value The value.
         * @return This object for method chaining.
         */
        Builder value(String value);

        AdditionalMetadata build();
    }

    private static final class BuilderImpl implements Builder {
        private String name;
        private String value;

        @Override
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder value(String value) {
            this.value = value;
            return this;
        }

        @Override
        public AdditionalMetadata build() {
            return new AdditionalMetadata(this);
        }
    }
}
