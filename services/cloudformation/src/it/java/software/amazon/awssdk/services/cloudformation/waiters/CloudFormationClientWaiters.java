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

package software.amazon.awssdk.services.cloudformation.waiters;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.AmazonServiceException;
import software.amazon.awssdk.core.waiters.FixedDelayStrategy;
import software.amazon.awssdk.core.waiters.MaxAttemptsRetryStrategy;
import software.amazon.awssdk.core.waiters.PollingStrategy;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.core.waiters.WaiterBuilder;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;

@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class CloudFormationClientWaiters {

    /**
     * Represents the service client
     */
    private final CloudFormationClient client;

    private final ExecutorService executorService = Executors.newFixedThreadPool(50);

    /**
     * Constructs a new CloudFormationClientWaiters with the given client
     * 
     * @param client
     *        Service client
     */
    @SdkInternalApi
    public CloudFormationClientWaiters(CloudFormationClient client) {
        this.client = client;
    }

    /**
     * Builds a StackCreateComplete waiter by using custom parameters waiterParameters and other parameters defined in
     * the waiters specification, and then polls until it determines whether the resource entered the desired state or
     * not, where polling criteria is bound by either default polling strategy or custom polling strategy.
     */
    public Waiter<DescribeStacksRequest> stackCreateComplete() {

        return new WaiterBuilder<DescribeStacksRequest, DescribeStacksResponse, AmazonServiceException>()
                .withSdkFunction(new DescribeStacksFunction(client))
                .withAcceptors(new StackCreateComplete.IsCREATE_COMPLETEMatcher(),
                        new StackCreateComplete.IsCREATE_FAILEDMatcher(), new StackCreateComplete.IsDELETE_COMPLETEMatcher(),
                        new StackCreateComplete.IsDELETE_FAILEDMatcher(), new StackCreateComplete.IsROLLBACK_FAILEDMatcher(),
                        new StackCreateComplete.IsROLLBACK_COMPLETEMatcher(), new StackCreateComplete.IsValidationErrorMatcher())
                .withDefaultPollingStrategy(new PollingStrategy(new MaxAttemptsRetryStrategy(120), new FixedDelayStrategy(30)))
                .withExecutorService(executorService).build();
    }

    /**
     * Builds a StackUpdateComplete waiter by using custom parameters waiterParameters and other parameters defined in
     * the waiters specification, and then polls until it determines whether the resource entered the desired state or
     * not, where polling criteria is bound by either default polling strategy or custom polling strategy.
     */
    public Waiter<DescribeStacksRequest> stackUpdateComplete() {

        return new WaiterBuilder<DescribeStacksRequest, DescribeStacksResponse, AmazonServiceException>()
                .withSdkFunction(new DescribeStacksFunction(client))
                .withAcceptors(new StackUpdateComplete.IsUPDATE_COMPLETEMatcher(),
                        new StackUpdateComplete.IsUPDATE_FAILEDMatcher(),
                        new StackUpdateComplete.IsUPDATE_ROLLBACK_FAILEDMatcher(),
                        new StackUpdateComplete.IsUPDATE_ROLLBACK_COMPLETEMatcher(),
                        new StackUpdateComplete.IsValidationErrorMatcher())
                .withDefaultPollingStrategy(new PollingStrategy(new MaxAttemptsRetryStrategy(120), new FixedDelayStrategy(30)))
                .withExecutorService(executorService).build();
    }
}
