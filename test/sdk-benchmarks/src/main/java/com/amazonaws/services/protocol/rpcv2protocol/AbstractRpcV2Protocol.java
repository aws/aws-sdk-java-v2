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
package com.amazonaws.services.protocol.rpcv2protocol;



import com.amazonaws.services.protocol.rpcv2protocol.model.*;
import com.amazonaws.*;

/**
 * Abstract implementation of {@code RpcV2Protocol}. Convenient method forms pass through to the corresponding overload
 * that takes a request object, which throws an {@code UnsupportedOperationException}.
 */

public class AbstractRpcV2Protocol implements RpcV2Protocol {

    protected AbstractRpcV2Protocol() {
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
    public EmptyInputOutputResult emptyInputOutput(EmptyInputOutputRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Float16Result float16(Float16Request request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FractionalSecondsResult fractionalSeconds(FractionalSecondsRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GreetingWithErrorsResult greetingWithErrors(GreetingWithErrorsRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NoInputOutputResult noInputOutput(NoInputOutputRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OptionalInputOutputResult optionalInputOutput(OptionalInputOutputRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RecursiveShapesResult recursiveShapes(RecursiveShapesRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RpcV2CborDenseMapsResult rpcV2CborDenseMaps(RpcV2CborDenseMapsRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RpcV2CborListsResult rpcV2CborLists(RpcV2CborListsRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleScalarPropertiesResult simpleScalarProperties(SimpleScalarPropertiesRequest request) {
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
