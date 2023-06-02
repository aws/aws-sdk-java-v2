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

package software.amazon.awssdk.http.auth.spi.internal;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.spi.AuthOption;
import software.amazon.awssdk.http.auth.spi.SignerProperty;
import software.amazon.awssdk.identity.spi.IdentityProperty;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultAuthOption implements AuthOption {

    private final String schemeId;
    private final Map<IdentityProperty<?>, Object> identityProperties;
    private final Map<SignerProperty<?>, Object> signerProperties;

    DefaultAuthOption(BuilderImpl builder) {
        this.schemeId = Validate.paramNotBlank(builder.schemeId, "schemeId");
        this.identityProperties = new HashMap<>(builder.identityProperties);
        this.signerProperties = new HashMap<>(builder.signerProperties);
    }

    @Override
    public String schemeId() {
        return schemeId;
    }

    @Override
    public <T> T identityProperty(IdentityProperty<T> property) {
        return (T) identityProperties.get(property);
    }

    @Override
    public <T> T signerProperty(SignerProperty<T> property) {
        return (T) signerProperties.get(property);
    }

    @Override
    public <T> void forEachIdentityProperty(IdentityPropertyConsumer consumer) {
        for (IdentityProperty<?> p : identityProperties.keySet()) {
            IdentityProperty<T> property = (IdentityProperty<T>) p;
            consumer.accept(property, this.identityProperty(property));
        }
    }

    @Override
    public <T> void forEachSignerProperty(SignerPropertyConsumer consumer) {
        for (SignerProperty<?> p : signerProperties.keySet()) {
            SignerProperty<T> property = (SignerProperty<T>) p;
            consumer.accept(property, this.signerProperty(property));
        }
    }

    @Override
    public String toString() {
        return ToString.builder("AuthOption")
                       .add("identityProperties", identityProperties)
                       .add("signerProperties", signerProperties)
                       .build();
    }


    public static final class BuilderImpl implements Builder {
        private String schemeId;
        private final Map<IdentityProperty<?>, Object> identityProperties = new HashMap<>();
        private final Map<SignerProperty<?>, Object> signerProperties = new HashMap<>();

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
        public <T> Builder putSignerProperty(SignerProperty<T> key, T value) {
            this.signerProperties.put(key, value);
            return this;
        }

        @Override
        public AuthOption build() {
            return new DefaultAuthOption(this);
        }
    }
}
