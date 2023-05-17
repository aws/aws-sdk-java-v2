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
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.SignerProperty;
import software.amazon.awssdk.http.auth.spi.SyncHttpSignRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultSyncHttpSignRequest<IdentityT extends Identity> implements SyncHttpSignRequest<IdentityT> {

    private final SdkHttpRequest request;
    private final ContentStreamProvider payload;
    private final IdentityT identity;
    private final Map<SignerProperty<?>, Object> properties;

    DefaultSyncHttpSignRequest(BuilderImpl<IdentityT>  builder) {
        this.request = Validate.paramNotNull(builder.request, "request");
        this.payload = builder.payload;
        this.identity = Validate.paramNotNull(builder.identity, "identity");
        this.properties = new HashMap<>(builder.properties);
    }

    @Override
    public SdkHttpRequest request() {
        return request;
    }

    @Override
    public Optional<ContentStreamProvider> payload() {
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
        return ToString.builder("SyncHttpSignRequest")
                       .add("request", request)
                       .add("properties", properties)
                       .build();
    }

    @SdkInternalApi
    public static final class BuilderImpl<IdentityT extends Identity> implements Builder<IdentityT> {
        private SdkHttpRequest request;
        private ContentStreamProvider payload;
        private IdentityT identity;
        private final Map<SignerProperty<?>, Object> properties = new HashMap<>();

        @Override
        public Builder<IdentityT> request(SdkHttpRequest request) {
            this.request = request;
            return this;
        }

        @Override
        public Builder<IdentityT> payload(ContentStreamProvider payload) {
            this.payload = payload;
            return this;
        }

        @Override
        public Builder<IdentityT> identity(IdentityT identity) {
            this.identity = identity;
            return this;
        }

        @Override
        public <T> Builder<IdentityT> putProperty(SignerProperty<T> key, T value) {
            this.properties.put(key, value);
            return this;
        }

        @Override
        public SyncHttpSignRequest<IdentityT> build() {
            return new DefaultSyncHttpSignRequest<>(this);
        }
    }
}
