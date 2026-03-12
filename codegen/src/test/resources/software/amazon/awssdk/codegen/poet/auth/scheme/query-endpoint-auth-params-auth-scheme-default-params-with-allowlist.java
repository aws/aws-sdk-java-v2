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

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.query.auth.scheme.QueryAuthSchemeParams;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointProvider;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultQueryAuthSchemeParams implements QueryAuthSchemeParams, QueryEndpointResolverAware {
    private final String operation;

    private final Region region;

    private final RegionSet regionSet;

    private final Boolean defaultTrueParam;

    private final String defaultStringParam;

    private final String deprecatedParam;

    private final Boolean booleanContextParam;

    private final String stringContextParam;

    private final String operationContextParam;

    private final QueryEndpointProvider endpointProvider;

    private DefaultQueryAuthSchemeParams(Builder builder) {
        this.operation = Validate.paramNotNull(builder.operation, "operation");
        this.region = builder.region;
        this.regionSet = builder.regionSet;
        this.defaultTrueParam = Validate.paramNotNull(builder.defaultTrueParam, "defaultTrueParam");
        this.defaultStringParam = Validate.paramNotNull(builder.defaultStringParam, "defaultStringParam");
        this.deprecatedParam = builder.deprecatedParam;
        this.booleanContextParam = builder.booleanContextParam;
        this.stringContextParam = builder.stringContextParam;
        this.operationContextParam = builder.operationContextParam;
        this.endpointProvider = builder.endpointProvider;
    }

    public static QueryAuthSchemeParams.Builder builder() {
        return new Builder();
    }

    @Override
    public String operation() {
        return operation;
    }

    @Override
    public Region region() {
        return region;
    }

    @Override
    public RegionSet regionSet() {
        return regionSet;
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

    @Override
    public QueryEndpointProvider endpointProvider() {
        return endpointProvider;
    }

    @Override
    public QueryAuthSchemeParams.Builder toBuilder() {
        return new Builder(this);
    }

    private static final class Builder implements QueryAuthSchemeParams.Builder, QueryEndpointResolverAware.Builder {
        private String operation;

        private Region region;

        private RegionSet regionSet;

        private Boolean defaultTrueParam = true;

        private String defaultStringParam = "hello endpoints";

        private String deprecatedParam;

        private Boolean booleanContextParam;

        private String stringContextParam;

        private String operationContextParam;

        private QueryEndpointProvider endpointProvider;

        Builder() {
        }

        Builder(DefaultQueryAuthSchemeParams params) {
            this.operation = params.operation;
            this.region = params.region;
            this.regionSet = params.regionSet;
            this.defaultTrueParam = params.defaultTrueParam;
            this.defaultStringParam = params.defaultStringParam;
            this.deprecatedParam = params.deprecatedParam;
            this.booleanContextParam = params.booleanContextParam;
            this.stringContextParam = params.stringContextParam;
            this.operationContextParam = params.operationContextParam;
            this.endpointProvider = params.endpointProvider;
        }

        @Override
        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }

        @Override
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public Builder regionSet(RegionSet regionSet) {
            this.regionSet = regionSet;
            return this;
        }

        @Override
        public Builder defaultTrueParam(Boolean defaultTrueParam) {
            this.defaultTrueParam = defaultTrueParam;
            if (this.defaultTrueParam == null) {
                this.defaultTrueParam = true;
            }
            return this;
        }

        @Override
        public Builder defaultStringParam(String defaultStringParam) {
            this.defaultStringParam = defaultStringParam;
            if (this.defaultStringParam == null) {
                this.defaultStringParam = "hello endpoints";
            }
            return this;
        }

        @Deprecated
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
        public Builder endpointProvider(QueryEndpointProvider endpointProvider) {
            this.endpointProvider = endpointProvider;
            return this;
        }

        @Override
        public QueryAuthSchemeParams build() {
            return new DefaultQueryAuthSchemeParams(this);
        }
    }
}
