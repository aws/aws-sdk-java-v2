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

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.custom.s3.transfer.S3TransferManager;
import software.amazon.awssdk.services.s3.S3CrtAsyncClient;
import software.amazon.awssdk.utils.SdkAutoCloseable;

@SdkInternalApi
public final class DefaultS3TransferManager implements S3TransferManager {
    private final S3CrtAsyncClient s3CrtAsyncClient;
    private final List<SdkAutoCloseable> closables = new ArrayList<>();

    public DefaultS3TransferManager(DefaultBuilder builder) {
        if (builder.s3CrtAsyncClient == null) {
            s3CrtAsyncClient = S3CrtAsyncClient.builder()
                                               .build();
            closables.add(s3CrtAsyncClient);
        } else {
            s3CrtAsyncClient = builder.s3CrtAsyncClient;
        }
    }

    @Override
    public void close() {
        closables.forEach(SdkAutoCloseable::close);
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    private static class DefaultBuilder implements S3TransferManager.Builder {
        private S3CrtAsyncClient s3CrtAsyncClient;


        @Override
        public Builder s3CrtClient(S3CrtAsyncClient s3CrtAsyncClient) {
            this.s3CrtAsyncClient = s3CrtAsyncClient;
            return this;
        }

        @Override
        public S3TransferManager build() {
            return new DefaultS3TransferManager(this);
        }
    }
}
