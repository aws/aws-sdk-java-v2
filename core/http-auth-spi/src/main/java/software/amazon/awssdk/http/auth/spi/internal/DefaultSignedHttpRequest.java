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

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.SignedHttpRequest;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultSignedHttpRequest<PayloadT> implements SignedHttpRequest<PayloadT> {

    private final Class<PayloadT> payloadType;
    private final SdkHttpRequest request;
    private final PayloadT payload;

    DefaultSignedHttpRequest(BuilderImpl<PayloadT>  builder) {
        this.payloadType = Validate.paramNotNull(builder.payloadType, "payloadType");
        this.request = Validate.paramNotNull(builder.request, "request");
        this.payload = builder.payload;
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
    public Optional payload() {
        return payload == null ? Optional.empty() : Optional.of(payload);
    }

    @Override
    public String toString() {
        return ToString.builder("SignedHttpRequest")
                       .add("payloadType", payloadType)
                       .add("request", request)
                       .build();
    }

    public static final class BuilderImpl<PayloadT> implements Builder<PayloadT> {
        private final Class<PayloadT> payloadType;
        private SdkHttpRequest request;
        private PayloadT payload;

        public BuilderImpl(Class<PayloadT> payloadType) {
            this.payloadType = payloadType;
        }

        @Override
        public Builder request(SdkHttpRequest request) {
            this.request = request;
            return this;
        }

        @Override
        public Builder payload(PayloadT payload) {
            this.payload = payload;
            return this;
        }

        @Override
        public SignedHttpRequest build() {
            return new DefaultSignedHttpRequest(this);
        }
    }
}
