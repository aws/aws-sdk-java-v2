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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.utils.ToString;

@SdkInternalApi
public final class DefaultSyncSignRequest<IdentityT extends Identity>
    extends DefaultSignRequest<ContentStreamProvider, IdentityT> implements SyncSignRequest<IdentityT> {

    private DefaultSyncSignRequest(BuilderImpl<IdentityT> builder) {
        super(builder);
    }

    public static <IdentityT extends Identity> SyncSignRequest.Builder<IdentityT> builder() {
        return new BuilderImpl<>();
    }

    public static <IdentityT extends Identity> SyncSignRequest.Builder<IdentityT> builder(IdentityT identity) {
        return new BuilderImpl<>(identity);
    }

    @Override
    public String toString() {
        return ToString.builder("SyncSignRequest")
            .add("request", request)
            .add("identity", identity)
            .add("properties", properties)
            .build();
    }

    @Override
    public SyncSignRequest.Builder<IdentityT> toBuilder() {
        return new BuilderImpl<>(this);
    }

    @SdkInternalApi
    public static final class BuilderImpl<IdentityT extends Identity>
        extends DefaultSignRequest.BuilderImpl<SyncSignRequest.Builder<IdentityT>, ContentStreamProvider, IdentityT>
        implements SyncSignRequest.Builder<IdentityT> {

        // Used to enable consumer builder pattern in HttpSigner.sign()
        private BuilderImpl() {
        }

        // Used by SyncSignRequest#builder() where identity is passed as parameter, to avoid having to pass Class<IdentityT>.
        private BuilderImpl(IdentityT identity) {
            super(identity);
        }

        private BuilderImpl(DefaultSyncSignRequest<IdentityT> request) {
            properties(request.properties);
            identity(request.identity);
            payload(request.payload);
            request(request.request);
        }

        @Override
        public SyncSignRequest<IdentityT> build() {
            return new DefaultSyncSignRequest<>(this);
        }
    }
}
