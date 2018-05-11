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

package software.amazon.awssdk.awscore.protocol.json;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.protocol.json.JsonErrorUnmarshaller;

/**
 * Unmarshaller for JSON error responses from AWS services.
 */
@SdkInternalApi
@ThreadSafe
public class AwsJsonErrorUnmarshaller extends JsonErrorUnmarshaller<AwsServiceException> {

    public static final AwsJsonErrorUnmarshaller DEFAULT_UNMARSHALLER = new AwsJsonErrorUnmarshaller(
        AwsServiceException.class, null);

    private final String handledErrorCode;

    /**
     * @param exceptionClass   Exception class this unmarshaller will attempt to deserialize error response into
     * @param handledErrorCode AWS error code that this unmarshaller handles. Pass null to handle all exceptions
     */
    public AwsJsonErrorUnmarshaller(Class<? extends AwsServiceException> exceptionClass, String handledErrorCode) {
        super(exceptionClass);
        this.handledErrorCode = handledErrorCode;
    }

    /**
     * @param actualErrorCode Actual AWS error code found in the error response.
     * @return True if the actualErrorCode can be handled by this unmarshaller, false otherwise
     */
    public boolean matchErrorCode(String actualErrorCode) {
        return handledErrorCode == null || handledErrorCode.equals(actualErrorCode);
    }

}
