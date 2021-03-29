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

package software.amazon.awssdk.custom.s3.transfer.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.custom.s3.transfer.CompletedDownload;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@SdkInternalApi
public final class DefaultCompletedDownload implements CompletedDownload {
    private final GetObjectResponse response;

    private DefaultCompletedDownload(Builder builder) {
        this.response = builder.response;
    }

    @Override
    public GetObjectResponse response() {
        return response;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private GetObjectResponse response;

        public Builder response(GetObjectResponse response) {
            this.response = response;
            return this;
        }

        public CompletedDownload build() {
            return new DefaultCompletedDownload(this);
        }
    }
}
