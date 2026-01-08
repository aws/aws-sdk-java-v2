/*
 * Copyright 2021-2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.services.protocol.ec2;



import com.amazonaws.services.protocol.ec2.model.*;

/**
 * Abstract implementation of {@code AmazonProtocolEc2Async}. Convenient method forms pass through to the corresponding
 * overload that takes a request object and an {@code AsyncHandler}, which throws an
 * {@code UnsupportedOperationException}.
 */

public class AbstractAmazonProtocolEc2Async extends AbstractAmazonProtocolEc2 implements AmazonProtocolEc2Async {

    protected AbstractAmazonProtocolEc2Async() {
    }

    @Override
    public java.util.concurrent.Future<AllTypesResult> allTypesAsync(AllTypesRequest request) {

        return allTypesAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<AllTypesResult> allTypesAsync(AllTypesRequest request,
            com.amazonaws.handlers.AsyncHandler<AllTypesRequest, AllTypesResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<Ec2TypesResult> ec2TypesAsync(Ec2TypesRequest request) {

        return ec2TypesAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<Ec2TypesResult> ec2TypesAsync(Ec2TypesRequest request,
            com.amazonaws.handlers.AsyncHandler<Ec2TypesRequest, Ec2TypesResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<IdempotentOperationResult> idempotentOperationAsync(IdempotentOperationRequest request) {

        return idempotentOperationAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<IdempotentOperationResult> idempotentOperationAsync(IdempotentOperationRequest request,
            com.amazonaws.handlers.AsyncHandler<IdempotentOperationRequest, IdempotentOperationResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

}
