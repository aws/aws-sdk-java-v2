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
import software.amazon.awssdk.core.waiters.AcceptorPathMatcher;
import software.amazon.awssdk.core.waiters.ObjectMapperSingleton;
import software.amazon.awssdk.services.dynamodb.model.*;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import javax.annotation.Generated;

import io.burt.jmespath.Expression;
import io.burt.jmespath.jackson.JacksonRuntime;

@SdkInternalApi
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
class TableExists {

    static class IsACTIVEMatcher extends WaiterAcceptor<DescribeTableResponse, AmazonServiceException> {
        private static final JsonNode EXPECTED_RESULT;

        static {
            try {
                EXPECTED_RESULT = ObjectMapperSingleton.getObjectMapper().readTree("\"ACTIVE\"");
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        private static final Expression<JsonNode> AST = new JacksonRuntime().compile("table.tableStatus");

        /**
         * Takes the result and determines whether the state of the resource matches the expected state. To determine
         * the current state of the resource, JmesPath expression is evaluated and compared against the expected result.
         * 
         * @param result
         *        Corresponding result of the operation
         * @return True if current state of the resource matches the expected state, False otherwise
         */
        @Override
        public boolean matches(DescribeTableResponse result) {
            JsonNode queryNode = ObjectMapperSingleton.getObjectMapper().valueToTree(result);
            JsonNode finalResult = AST.search(queryNode);
            return AcceptorPathMatcher.path(EXPECTED_RESULT, finalResult);
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
            return WaiterState.RETRY;
        }
    }
}
