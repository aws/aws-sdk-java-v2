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

import software.amazon.awssdk.SdkRequest;
import software.amazon.awssdk.handlers.AwsExecutionAttributes;
import software.amazon.awssdk.interceptor.Context;
import software.amazon.awssdk.interceptor.ExecutionAttributes;
import software.amazon.awssdk.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.BucketUtils;
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
                                    .createBucketConfiguration(CreateBucketConfiguration.builder()
                                                                                        .locationConstraint(region.value())
                                                                                        .build())
                                    .build();
            }
        }

        return sdkRequest;
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
