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
import software.amazon.awssdk.http.auth.spi.BaseSignedRequest;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
abstract class DefaultBaseSignedRequest<PayloadT> implements BaseSignedRequest<PayloadT> {

    protected final SdkHttpRequest request;
    protected final PayloadT payload;

    protected DefaultBaseSignedRequest(BuilderImpl<?, PayloadT> builder) {
        this.request = Validate.paramNotNull(builder.request, "request");
        this.payload = builder.payload;
    }

    @Override
    public SdkHttpRequest request() {
        return request;
    }

    @Override
    public Optional<PayloadT> payload() {
        return Optional.ofNullable(payload);
    }

    protected abstract static class BuilderImpl<B extends Builder<B, PayloadT>, PayloadT> implements Builder<B, PayloadT> {
        private SdkHttpRequest request;
        private PayloadT payload;

        protected BuilderImpl() {
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

        private B thisBuilder() {
            return (B) this;
        }
    }
}
