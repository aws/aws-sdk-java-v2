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

package software.amazon.awssdk.awscore.exception;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.internal.AwsErrorCodes;
import software.amazon.awssdk.core.exception.SdkServiceException;

/**
 * Extension of {@link SdkServiceException} that represents an error response returned
 * by an Amazon web service.
 *
 * <p>
 * AmazonServiceException provides callers several pieces of
 * information that can be used to obtain more information about the error and
 * why it occurred. In particular, the errorType field can be used to determine
 * if the caller's request was invalid, or the service encountered an error on
 * the server side while processing it.
 *
 * @see SdkServiceException
 */
@SdkPublicApi
public class AwsServiceException extends SdkServiceException {

    public AwsServiceException(String errorMessage) {
        super(errorMessage);
    }

    public AwsServiceException(String errorMessage, Exception cause) {
        super(errorMessage, cause);
    }

    @Override
    public boolean isClockSkewException() {
        return Optional.ofNullable(errorCode())
                       .map(AwsErrorCodes.CLOCK_SKEW_ERROR_CODES::contains)
                       .orElse(false);
    }

    @Override
    public boolean isThrottlingException() {
        return super.isThrottlingException() ||
               Optional.ofNullable(errorCode())
                       .map(AwsErrorCodes.THROTTLING_ERROR_CODES::contains)
                       .orElse(false);
    }

}
