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

import javax.annotation.Generated;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.waiters.SdkFunction;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;

@SdkInternalApi
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class DescribeStacksFunction implements SdkFunction<DescribeStacksRequest, DescribeStacksResponse> {

    /**
     * Represents the service client
     */
    private final CloudFormationClient client;

    /**
     * Constructs a new DescribeStacksFunction with the given client
     * 
     * @param client
     *        Service client
     */
    public DescribeStacksFunction(CloudFormationClient client) {
        this.client = client;
    }

    /**
     * Makes a call to the operation specified by the waiter by taking the corresponding request and returns the
     * corresponding result
     * 
     * @param describeStacksRequest
     *        Corresponding request for the operation
     * @return Corresponding result of the operation
     */
    @Override
    public DescribeStacksResponse apply(DescribeStacksRequest describeStacksRequest) {
        return client.describeStacks(describeStacksRequest);
    }
}
