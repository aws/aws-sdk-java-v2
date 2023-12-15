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

package software.amazon.awssdk.http.auth.spi.internal.scheme;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.SignerProperty;
import software.amazon.awssdk.identity.spi.IdentityProperty;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
@Immutable
public final class DefaultAuthSchemeOption implements AuthSchemeOption {

    private final String schemeId;
    private final Map<IdentityProperty<?>, Object> identityProperties;
    private final Map<SignerProperty<?>, Object> signerProperties;

    DefaultAuthSchemeOption(BuilderImpl builder) {
        this.schemeId = Validate.paramNotBlank(builder.schemeId, "schemeId");
        this.identityProperties = new HashMap<>(builder.identityProperties);
        this.signerProperties = new HashMap<>(builder.signerProperties);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public String schemeId() {
        return schemeId;
    }

    @SuppressWarnings("unchecked") // Safe because of the implementation of putIdentityProperty
    @Override
    public <T> T identityProperty(IdentityProperty<T> property) {
        return (T) identityProperties.get(property);
    }

    @SuppressWarnings("unchecked") // Safe because of the implementation of putSignerProperty
    @Override
    public <T> T signerProperty(SignerProperty<T> property) {
        return (T) signerProperties.get(property);
    }

    @Override
    public void forEachIdentityProperty(IdentityPropertyConsumer consumer) {
        identityProperties.keySet().forEach(property -> consumeProperty(property, consumer));
    }

    private <T> void consumeProperty(IdentityProperty<T> property, IdentityPropertyConsumer consumer) {
        consumer.accept(property, this.identityProperty(property));
    }

    @Override
    public void forEachSignerProperty(SignerPropertyConsumer consumer) {
        signerProperties.keySet().forEach(property -> consumeProperty(property, consumer));
    }

    private <T> void consumeProperty(SignerProperty<T> property, SignerPropertyConsumer consumer) {
        consumer.accept(property, this.signerProperty(property));
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    @Override
    public String toString() {
        return ToString.builder("AuthSchemeOption")
                       .add("schemeId", schemeId)
                       .add("identityProperties", identityProperties)
                       .add("signerProperties", signerProperties)
                       .build();
    }


    public static final class BuilderImpl implements Builder {
        private String schemeId;
        private final Map<IdentityProperty<?>, Object> identityProperties = new HashMap<>();
        private final Map<SignerProperty<?>, Object> signerProperties = new HashMap<>();

        private BuilderImpl() {
        }

        private BuilderImpl(DefaultAuthSchemeOption authSchemeOption) {
            this.schemeId = authSchemeOption.schemeId;
            this.identityProperties.putAll(authSchemeOption.identityProperties);
            this.signerProperties.putAll(authSchemeOption.signerProperties);
        }

        @Override
        public Builder schemeId(String schemeId) {
            this.schemeId = schemeId;
            return this;
        }

        @Override
        public <T> Builder putIdentityProperty(IdentityProperty<T> key, T value) {
            this.identityProperties.put(key, value);
            return this;
        }

        @Override
        public <T> Builder putIdentityPropertyIfAbsent(IdentityProperty<T> key, T value) {
            this.identityProperties.putIfAbsent(key, value);
            return this;
        }

        @Override
        public <T> Builder putSignerProperty(SignerProperty<T> key, T value) {
            this.signerProperties.put(key, value);
            return this;
        }

        @Override
        public <T> Builder putSignerPropertyIfAbsent(SignerProperty<T> key, T value) {
            this.signerProperties.putIfAbsent(key, value);
            return this;
        }

        @Override
        public AuthSchemeOption build() {
            return new DefaultAuthSchemeOption(this);
        }
    }
}
