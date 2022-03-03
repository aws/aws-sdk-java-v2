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

package software.amazon.awssdk.services.s3.internal.extensions;

import static software.amazon.awssdk.utils.CompletableFutureUtils.failedFuture;
import static software.amazon.awssdk.utils.CompletableFutureUtils.forwardExceptionTo;
import static software.amazon.awssdk.utils.CompletableFutureUtils.unwrap;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.extensions.S3AsyncClientSdkExtension;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.utils.Validate;

public class DefaultS3AsyncClientSdkExtension implements S3AsyncClientSdkExtension {

    private final S3AsyncClient s3;

    public DefaultS3AsyncClientSdkExtension(S3AsyncClient s3) {
        this.s3 = Validate.notNull(s3, "s3");
    }

    @Override
    public CompletableFuture<Boolean> doesBucketExist(String bucket) {
        try {
            Validate.notEmpty(bucket, "bucket");
            CompletableFuture<Boolean> returnFuture = new CompletableFuture<>();
            CompletableFuture<HeadBucketResponse> responseFuture = s3.headBucket(r -> r.bucket(bucket));
            forwardExceptionTo(returnFuture, responseFuture);
            responseFuture.whenComplete((r, t) -> {
                t = unwrap(t);
                if (t == null) {
                    returnFuture.complete(true);
                } else if (t instanceof NoSuchBucketException) {
                    returnFuture.complete(false);
                } else {
                    returnFuture.completeExceptionally(t);
                }
            });
            return returnFuture;
        } catch (Throwable t) {
            return failedFuture(t);
        }
    }

    @Override
    public CompletableFuture<Boolean> doesObjectExist(String bucket, String key) {
        try {
            Validate.notEmpty(bucket, "bucket");
            Validate.notEmpty(bucket, "key");
            CompletableFuture<Boolean> returnFuture = new CompletableFuture<>();
            CompletableFuture<HeadObjectResponse> responseFuture = s3.headObject(r -> r.bucket(bucket).key(key));
            forwardExceptionTo(returnFuture, responseFuture);
            responseFuture.whenComplete((r, t) -> {
                t = unwrap(t);
                if (t == null) {
                    returnFuture.complete(true);
                } else if (t instanceof NoSuchKeyException) {
                    returnFuture.complete(false);
                } else {
                    returnFuture.completeExceptionally(t);
                }
            });
            return returnFuture;
        } catch (Throwable t) {
            return failedFuture(t);
        }
    }
}
