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
import software.amazon.awssdk.custom.s3.transfer.CompletedUpload;
import software.amazon.awssdk.custom.s3.transfer.Upload;

@SdkInternalApi
public final class DefaultUpload implements Upload {
    private final CompletableFuture<CompletedUpload> completionFeature;

    public DefaultUpload(CompletableFuture<CompletedUpload> completionFeature) {
        this.completionFeature = completionFeature;
    }

    @Override
    public CompletableFuture<CompletedUpload> completionFuture() {
        return completionFeature;
    }
}
