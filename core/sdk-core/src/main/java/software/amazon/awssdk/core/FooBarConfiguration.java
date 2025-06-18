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

package software.amazon.awssdk.core;

import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;


@Immutable
@SdkPublicApi
public final class FooBarConfiguration {

    private final String foo;
    private final List<String> bar;
    private Optional<String> optionalField;

    private FooBarConfiguration(Builder builder) {
        this.foo = builder.foo;
        this.bar = builder.bar;
        this.optionalField = builder.optionalField;
    }

    /**
     * Returns the foo configuration value.
     *
     * @return The foo value, if present.
     */
    public Optional<String> foo() {
        return Optional.ofNullable(foo);
    }

    /**
     * Returns the bar configuration value.
     *
     * @return The bar value, if present.
     */
    public Optional<List<String>> bar() {
        return Optional.ofNullable(bar);
    }

    /**
     * Create a new builder for {@link FooBarConfiguration}.
     *
     * @return A new builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link FooBarConfiguration}.
     */
    public static final class Builder {
        private Optional<String> optionalField;
        private String foo;
        private List<String> bar;

        private Builder() {
        }

        public Builder foo(String foo) {
            this.foo = foo;
            return this;
        }

        public Builder bar(List<String> bar) {
            this.bar = bar;
            return this;
        }

        public Builder optionalField(Optional<String> optionalField) {
            this.optionalField = optionalField;
            return this;
        }

        /**
         * Build a new {@link FooBarConfiguration} with the properties set on this builder.
         *
         * @return The new {@code FooBarConfiguration}.
         */
        public FooBarConfiguration build() {
            return new FooBarConfiguration(this);
        }
    }
}
