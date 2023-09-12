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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.BaseSignRequest;
import software.amazon.awssdk.http.auth.spi.SignerProperty;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
abstract class DefaultBaseSignRequest<PayloadT, IdentityT extends Identity> implements BaseSignRequest<PayloadT, IdentityT> {

    protected final SdkHttpRequest request;
    protected final PayloadT payload;
    protected final IdentityT identity;
    protected final Map<SignerProperty<?>, Object> properties;

    protected DefaultBaseSignRequest(BuilderImpl<?, PayloadT, IdentityT> builder) {
        this.request = Validate.paramNotNull(builder.request, "request");
        this.payload = builder.payload;
        this.identity = Validate.paramNotNull(builder.identity, "identity");
        this.properties = Collections.unmodifiableMap(new HashMap<>(builder.properties));
    }

    @Override
    public SdkHttpRequest request() {
        return request;
    }

    @Override
    public Optional<PayloadT> payload() {
        return Optional.ofNullable(payload);
    }

    @Override
    public IdentityT identity() {
        return identity;
    }

    @Override
    public <T> T property(SignerProperty<T> property) {
        return (T) properties.get(property);
    }

    @SdkInternalApi
    protected abstract static class BuilderImpl<B extends Builder<B, PayloadT, IdentityT>, PayloadT,
        IdentityT extends Identity> implements Builder<B, PayloadT, IdentityT> {
        private final Map<SignerProperty<?>, Object> properties = new HashMap<>();
        private SdkHttpRequest request;
        private PayloadT payload;
        private IdentityT identity;

        protected BuilderImpl() {
        }

        protected BuilderImpl(IdentityT identity) {
            this.identity = identity;
        }

        @Override
        public B request(SdkHttpRequest request) {
            this.request = request;
            return thisBuilder();
        }

        @Override
        public B payload(PayloadT payload) {
            this.payload = payload;
            return thisBuilder();
        }

        @Override
        public B identity(IdentityT identity) {
            this.identity = identity;
            return thisBuilder();
        }

        @Override
        public <T> B putProperty(SignerProperty<T> key, T value) {
            this.properties.put(key, value);
            return thisBuilder();
        }

        protected B properties(Map<SignerProperty<?>, Object> properties) {
            this.properties.clear();
            this.properties.putAll(properties);
            return thisBuilder();
        }

        private B thisBuilder() {
            return (B) this;
        }
    }
}
