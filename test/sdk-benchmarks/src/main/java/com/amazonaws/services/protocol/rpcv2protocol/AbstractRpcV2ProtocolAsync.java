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

/**
 * Abstract implementation of {@code RpcV2ProtocolAsync}. Convenient method forms pass through to the corresponding
 * overload that takes a request object and an {@code AsyncHandler}, which throws an
 * {@code UnsupportedOperationException}.
 */

public class AbstractRpcV2ProtocolAsync extends AbstractRpcV2Protocol implements RpcV2ProtocolAsync {

    protected AbstractRpcV2ProtocolAsync() {
    }

    @Override
    public java.util.concurrent.Future<EmptyInputOutputResult> emptyInputOutputAsync(EmptyInputOutputRequest request) {

        return emptyInputOutputAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<EmptyInputOutputResult> emptyInputOutputAsync(EmptyInputOutputRequest request,
            com.amazonaws.handlers.AsyncHandler<EmptyInputOutputRequest, EmptyInputOutputResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<Float16Result> float16Async(Float16Request request) {

        return float16Async(request, null);
    }

    @Override
    public java.util.concurrent.Future<Float16Result> float16Async(Float16Request request,
            com.amazonaws.handlers.AsyncHandler<Float16Request, Float16Result> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<FractionalSecondsResult> fractionalSecondsAsync(FractionalSecondsRequest request) {

        return fractionalSecondsAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<FractionalSecondsResult> fractionalSecondsAsync(FractionalSecondsRequest request,
            com.amazonaws.handlers.AsyncHandler<FractionalSecondsRequest, FractionalSecondsResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<GreetingWithErrorsResult> greetingWithErrorsAsync(GreetingWithErrorsRequest request) {

        return greetingWithErrorsAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<GreetingWithErrorsResult> greetingWithErrorsAsync(GreetingWithErrorsRequest request,
            com.amazonaws.handlers.AsyncHandler<GreetingWithErrorsRequest, GreetingWithErrorsResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<NoInputOutputResult> noInputOutputAsync(NoInputOutputRequest request) {

        return noInputOutputAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<NoInputOutputResult> noInputOutputAsync(NoInputOutputRequest request,
            com.amazonaws.handlers.AsyncHandler<NoInputOutputRequest, NoInputOutputResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<OptionalInputOutputResult> optionalInputOutputAsync(OptionalInputOutputRequest request) {

        return optionalInputOutputAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<OptionalInputOutputResult> optionalInputOutputAsync(OptionalInputOutputRequest request,
            com.amazonaws.handlers.AsyncHandler<OptionalInputOutputRequest, OptionalInputOutputResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<RecursiveShapesResult> recursiveShapesAsync(RecursiveShapesRequest request) {

        return recursiveShapesAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<RecursiveShapesResult> recursiveShapesAsync(RecursiveShapesRequest request,
            com.amazonaws.handlers.AsyncHandler<RecursiveShapesRequest, RecursiveShapesResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<RpcV2CborDenseMapsResult> rpcV2CborDenseMapsAsync(RpcV2CborDenseMapsRequest request) {

        return rpcV2CborDenseMapsAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<RpcV2CborDenseMapsResult> rpcV2CborDenseMapsAsync(RpcV2CborDenseMapsRequest request,
            com.amazonaws.handlers.AsyncHandler<RpcV2CborDenseMapsRequest, RpcV2CborDenseMapsResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<RpcV2CborListsResult> rpcV2CborListsAsync(RpcV2CborListsRequest request) {

        return rpcV2CborListsAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<RpcV2CborListsResult> rpcV2CborListsAsync(RpcV2CborListsRequest request,
            com.amazonaws.handlers.AsyncHandler<RpcV2CborListsRequest, RpcV2CborListsResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<SimpleScalarPropertiesResult> simpleScalarPropertiesAsync(SimpleScalarPropertiesRequest request) {

        return simpleScalarPropertiesAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<SimpleScalarPropertiesResult> simpleScalarPropertiesAsync(SimpleScalarPropertiesRequest request,
            com.amazonaws.handlers.AsyncHandler<SimpleScalarPropertiesRequest, SimpleScalarPropertiesResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

}
