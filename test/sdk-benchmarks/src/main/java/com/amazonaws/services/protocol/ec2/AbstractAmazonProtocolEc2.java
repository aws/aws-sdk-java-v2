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
import com.amazonaws.*;

/**
 * Abstract implementation of {@code AmazonProtocolEc2}. Convenient method forms pass through to the corresponding
 * overload that takes a request object, which throws an {@code UnsupportedOperationException}.
 */

public class AbstractAmazonProtocolEc2 implements AmazonProtocolEc2 {

    protected AbstractAmazonProtocolEc2() {
    }

    @Override
    public void setEndpoint(String endpoint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRegion(com.amazonaws.regions.Region region) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AllTypesResult allTypes(AllTypesRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Ec2TypesResult ec2Types(Ec2TypesRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IdempotentOperationResult idempotentOperation(IdempotentOperationRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request) {
        throw new UnsupportedOperationException();
    }

}
