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

package software.amazon.awssdk.services.s3.handlers;

import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.services.s3.BucketUtils;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

public class CreateBucketRequestHandler extends RequestHandler {

    @Override
    @ReviewBeforeRelease("Automatically set location constraint to the bucket region if not provided. Also" +
                         " perhaps remove the location constraint from the model so that is may only be" +
                         " the current region.")
    public AmazonWebServiceRequest beforeMarshalling(AmazonWebServiceRequest request) {

        if (request instanceof CreateBucketRequest) {
            CreateBucketRequest createBucketRequest = (CreateBucketRequest) request;
            validateBucketNameIsS3Compatible(createBucketRequest.bucket());
        }

        return request;
    }

    /**
     * Validates that the name of the bucket being requested to be created
     * is a valid S3 bucket name according to their guidelines. If the bucket
     * name is not valid, an {@link IllegalArgumentException} is thrown. See
     * {@link BucketUtils#isValidDnsBucketName(String, boolean)} for additional
     * details.
     *
     * @param bucketName Name of the bucket
     */
    private void validateBucketNameIsS3Compatible(String bucketName) {
        BucketUtils.isValidDnsBucketName(bucketName, true);
    }
}
