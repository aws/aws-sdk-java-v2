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

package software.amazon.awssdk.transfer.s3.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.crt.s3.CrtS3RuntimeException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.InvalidObjectStateException;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;
import software.amazon.awssdk.services.s3.model.ObjectAlreadyInActiveTierErrorException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.StringUtils;

@SdkInternalApi
public class CrtErrorHandler {

    private final Map<String, S3Exception.Builder> s3ExceptionBuilderMap;

    public CrtErrorHandler() {
        s3ExceptionBuilderMap = getS3ExceptionBuilderMap();
    }

    /**
     * This class transform a crtTRunTimeException to the S3 service Exceptions.
     * CrtS3RuntimeException are the exceptions generated due to failures in CRTClient due to S3 Service errors.
     *
     * @param crtRuntimeException Exception that is thrown by CrtClient.
     * @return
     */
    public Exception transformException(Exception crtRuntimeException) {
        Optional<CrtS3RuntimeException> crtS3RuntimeExceptionOptional = getCrtS3RuntimeException(crtRuntimeException);
        Exception exception = crtS3RuntimeExceptionOptional
                .filter(CrtErrorHandler::isErrorDetailsAvailable)
                .map(e -> getServiceSideException(e))
                .orElse(SdkClientException.create(crtRuntimeException.getMessage(), crtRuntimeException));
        return exception;
    }

    private Exception getServiceSideException(CrtS3RuntimeException e) {
        if (s3ExceptionBuilderMap.get(e.getAwsErrorCode()) != null) {
            return s3ExceptionBuilderMap.get(e.getAwsErrorCode())
                    .awsErrorDetails(
                            AwsErrorDetails.builder().errorCode(e.getAwsErrorCode())
                                    .errorMessage(e.getAwsErrorMessage()).build())
                    .cause(e)
                    .message(e.getMessage())
                    .statusCode(e.getStatusCode())
                    .build();
        }
        return S3Exception.builder().statusCode(e.getStatusCode()).message(e.getMessage()).cause(e).build();
    }

    /**
     * This method checks if the exception has the required details to transform to S3 Exception.
     * @param crtS3RuntimeException the exception that needs to be checked
     * @return true if exception has the required details.
     */
    private static boolean isErrorDetailsAvailable(CrtS3RuntimeException crtS3RuntimeException) {
        return StringUtils.isNotBlank(crtS3RuntimeException.getAwsErrorCode());
    }

    /**
     * Checks if the Exception or its cause is of CrtS3RuntimeException.
     * The S3 Service related exception are in the form of CrtS3RuntimeException.
     * @param crtRuntimeException
     * @return CrtS3RuntimeException else return empty,
     */
    private Optional<CrtS3RuntimeException> getCrtS3RuntimeException(Exception crtRuntimeException) {
        if (crtRuntimeException instanceof CrtS3RuntimeException) {
            return Optional.of((CrtS3RuntimeException) crtRuntimeException);
        }
        Throwable cause = crtRuntimeException.getCause();
        if (cause instanceof CrtS3RuntimeException) {
            return Optional.of((CrtS3RuntimeException) cause);
        }
        return Optional.empty();
    }


    /**
     * Gets a Mapping of AWSErrorCode to its corresponding S3 Exception Builders.
     *
     * @return
     */
    private Map<String, S3Exception.Builder> getS3ExceptionBuilderMap() {
        Map<String, S3Exception.Builder> s3ExceptionBuilderMap = new HashMap<>();
        s3ExceptionBuilderMap.put("ObjectAlreadyInActiveTierError", ObjectAlreadyInActiveTierErrorException.builder());
        s3ExceptionBuilderMap.put("NoSuchUpload", NoSuchUploadException.builder());
        s3ExceptionBuilderMap.put("BucketAlreadyExists", BucketAlreadyExistsException.builder());
        s3ExceptionBuilderMap.put("BucketAlreadyOwnedByYou", BucketAlreadyOwnedByYouException.builder());
        s3ExceptionBuilderMap.put("InvalidObjectState", InvalidObjectStateException.builder());
        s3ExceptionBuilderMap.put("NoSuchBucket", NoSuchBucketException.builder());
        s3ExceptionBuilderMap.put("NoSuchKey", NoSuchKeyException.builder());
        return s3ExceptionBuilderMap;
    }
}