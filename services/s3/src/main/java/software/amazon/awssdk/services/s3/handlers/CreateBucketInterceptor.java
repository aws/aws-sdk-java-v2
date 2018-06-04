/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import software.amazon.awssdk.auth.AwsExecutionAttributes;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.internal.BucketUtils;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

public class CreateBucketInterceptor implements ExecutionInterceptor {

    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        SdkRequest sdkRequest = context.request();

        if (sdkRequest instanceof CreateBucketRequest) {
            CreateBucketRequest request = (CreateBucketRequest) sdkRequest;
            validateBucketNameIsS3Compatible(request.bucket());

            if (request.createBucketConfiguration() == null || request.createBucketConfiguration().locationConstraint() == null) {
                Region region = executionAttributes.getAttribute(AwsExecutionAttributes.AWS_REGION);
                sdkRequest = request.toBuilder()
                                    .createBucketConfiguration(toLocationConstraint(region))
                                    .build();
            }
        }

        return sdkRequest;
    }

    private CreateBucketConfiguration toLocationConstraint(Region region) {
        if (region.equals(Region.US_EAST_1)) {
            // us-east-1 requires no location restraint. See http://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketPUT.html
            return null;
        }
        return CreateBucketConfiguration.builder()
                                        .locationConstraint(region.value())
                                        .build();
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
