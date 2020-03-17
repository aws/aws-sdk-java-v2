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

package software.amazon.awssdk.services.s3.internal.handlers;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Translate S3Exception for the API calls that do not contain the detailed error code.
 */
@SdkInternalApi
public final class ExceptionTranslationInterceptor implements ExecutionInterceptor {

    @Override
    public Throwable modifyException(Context.FailedExecution context, ExecutionAttributes executionAttributes) {

        if (!isS3Exception404(context.exception()) || !isHeadRequest(context.request())) {
            return context.exception();
        }

        String message = context.exception().getMessage();
        S3Exception exception = (S3Exception) (context.exception());

        String requestIdFromHeader = exception.awsErrorDetails()
                                              .sdkHttpResponse()
                                              .firstMatchingHeader("x-amz-request-id")
                                              .orElse(null);

        String requestId = Optional.ofNullable(exception.requestId()).orElse(requestIdFromHeader);

        AwsErrorDetails errorDetails = exception.awsErrorDetails();

        if (context.request() instanceof HeadObjectRequest) {
            return NoSuchKeyException.builder()
                                    .awsErrorDetails(fillErrorDetails(errorDetails, "NoSuchKey",
                                                                      "The specified key does not exist."))
                                    .statusCode(404)
                                    .requestId(requestId)
                                    .message(message)
                                    .build();
        }

        if (context.request() instanceof HeadBucketRequest) {
            return NoSuchBucketException.builder()
                                       .awsErrorDetails(fillErrorDetails(errorDetails, "NoSuchBucket",
                                                                         "The specified bucket does not exist."))
                                       .statusCode(404)
                                       .requestId(requestId)
                                       .message(message)
                                       .build();
        }

        return context.exception();
    }

    private AwsErrorDetails fillErrorDetails(AwsErrorDetails original, String errorCode, String errorMessage) {
        return original.toBuilder().errorMessage(errorMessage).errorCode(errorCode).build();
    }

    private boolean isHeadRequest(SdkRequest request) {
        return (request instanceof HeadObjectRequest || request instanceof HeadBucketRequest);
    }

    private boolean isS3Exception404(Throwable thrown) {
        if (!(thrown instanceof S3Exception)) {
            return false;
        }

        return ((S3Exception) thrown).statusCode() == 404;
    }
}
