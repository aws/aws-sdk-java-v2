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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.extensions.S3ClientSdkExtension;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public class DefaultS3ClientSdkExtension implements S3ClientSdkExtension {

    private final S3Client s3;

    public DefaultS3ClientSdkExtension(S3Client s3) {
        this.s3 = Validate.notNull(s3, "s3");
    }

    @Override
    public boolean doesBucketExist(String bucket) {
        Validate.notNull(bucket, "bucket");
        try {
            s3.headBucket(r -> r.bucket(bucket));
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
    }
}
