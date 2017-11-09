/*
 * Copyright 2012-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.dynamodb.waiters;

import software.amazon.awssdk.core.AmazonServiceException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterState;

import software.amazon.awssdk.services.dynamodb.model.*;

import javax.annotation.Generated;

@SdkInternalApi
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
class TableNotExists {

    static class IsResourceNotFoundExceptionMatcher extends WaiterAcceptor<DescribeTableResponse, AmazonServiceException> {
        /**
         * Takes the response exception and determines whether this exception matches the expected exception, by
         * comparing the respective error codes.
         * 
         * @param e
         *        Response Exception
         * @return True if it matches, False otherwise
         */
        @Override
        public boolean matches(AmazonServiceException e) {
            return "ResourceNotFoundException".equals(e.getErrorCode());
        }

        /**
         * Represents the current waiter state in the case where resource state matches the expected state
         * 
         * @return Corresponding state of the waiter
         */
        @Override
        public WaiterState getState() {
            return WaiterState.SUCCESS;
        }
    }
}
