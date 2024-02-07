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

import com.squareup.javapoet.CodeBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import software.amazon.awssdk.utils.Validate;

/**
 * Represents a modeled auth scheme option.
 */
public final class AuthSchemeCodegenMetadata {
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
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

        public Builder properties(List<SignerPropertyValueProvider> properties) {
            this.properties.clear();
            this.properties.addAll(properties);
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
        private final BiConsumer<CodeBlock.Builder, AuthSchemeSpecUtils> valueEmitter;
        private final Supplier<Object> valueSupplier;

        SignerPropertyValueProvider(Builder builder) {
            this.containingClass = Validate.paramNotNull(builder.containingClass, "containingClass");
            this.valueEmitter = Validate.paramNotNull(builder.valueEmitter, "valueEmitter");
            this.fieldName = Validate.paramNotNull(builder.fieldName, "fieldName");
            this.valueSupplier = builder.valueSupplier;
        }

        public Class<?> containingClass() {
            return containingClass;
        }

        public String fieldName() {
            return fieldName;
        }

        public boolean isConstant() {
            return valueSupplier != null;
        }

        public Object value() {
            return valueSupplier.get();
        }

        public void emitValue(CodeBlock.Builder spec, AuthSchemeSpecUtils utils) {
            valueEmitter.accept(spec, utils);
        }


        public static Builder builder() {
            return new Builder();
        }

        static class Builder {
            private Class<?> containingClass;
            private String fieldName;
            private BiConsumer<CodeBlock.Builder, AuthSchemeSpecUtils> valueEmitter;
            private Supplier<Object> valueSupplier;

            public Builder containingClass(Class<?> containingClass) {
                this.containingClass = containingClass;
                return this;
            }

            public Builder fieldName(String fieldName) {
                this.fieldName = fieldName;
                return this;
            }

            public Builder valueEmitter(BiConsumer<CodeBlock.Builder, AuthSchemeSpecUtils> valueEmitter) {
                this.valueEmitter = valueEmitter;
                return this;
            }

            public Builder constantValueSupplier(Supplier<Object> valueSupplier) {
                this.valueSupplier = valueSupplier;
                if (valueEmitter == null) {
                    valueEmitter = (spec, utils) -> spec.add("$L", valueSupplier.get());
                }
                return this;
            }

            public SignerPropertyValueProvider build() {
                return new SignerPropertyValueProvider(this);
            }
        }
    }
}
