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

import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.utils.ToString;

@SdkInternalApi
public final class DefaultAsyncSignRequest<IdentityT extends Identity>
    extends DefaultBaseSignRequest<Publisher<ByteBuffer>, IdentityT> implements AsyncSignRequest<IdentityT> {

    private DefaultAsyncSignRequest(BuilderImpl<IdentityT> builder) {
        super(builder);
    }

    public static <IdentityT extends Identity> AsyncSignRequest.Builder<IdentityT> builder() {
        return new BuilderImpl<>();
    }

    public static <IdentityT extends Identity> AsyncSignRequest.Builder<IdentityT> builder(IdentityT identity) {
        return new BuilderImpl<>(identity);
    }

    @Override
    public String toString() {
        return ToString.builder("AsyncSignRequest")
            .add("request", request)
            .add("identity", identity)
            .add("properties", properties)
            .build();
    }

    @Override
    public AsyncSignRequest.Builder<IdentityT> toBuilder() {
        return new BuilderImpl<>(this);
    }

    @SdkInternalApi
    public static final class BuilderImpl<IdentityT extends Identity>
        extends DefaultBaseSignRequest.BuilderImpl<AsyncSignRequest.Builder<IdentityT>, Publisher<ByteBuffer>, IdentityT>
        implements AsyncSignRequest.Builder<IdentityT> {

        // Used to enable consumer builder pattern in HttpSigner.signAsync()
        private BuilderImpl() {
        }

        // Used by AsyncSignRequest#builder() where identity is passed as parameter, to avoid having to pass Class<IdentityT>.
        private BuilderImpl(IdentityT identity) {
            super(identity);
        }

        private BuilderImpl(DefaultAsyncSignRequest<IdentityT> request) {
            properties(request.properties);
            identity(request.identity);
            payload(request.payload);
            request(request.request);
        }

        @Override
        public AsyncSignRequest<IdentityT> build() {
            return new DefaultAsyncSignRequest<>(this);
        }
    }
}
