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

import javax.annotation.Generated;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.waiters.SdkFunction;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;

@SdkInternalApi
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class DescribeTableFunction implements SdkFunction<DescribeTableRequest, DescribeTableResponse> {

    /**
     * Represents the service client
     */
    private final DynamoDBClient client;

    /**
     * Constructs a new DescribeTableFunction with the given client
     * 
     * @param client
     *        Service client
     */
    public DescribeTableFunction(DynamoDBClient client) {
        this.client = client;
    }

    /**
     * Makes a call to the operation specified by the waiter by taking the corresponding request and returns the
     * corresponding result
     * 
     * @param describeTableRequest
     *        Corresponding request for the operation
     * @return Corresponding result of the operation
     */
    @Override
    public DescribeTableResponse apply(DescribeTableRequest describeTableRequest) {
        return client.describeTable(describeTableRequest);
    }
}
