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
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.HttpSignRequest;
import software.amazon.awssdk.http.auth.spi.SignerProperty;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultHttpSignRequest<PayloadT, IdentityT> implements HttpSignRequest<PayloadT, IdentityT> {

    private final Class<PayloadT> payloadType;
    private final SdkHttpRequest request;
    private final PayloadT payload;
    private final IdentityT identity;
    private final Map<SignerProperty<?>, Object> properties;

    DefaultHttpSignRequest(BuilderImpl<PayloadT, IdentityT>  builder) {
        this.payloadType = Validate.paramNotNull(builder.payloadType, "payloadType");
        this.request = Validate.paramNotNull(builder.request, "request");
        this.payload = builder.payload;
        this.identity = Validate.paramNotNull(builder.identity, "identity");
        this.properties = new HashMap<>(builder.properties);
    }

    @Override
    public Class<PayloadT> payloadType() {
        return payloadType;
    }

    @Override
    public SdkHttpRequest request() {
        return request;
    }

    @Override
    public Optional<PayloadT> payload() {
        return payload == null ? Optional.empty() : Optional.of(payload);
    }

    @Override
    public IdentityT identity() {
        return identity;
    }

    @Override
    public <T> T property(SignerProperty<T> property) {
        return (T) properties.get(property);
    }

    @Override
    public String toString() {
        return ToString.builder("HttpSignRequest")
                       .add("payloadType", payloadType)
                       .add("request", request)
                       .add("properties", properties)
                       .build();
    }


    public static final class BuilderImpl<PayloadT, IdentityT> implements Builder<PayloadT, IdentityT> {
        private final Class<PayloadT> payloadType;
        private SdkHttpRequest request;
        private PayloadT payload;
        private IdentityT identity;
        private final Map<SignerProperty<?>, Object> properties = new HashMap<>();

        public BuilderImpl(Class<PayloadT> payloadType) {
            this.payloadType = payloadType;
        }

        @Override
        public Builder<PayloadT, IdentityT> request(SdkHttpRequest request) {
            this.request = request;
            return this;
        }

        @Override
        public Builder<PayloadT, IdentityT> payload(PayloadT payload) {
            this.payload = payload;
            return this;
        }

        @Override
        public Builder<PayloadT, IdentityT> identity(IdentityT identity) {
            this.identity = identity;
            return this;
        }

        @Override
        public <T> Builder<PayloadT, IdentityT> putProperty(SignerProperty<T> key, T value) {
            this.properties.put(key, value);
            return this;
        }

        @Override
        public HttpSignRequest<PayloadT, IdentityT> build() {
            return new DefaultHttpSignRequest<>(this);
        }
    }
}
