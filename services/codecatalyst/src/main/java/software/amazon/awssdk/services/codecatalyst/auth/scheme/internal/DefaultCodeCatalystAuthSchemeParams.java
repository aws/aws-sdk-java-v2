/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.codecatalyst.auth.scheme.internal;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.codecatalyst.auth.scheme.CodeCatalystAuthSchemeParams;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultCodeCatalystAuthSchemeParams implements CodeCatalystAuthSchemeParams {
    private final String operation;

    private DefaultCodeCatalystAuthSchemeParams(Builder builder) {
        this.operation = Validate.paramNotNull(builder.operation, "operation");
    }

    public static CodeCatalystAuthSchemeParams.Builder builder() {
        return new Builder();
    }

    @Override
    public String operation() {
        return operation;
    }

    @Override
    public CodeCatalystAuthSchemeParams.Builder toBuilder() {
        return new Builder(this);
    }

    private static final class Builder implements CodeCatalystAuthSchemeParams.Builder {
        private String operation;

        Builder() {
        }

        Builder(DefaultCodeCatalystAuthSchemeParams params) {
            this.operation = params.operation;
        }

        @Override
        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }

        @Override
        public CodeCatalystAuthSchemeParams build() {
            return new DefaultCodeCatalystAuthSchemeParams(this);
        }
    }
}
