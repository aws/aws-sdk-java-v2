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
import software.amazon.awssdk.http.auth.spi.SyncSignedHttpRequest;
import software.amazon.awssdk.utils.ToString;

@SdkInternalApi
public final class DefaultSyncSignedHttpRequest
    extends DefaultSignedHttpRequest<ContentStreamProvider> implements SyncSignedHttpRequest {

    private DefaultSyncSignedHttpRequest(BuilderImpl builder) {
        super(builder);
    }

    @Override
    public String toString() {
        return ToString.builder("SyncSignedHttpRequest")
                       .add("request", request)
                       .build();
    }

    @SdkInternalApi
    public static final class BuilderImpl
        extends DefaultSignedHttpRequest.BuilderImpl<SyncSignedHttpRequest.Builder, ContentStreamProvider>
        implements SyncSignedHttpRequest.Builder {

        @Override
        public SyncSignedHttpRequest build() {
            return new DefaultSyncSignedHttpRequest(this);
        }
    }
}
