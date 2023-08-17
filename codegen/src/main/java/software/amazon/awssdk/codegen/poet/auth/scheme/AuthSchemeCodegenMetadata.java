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

import com.squareup.javapoet.MethodSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.http.auth.BearerAuthScheme;
import software.amazon.awssdk.http.auth.aws.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.NoAuthAuthScheme;
import software.amazon.awssdk.utils.Validate;

public final class AuthSchemeCodegenMetadata {

    static final AuthSchemeCodegenMetadata SIGV4 = builder()
        .schemeId(AwsV4AuthScheme.SCHEME_ID)
        .authSchemeClass(AwsV4AuthScheme.class)
        .addProperty(SignerPropertyValueProvider.builder()
                                                .containingClass(AwsV4HttpSigner.class)
                                                .fieldName("SERVICE_SIGNING_NAME")
                                                .valueEmitter((spec, utils) -> spec.addCode("$S", utils.signingName()))
                                                .build())
        .addProperty(SignerPropertyValueProvider.builder()
                                                .containingClass(AwsV4HttpSigner.class)
                                                .fieldName("REGION_NAME")
                                                .valueEmitter((spec, utils) -> spec.addCode("$L", "params.region().id()"))
                                                .build())
        .build();

    static final AuthSchemeCodegenMetadata SIGV4_UNSIGNED_BODY =
        SIGV4.toBuilder()
             .addProperty(SignerPropertyValueProvider.builder()
                                                     .containingClass(AwsV4HttpSigner.class)
                                                     .fieldName("PAYLOAD_SIGNING_ENABLED")
                                                     .valueEmitter((spec, utils) -> spec.addCode("$L", false))
                                                     .build())
             .build();

    static final AuthSchemeCodegenMetadata S3 =
        SIGV4.toBuilder()
             .addProperty(SignerPropertyValueProvider.builder()
                                                     .containingClass(AwsV4HttpSigner.class)
                                                     .fieldName("DOUBLE_URL_ENCODE")
                                                     .valueEmitter((spec, utils) -> spec.addCode("$L", "false"))
                                                     .build())
             .addProperty(SignerPropertyValueProvider.builder()
                                                     .containingClass(AwsV4HttpSigner.class)
                                                     .fieldName("NORMALIZE_PATH")
                                                     .valueEmitter((spec, utils) -> spec.addCode("$L", "false"))
                                                     .build())
             .addProperty(SignerPropertyValueProvider.builder()
                                                     .containingClass(AwsV4HttpSigner.class)
                                                     .fieldName("PAYLOAD_SIGNING_ENABLED")
                                                     .valueEmitter((spec, utils) -> spec.addCode("$L", false))
                                                     .build())
             .build();

    static final AuthSchemeCodegenMetadata S3V4 =
        SIGV4.toBuilder()
             .addProperty(SignerPropertyValueProvider.builder()
                                                     .containingClass(AwsV4HttpSigner.class)
                                                     .fieldName("DOUBLE_URL_ENCODE")
                                                     .valueEmitter((spec, utils) -> spec.addCode("$L", "false"))
                                                     .build())
             .addProperty(SignerPropertyValueProvider.builder()
                                                     .containingClass(AwsV4HttpSigner.class)
                                                     .fieldName("NORMALIZE_PATH")
                                                     .valueEmitter((spec, utils) -> spec.addCode("$L", "false"))
                                                     .build())
             .build();

    static final AuthSchemeCodegenMetadata BEARER = builder()
        .schemeId(BearerAuthScheme.SCHEME_ID)
        .authSchemeClass(BearerAuthScheme.class)
        .build();

    static final AuthSchemeCodegenMetadata NO_AUTH = builder()
        .schemeId(NoAuthAuthScheme.SCHEME_ID)
        .authSchemeClass(NoAuthAuthScheme.class)
        .build();

    private final String schemeId;
    private final List<SignerPropertyValueProvider> properties;
    private final Class<?> authSchemeClass;

    private AuthSchemeCodegenMetadata(Builder builder) {
        this.schemeId = Validate.paramNotNull(builder.schemeId, "schemeId");
        this.properties = Collections.unmodifiableList(Validate.paramNotNull(builder.properties, "properties"));
        this.authSchemeClass = Validate.paramNotNull(builder.authSchemeClass, "authSchemeClass");
    }

    public String schemeId() {
        return schemeId;
    }

    public Class<?> authSchemeClass() {
        return authSchemeClass;
    }

    public List<SignerPropertyValueProvider> properties() {
        return properties;
    }

    public Builder toBuilder() {
        return new Builder(this);
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
                return SIGV4;
            case V4_UNSIGNED_BODY:
                return SIGV4_UNSIGNED_BODY;
            case S3:
                return S3;
            case S3V4:
                return S3V4;
            default:
                throw new IllegalArgumentException("Unknown auth type: " + type);
        }
    }

    private static class Builder {
        private String schemeId;
        private List<SignerPropertyValueProvider> properties = new ArrayList<>();
        private Class<?> authSchemeClass;

        Builder() {
        }

        Builder(AuthSchemeCodegenMetadata other) {
            this.schemeId = other.schemeId;
            this.properties.addAll(other.properties);
            this.authSchemeClass = other.authSchemeClass;
        }

        public Builder schemeId(String schemeId) {
            this.schemeId = schemeId;
            return this;
        }

        public Builder addProperty(SignerPropertyValueProvider property) {
            this.properties.add(property);
            return this;
        }

        public Builder authSchemeClass(Class<?> authSchemeClass) {
            this.authSchemeClass = authSchemeClass;
            return this;
        }

        public AuthSchemeCodegenMetadata build() {
            return new AuthSchemeCodegenMetadata(this);
        }
    }

    static class SignerPropertyValueProvider {
        private final Class<?> containingClass;
        private final String fieldName;
        private final BiConsumer<MethodSpec.Builder, AuthSchemeSpecUtils> valueEmitter;

        SignerPropertyValueProvider(Builder builder) {
            this.containingClass = Validate.paramNotNull(builder.containingClass, "containingClass");
            this.valueEmitter = Validate.paramNotNull(builder.valueEmitter, "valueEmitter");
            this.fieldName = Validate.paramNotNull(builder.fieldName, "fieldName");
        }

        public Class<?> containingClass() {
            return containingClass;
        }

        public String fieldName() {
            return fieldName;
        }

        public void emitValue(MethodSpec.Builder spec, AuthSchemeSpecUtils utils) {
            valueEmitter.accept(spec, utils);
        }

        private static Builder builder() {
            return new Builder();
        }

        static class Builder {
            private Class<?> containingClass;
            private String fieldName;
            private BiConsumer<MethodSpec.Builder, AuthSchemeSpecUtils> valueEmitter;

            public Builder containingClass(Class<?> containingClass) {
                this.containingClass = containingClass;
                return this;
            }

            public Builder fieldName(String fieldName) {
                this.fieldName = fieldName;
                return this;
            }

            public Builder valueEmitter(BiConsumer<MethodSpec.Builder, AuthSchemeSpecUtils> valueEmitter) {
                this.valueEmitter = valueEmitter;
                return this;
            }

            public SignerPropertyValueProvider build() {
                return new SignerPropertyValueProvider(this);
            }
        }
    }
}
