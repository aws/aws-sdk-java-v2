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
import software.amazon.awssdk.core.AmazonServiceException;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.core.waiters.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class DynamoDBClientWaiters {

    /**
     * Represents the service client
     */
    private final DynamoDBClient client;

    private final ExecutorService executorService = Executors.newFixedThreadPool(50);

    /**
     * Constructs a new DynamoDBClientWaiters with the given client
     * 
     * @param client
     *        Service client
     */
    @SdkInternalApi
    public DynamoDBClientWaiters(DynamoDBClient client) {
        this.client = client;
    }

    /**
     * Builds a TableExists waiter by using custom parameters waiterParameters and other parameters defined in the
     * waiters specification, and then polls until it determines whether the resource entered the desired state or not,
     * where polling criteria is bound by either default polling strategy or custom polling strategy.
     */
    public Waiter<DescribeTableRequest> tableExists() {

        return new WaiterBuilder<DescribeTableRequest, DescribeTableResponse, AmazonServiceException>()
                .withSdkFunction(new DescribeTableFunction(client))
                .withAcceptors(new TableExists.IsACTIVEMatcher(), new TableExists.IsResourceNotFoundExceptionMatcher())
                .withDefaultPollingStrategy(new PollingStrategy(new MaxAttemptsRetryStrategy(25), new FixedDelayStrategy(20)))
                .withExecutorService(executorService).build();
    }

    /**
     * Builds a TableNotExists waiter by using custom parameters waiterParameters and other parameters defined in the
     * waiters specification, and then polls until it determines whether the resource entered the desired state or not,
     * where polling criteria is bound by either default polling strategy or custom polling strategy.
     */
    public Waiter<DescribeTableRequest> tableNotExists() {

        return new WaiterBuilder<DescribeTableRequest, DescribeTableResponse, AmazonServiceException>()
                .withSdkFunction(new DescribeTableFunction(client))
                .withAcceptors(new TableNotExists.IsResourceNotFoundExceptionMatcher())
                .withDefaultPollingStrategy(new PollingStrategy(new MaxAttemptsRetryStrategy(25), new FixedDelayStrategy(20)))
                .withExecutorService(executorService).build();
    }

}
