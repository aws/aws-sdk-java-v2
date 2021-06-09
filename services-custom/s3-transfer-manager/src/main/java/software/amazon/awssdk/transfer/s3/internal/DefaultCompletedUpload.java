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

package software.amazon.awssdk.transfer.s3.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.CompletedUpload;

@SdkInternalApi
public final class DefaultCompletedUpload implements CompletedUpload {
    private final PutObjectResponse response;

    private DefaultCompletedUpload(BuilderImpl builder) {
        this.response = builder.response;
    }

    @Override
    public PutObjectResponse response() {
        return response;
    }

    public static class BuilderImpl implements Builder {
        private PutObjectResponse response;

        @Override
        public Builder response(PutObjectResponse response) {
            this.response = response;
            return this;
        }

        @Override
        public CompletedUpload build() {
            return new DefaultCompletedUpload(this);
        }
    }
}
