/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static software.amazon.awssdk.utils.Validate.notNull;

/**
 * Encapsulates the API name and version of a library built using the AWS SDK.
 *
 * See {@link RequestOverrideConfiguration.Builder#addApiName(ApiName)}.
 */
public final class ApiName {
    private final String name;
    private final String version;

    private ApiName(BuilderImpl b) {
        this.name = notNull(b.name, "name must not be null");
        this.version = notNull(b.version, "version must not be null");
    }

    public String name() {
        return name;
    }

    public String version() {
        return version;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder {
        /**
         * Set the name of the API.
         *
         * @param name The name.
         *
         * @return This object for method chaining.
         */
        Builder name(String name);

        /**
         * Set the version of the API.
         *
         * @param version The version.
         *
         * @return This object for method chaining.
         */
        Builder version(String version);

        ApiName build();
    }

    private static class BuilderImpl implements Builder {
        private String name;
        private String version;

        @Override
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        @Override
        public ApiName build() {
            return new ApiName(this);
        }
    }
}
