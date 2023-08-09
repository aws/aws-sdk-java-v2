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

package software.amazon.awssdk.codegen.poet.auth.scheme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.http.auth.BearerAuthScheme;
import software.amazon.awssdk.http.auth.aws.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner;
import software.amazon.awssdk.utils.Validate;

public final class AuthSchemeCodegenMetadata {

    static final AuthSchemeCodegenMetadata SIGV4 = builder()
        .schemeId(AwsV4AuthScheme.SCHEME_ID)
        .addProperty(SignerPropertyValueProvider.builder()
                                                .containingClass(AwsV4HttpSigner.class)
                                                .fieldName("SERVICE_SIGNING_NAME")
                                                .valueProvider(AuthSchemeSpecUtils::signingName)
                                                .valueType(ValueType.STRING)
                                                .build())
        .addProperty(SignerPropertyValueProvider.builder()
                                                .containingClass(AwsV4HttpSigner.class)
                                                .fieldName("REGION_NAME")
                                                .valueProvider(utils -> "params.region().id()")
                                                .valueType(ValueType.EXPRESSION)
                                                .build())
        .build();

    static final AuthSchemeCodegenMetadata S3 = builder()
        .schemeId(AwsV4AuthScheme.SCHEME_ID)
        .addProperty(SignerPropertyValueProvider.builder()
                                                .containingClass(AwsV4HttpSigner.class)
                                                .fieldName("SERVICE_SIGNING_NAME")
                                                .valueProvider(AuthSchemeSpecUtils::signingName)
                                                .valueType(ValueType.STRING)
                                                .build())
        .addProperty(SignerPropertyValueProvider.builder()
                                                .containingClass(AwsV4HttpSigner.class)
                                                .fieldName("REGION_NAME")
                                                .valueProvider(utils -> "params.region().id()")
                                                .valueType(ValueType.EXPRESSION)
                                                .build())
        .addProperty(SignerPropertyValueProvider.builder()
                                                .containingClass(AwsV4HttpSigner.class)
                                                .fieldName("DOUBLE_URL_ENCODE")
                                                .valueProvider(utils -> "false")
                                                .valueType(ValueType.EXPRESSION)
                                                .build())
        .addProperty(SignerPropertyValueProvider.builder()
                                                .containingClass(AwsV4HttpSigner.class)
                                                .fieldName("NORMALIZE_PATH")
                                                .valueProvider(utils -> "false")
                                                .valueType(ValueType.EXPRESSION)
                                                .build())
        .build();

    static final AuthSchemeCodegenMetadata BEARER = builder()
        .schemeId(BearerAuthScheme.SCHEME_ID)
        .build();

    static final AuthSchemeCodegenMetadata NO_AUTH = builder()
        .schemeId(SelectedAuthScheme.SMITHY_NO_AUTH)
        .build();

    private final String schemeId;
    private final List<SignerPropertyValueProvider> properties;

    private AuthSchemeCodegenMetadata(Builder builder) {
        this.schemeId = Validate.paramNotNull(builder.schemeId, "schemeId");
        this.properties = Collections.unmodifiableList(Validate.paramNotNull(builder.properties, "properties"));
    }

    public String schemeId() {
        return schemeId;
    }

    public Class<?> authSchemeClass() {
        switch (schemeId) {
            case AwsV4AuthScheme.SCHEME_ID:
                return AwsV4AuthScheme.class;
            case BearerAuthScheme.SCHEME_ID:
                return BearerAuthScheme.class;
            case SelectedAuthScheme.SMITHY_NO_AUTH:
                return null;
            default:
                throw new IllegalArgumentException("Auth scheme class for schemeId: " + schemeId + " not configured.");
        }
    }

    public List<SignerPropertyValueProvider> properties() {
        return properties;
    }

    private static Builder builder() {
        return new Builder();
    }

    public static AuthSchemeCodegenMetadata fromAuthType(AuthType type) {
        switch (type) {
            case BEARER:
                return BEARER;
            case NONE:
                return NO_AUTH;
            case V4:
            case V4_UNSIGNED_BODY:
                return SIGV4;
            case S3:
            case S3V4:
                return S3;
            default:
                throw new IllegalArgumentException("Unknown auth type: " + type);
        }
    }

    private static class Builder {
        private String schemeId;
        private List<SignerPropertyValueProvider> properties = new ArrayList<>();

        public Builder schemeId(String schemeId) {
            this.schemeId = schemeId;
            return this;
        }

        public Builder addProperty(SignerPropertyValueProvider property) {
            this.properties.add(property);
            return this;
        }

        public AuthSchemeCodegenMetadata build() {
            return new AuthSchemeCodegenMetadata(this);
        }
    }

    enum ValueType {
        STRING, EXPRESSION
    }

    static class SignerPropertyValueProvider {
        private final Class<?> containingClass;
        private final String fieldName;
        private final Function<AuthSchemeSpecUtils, String> valueProvider;
        private final ValueType valueType;

        SignerPropertyValueProvider(Builder builder) {
            this.containingClass = Validate.paramNotNull(builder.containingClass, "containingClass");
            this.valueProvider = Validate.paramNotNull(builder.valueProvider, "valueProvider");
            this.fieldName = Validate.paramNotNull(builder.fieldName, "fieldName");
            this.valueType = Validate.paramNotNull(builder.valueType, "valueType");
        }

        public Class<?> containingClass() {
            return containingClass;
        }

        public String fieldName() {
            return fieldName;
        }

        public Function<AuthSchemeSpecUtils, String> valueProvider() {
            return valueProvider;
        }

        public ValueType valueType() {
            return valueType;
        }

        private static Builder builder() {
            return new Builder();
        }

        static class Builder {
            private Class<?> containingClass;
            private String fieldName;
            private Function<AuthSchemeSpecUtils, String> valueProvider;
            private ValueType valueType;

            public Builder containingClass(Class<?> containingClass) {
                this.containingClass = containingClass;
                return this;
            }

            public Builder fieldName(String fieldName) {
                this.fieldName = fieldName;
                return this;
            }

            public Builder valueProvider(Function<AuthSchemeSpecUtils, String> valueProvider) {
                this.valueProvider = valueProvider;
                return this;
            }

            public Builder valueType(ValueType valueType) {
                this.valueType = valueType;
                return this;
            }

            public SignerPropertyValueProvider build() {
                return new SignerPropertyValueProvider(this);
            }
        }
    }
}
