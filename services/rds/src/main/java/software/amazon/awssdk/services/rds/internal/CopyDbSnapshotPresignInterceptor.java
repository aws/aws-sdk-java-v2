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

package software.amazon.awssdk.services.rds.internal;

import java.time.Clock;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.services.rds.model.CopyDbSnapshotRequest;
import software.amazon.awssdk.services.rds.transform.CopyDbSnapshotRequestMarshaller;

/**
 * Handler for pre-signing {@link CopyDbSnapshotRequest}.
 */
@SdkInternalApi
public final class CopyDbSnapshotPresignInterceptor extends RdsPresignInterceptor<CopyDbSnapshotRequest> {

    public static final CopyDbSnapshotRequestMarshaller MARSHALLER = new CopyDbSnapshotRequestMarshaller(PROTOCOL_FACTORY);

    public CopyDbSnapshotPresignInterceptor() {
        super(CopyDbSnapshotRequest.class);
    }

    @SdkTestInternalApi
    CopyDbSnapshotPresignInterceptor(Clock signingDateOverride) {
        super(CopyDbSnapshotRequest.class, signingDateOverride);
    }

    @Override
    protected PresignableRequest adaptRequest(final CopyDbSnapshotRequest originalRequest) {
        return new PresignableRequest() {
            @Override
            public String getSourceRegion() {
                return originalRequest.sourceRegion();
            }

            @Override
            public SdkHttpFullRequest marshall() {
                return MARSHALLER.marshall(originalRequest);
            }
        };
    }
}
