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
import software.amazon.awssdk.http.auth.spi.AsyncHttpSignRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.utils.ToString;

@SdkInternalApi
public final class DefaultAsyncHttpSignRequest<IdentityT extends Identity>
    extends DefaultHttpSignRequest<Publisher<ByteBuffer>, IdentityT> implements AsyncHttpSignRequest<IdentityT> {

    private DefaultAsyncHttpSignRequest(BuilderImpl<IdentityT> builder) {
        super(builder);
    }

    @Override
    public String toString() {
        return ToString.builder("AsyncHttpSignRequest")
                       .add("request", request)
                       .add("identity", identity)
                       .add("properties", properties)
                       .build();
    }

    @SdkInternalApi
    public static final class BuilderImpl<IdentityT extends Identity>
        extends DefaultHttpSignRequest.BuilderImpl<BuilderImpl<IdentityT>, Publisher<ByteBuffer>, IdentityT>
        implements AsyncHttpSignRequest.Builder<IdentityT> {

        // Used to enable consumer builder pattern in HttpSigner.signAsync()
        public BuilderImpl() {
        }

        // Used by AsyncHttpSignRequest#builder() where identity is passed as parameter, to avoid having to pass Class<IdentityT>.
        public BuilderImpl(IdentityT identity) {
            super(identity);
        }

        @Override
        public AsyncHttpSignRequest<IdentityT> build() {
            return new DefaultAsyncHttpSignRequest<>(this);
        }
    }
}
