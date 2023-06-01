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

package software.amazon.awssdk.services.query.auth.scheme.internal;

import java.util.Optional;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.query.auth.scheme.QueryAuthSchemeParams;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultQueryAuthSchemeParams implements QueryAuthSchemeParams {
    private final String operation;

    private final String region;

    private final Boolean defaultTrueParam;

    private final String defaultStringParam;

    private final String deprecatedParam;

    private final Boolean booleanContextParam;

    private final String stringContextParam;

    private final String operationContextParam;

    private DefaultQueryAuthSchemeParams(Builder builder) {
        this.operation = Validate.paramNotNull(builder.operation, "operation");
        this.region = builder.region;
        this.defaultTrueParam = builder.defaultTrueParam;
        this.defaultStringParam = builder.defaultStringParam;
        this.deprecatedParam = builder.deprecatedParam;
        this.booleanContextParam = builder.booleanContextParam;
        this.stringContextParam = builder.stringContextParam;
        this.operationContextParam = builder.operationContextParam;
    }

    public static QueryAuthSchemeParams.Builder builder() {
        return new Builder();
    }

    @Override
    public String operation() {
        return operation;
    }

    @Override
    public Optional<String> region() {
        return region == null ? Optional.empty() : Optional.of(region);
    }

    @Override
    public Boolean defaultTrueParam() {
        return defaultTrueParam;
    }

    @Override
    public String defaultStringParam() {
        return defaultStringParam;
    }

    @Deprecated
    @Override
    public String deprecatedParam() {
        return deprecatedParam;
    }

    @Override
    public Boolean booleanContextParam() {
        return booleanContextParam;
    }

    @Override
    public String stringContextParam() {
        return stringContextParam;
    }

    @Override
    public String operationContextParam() {
        return operationContextParam;
    }

    private static final class Builder implements QueryAuthSchemeParams.Builder {
        private String operation;

        private String region;

        private Boolean defaultTrueParam;

        private String defaultStringParam;

        private String deprecatedParam;

        private Boolean booleanContextParam;

        private String stringContextParam;

        private String operationContextParam;

        @Override
        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }

        @Override
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        @Override
        public Builder defaultTrueParam(Boolean defaultTrueParam) {
            this.defaultTrueParam = defaultTrueParam;
            return this;
        }

        @Override
        public Builder defaultStringParam(String defaultStringParam) {
            this.defaultStringParam = defaultStringParam;
            return this;
        }

        @Override
        public Builder deprecatedParam(String deprecatedParam) {
            this.deprecatedParam = deprecatedParam;
            return this;
        }

        @Override
        public Builder booleanContextParam(Boolean booleanContextParam) {
            this.booleanContextParam = booleanContextParam;
            return this;
        }

        @Override
        public Builder stringContextParam(String stringContextParam) {
            this.stringContextParam = stringContextParam;
            return this;
        }

        @Override
        public Builder operationContextParam(String operationContextParam) {
            this.operationContextParam = operationContextParam;
            return this;
        }

        @Override
        public QueryAuthSchemeParams build() {
            return new DefaultQueryAuthSchemeParams(this);
        }
    }
}
