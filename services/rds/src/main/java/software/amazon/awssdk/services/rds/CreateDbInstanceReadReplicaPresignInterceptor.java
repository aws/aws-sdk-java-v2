/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.rds;

import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullRequestAdapter;
import software.amazon.awssdk.services.rds.model.CreateDBInstanceReadReplicaRequest;
import software.amazon.awssdk.services.rds.transform.CreateDBInstanceReadReplicaRequestMarshaller;
import software.amazon.awssdk.util.ImmutableObjectUtils;

/**
 * Handler for pre-signing {@link CreateDBInstanceReadReplicaRequest}.
 */
public class CreateDbInstanceReadReplicaPresignInterceptor extends RdsPresignInterceptor<CreateDBInstanceReadReplicaRequest> {
    public CreateDbInstanceReadReplicaPresignInterceptor() {
        super(CreateDBInstanceReadReplicaRequest.class);
    }

    @Override
    protected PresignableRequest adaptRequest(final CreateDBInstanceReadReplicaRequest originalRequest) {
        return new PresignableRequest() {
            @Override
            public void setPreSignedUrl(String preSignedUrl) {
                ImmutableObjectUtils.setObjectMember(originalRequest, "preSignedUrl", preSignedUrl);
            }

            @Override
            public String getSourceRegion() {
                return originalRequest.sourceRegion();
            }

            @Override
            public SdkHttpFullRequest.Builder marshall() {
                return SdkHttpFullRequestAdapter.toMutableHttpFullRequest(
                        new CreateDBInstanceReadReplicaRequestMarshaller().marshall(originalRequest));
            }
        };
    }
}
