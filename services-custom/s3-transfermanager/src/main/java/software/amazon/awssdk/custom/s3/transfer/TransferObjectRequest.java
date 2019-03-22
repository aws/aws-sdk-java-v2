/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.custom.s3.transfer;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A request representing a transfer of an object to and from S3.
 */
@SdkPublicApi
public abstract class TransferObjectRequest extends TransferRequest {
    protected TransferObjectRequest(BuilderImpl builder) {
        super(builder);
    }

    @Override
    public abstract Builder toBuilder();

    protected interface Builder extends TransferRequest.Builder {
        @Override
        TransferObjectRequest build();
    }

    protected abstract static class BuilderImpl extends TransferRequest.BuilderImpl {
        protected BuilderImpl() {
        }

        protected BuilderImpl(TransferObjectRequest other) {
            super(other);
        }
    }
}
