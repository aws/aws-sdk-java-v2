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

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.custom.s3.transfer.CompletedDownload;
import software.amazon.awssdk.custom.s3.transfer.Download;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@SdkInternalApi
public final class DefaultDownload implements Download {
    private final CompletableFuture<CompletedDownload> completionFuture;
    private final CompletableFuture<?> partsCompleteFuture;

    public DefaultDownload(CompletableFuture<?> partsCompleteFuture) {
        this.partsCompleteFuture = partsCompleteFuture;
        this.completionFuture = this.partsCompleteFuture.thenApply(ignored -> null);
        CompletableFutureUtils.forwardExceptionTo(this.completionFuture, this.partsCompleteFuture);
    }

    @Override
    public CompletableFuture<CompletedDownload> completionFuture() {
        return completionFuture;
    }
}
