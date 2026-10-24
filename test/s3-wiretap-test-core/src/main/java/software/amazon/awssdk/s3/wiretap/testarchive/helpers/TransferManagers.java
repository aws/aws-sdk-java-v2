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

package software.amazon.awssdk.s3.wiretap.testarchive.helpers;

import java.util.function.Consumer;
import software.amazon.awssdk.s3.wiretap.client.ConfiguredClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

public final class TransferManagers {

    private TransferManagers() {
    }

    public static S3TransferManager over(ConfiguredClient c) {
        return over(c, b -> { });
    }

    public static S3TransferManager over(ConfiguredClient c, Consumer<S3TransferManager.Builder> configure) {
        if (c.asyncClient() == null) {
            throw new IllegalStateException(
                    "S3TransferManager needs an async client, but config [" + c.config().describe()
                            + "] built a sync client; sweep an async HttpClientFeature group");
        }
        S3TransferManager.Builder builder = S3TransferManager.builder().s3Client(c.asyncClient());
        configure.accept(builder);
        return builder.build();
    }
}
