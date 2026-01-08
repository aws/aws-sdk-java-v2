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
 * Interface for accessing RpcV2Protocol asynchronously. Each asynchronous method will return a Java Future object
 * representing the asynchronous operation; overloads which accept an {@code AsyncHandler} can be used to receive
 * notification when an asynchronous operation completes.
 * <p>
 * <b>Note:</b> Do not directly implement this interface, new methods are added to it regularly. Extend from
 * {@link AbstractRpcV2ProtocolAsync} instead.
 * </p>
 */

public interface RpcV2ProtocolAsync extends RpcV2Protocol {

    /**
     * @param emptyInputOutputRequest
     * @return A Java Future containing the result of the EmptyInputOutput operation returned by the service.
     * @sample RpcV2ProtocolAsync.EmptyInputOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/EmptyInputOutput" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<EmptyInputOutputResult> emptyInputOutputAsync(EmptyInputOutputRequest emptyInputOutputRequest);

    /**
     * @param emptyInputOutputRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the EmptyInputOutput operation returned by the service.
     * @sample RpcV2ProtocolAsyncHandler.EmptyInputOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/EmptyInputOutput" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<EmptyInputOutputResult> emptyInputOutputAsync(EmptyInputOutputRequest emptyInputOutputRequest,
            com.amazonaws.handlers.AsyncHandler<EmptyInputOutputRequest, EmptyInputOutputResult> asyncHandler);

    /**
     * @param float16Request
     * @return A Java Future containing the result of the Float16 operation returned by the service.
     * @sample RpcV2ProtocolAsync.Float16
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/Float16" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<Float16Result> float16Async(Float16Request float16Request);

    /**
     * @param float16Request
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the Float16 operation returned by the service.
     * @sample RpcV2ProtocolAsyncHandler.Float16
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/Float16" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<Float16Result> float16Async(Float16Request float16Request,
            com.amazonaws.handlers.AsyncHandler<Float16Request, Float16Result> asyncHandler);

    /**
     * @param fractionalSecondsRequest
     * @return A Java Future containing the result of the FractionalSeconds operation returned by the service.
     * @sample RpcV2ProtocolAsync.FractionalSeconds
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/FractionalSeconds"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<FractionalSecondsResult> fractionalSecondsAsync(FractionalSecondsRequest fractionalSecondsRequest);

    /**
     * @param fractionalSecondsRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the FractionalSeconds operation returned by the service.
     * @sample RpcV2ProtocolAsyncHandler.FractionalSeconds
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/FractionalSeconds"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<FractionalSecondsResult> fractionalSecondsAsync(FractionalSecondsRequest fractionalSecondsRequest,
            com.amazonaws.handlers.AsyncHandler<FractionalSecondsRequest, FractionalSecondsResult> asyncHandler);

    /**
     * <p>
     * This operation has three possible return values:
     * </p>
     * <ol>
     * <li>A successful response in the form of GreetingWithErrorsOutput</li>
     * <li>An InvalidGreeting error.</li>
     * <li>A ComplexError error.</li>
     * </ol>
     * <p>
     * Implementations must be able to successfully take a response and properly deserialize successful and error
     * responses.
     * </p>
     * 
     * @param greetingWithErrorsRequest
     * @return A Java Future containing the result of the GreetingWithErrors operation returned by the service.
     * @sample RpcV2ProtocolAsync.GreetingWithErrors
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/GreetingWithErrors"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<GreetingWithErrorsResult> greetingWithErrorsAsync(GreetingWithErrorsRequest greetingWithErrorsRequest);

    /**
     * <p>
     * This operation has three possible return values:
     * </p>
     * <ol>
     * <li>A successful response in the form of GreetingWithErrorsOutput</li>
     * <li>An InvalidGreeting error.</li>
     * <li>A ComplexError error.</li>
     * </ol>
     * <p>
     * Implementations must be able to successfully take a response and properly deserialize successful and error
     * responses.
     * </p>
     * 
     * @param greetingWithErrorsRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the GreetingWithErrors operation returned by the service.
     * @sample RpcV2ProtocolAsyncHandler.GreetingWithErrors
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/GreetingWithErrors"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<GreetingWithErrorsResult> greetingWithErrorsAsync(GreetingWithErrorsRequest greetingWithErrorsRequest,
            com.amazonaws.handlers.AsyncHandler<GreetingWithErrorsRequest, GreetingWithErrorsResult> asyncHandler);

    /**
     * @param noInputOutputRequest
     * @return A Java Future containing the result of the NoInputOutput operation returned by the service.
     * @sample RpcV2ProtocolAsync.NoInputOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/NoInputOutput" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<NoInputOutputResult> noInputOutputAsync(NoInputOutputRequest noInputOutputRequest);

    /**
     * @param noInputOutputRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the NoInputOutput operation returned by the service.
     * @sample RpcV2ProtocolAsyncHandler.NoInputOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/NoInputOutput" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<NoInputOutputResult> noInputOutputAsync(NoInputOutputRequest noInputOutputRequest,
            com.amazonaws.handlers.AsyncHandler<NoInputOutputRequest, NoInputOutputResult> asyncHandler);

    /**
     * @param optionalInputOutputRequest
     * @return A Java Future containing the result of the OptionalInputOutput operation returned by the service.
     * @sample RpcV2ProtocolAsync.OptionalInputOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/OptionalInputOutput"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<OptionalInputOutputResult> optionalInputOutputAsync(OptionalInputOutputRequest optionalInputOutputRequest);

    /**
     * @param optionalInputOutputRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the OptionalInputOutput operation returned by the service.
     * @sample RpcV2ProtocolAsyncHandler.OptionalInputOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/OptionalInputOutput"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<OptionalInputOutputResult> optionalInputOutputAsync(OptionalInputOutputRequest optionalInputOutputRequest,
            com.amazonaws.handlers.AsyncHandler<OptionalInputOutputRequest, OptionalInputOutputResult> asyncHandler);

    /**
     * @param recursiveShapesRequest
     * @return A Java Future containing the result of the RecursiveShapes operation returned by the service.
     * @sample RpcV2ProtocolAsync.RecursiveShapes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/RecursiveShapes" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<RecursiveShapesResult> recursiveShapesAsync(RecursiveShapesRequest recursiveShapesRequest);

    /**
     * @param recursiveShapesRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the RecursiveShapes operation returned by the service.
     * @sample RpcV2ProtocolAsyncHandler.RecursiveShapes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/RecursiveShapes" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<RecursiveShapesResult> recursiveShapesAsync(RecursiveShapesRequest recursiveShapesRequest,
            com.amazonaws.handlers.AsyncHandler<RecursiveShapesRequest, RecursiveShapesResult> asyncHandler);

    /**
     * <p>
     * The example tests basic map serialization.
     * </p>
     * 
     * @param rpcV2CborDenseMapsRequest
     * @return A Java Future containing the result of the RpcV2CborDenseMaps operation returned by the service.
     * @sample RpcV2ProtocolAsync.RpcV2CborDenseMaps
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/RpcV2CborDenseMaps"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<RpcV2CborDenseMapsResult> rpcV2CborDenseMapsAsync(RpcV2CborDenseMapsRequest rpcV2CborDenseMapsRequest);

    /**
     * <p>
     * The example tests basic map serialization.
     * </p>
     * 
     * @param rpcV2CborDenseMapsRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the RpcV2CborDenseMaps operation returned by the service.
     * @sample RpcV2ProtocolAsyncHandler.RpcV2CborDenseMaps
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/RpcV2CborDenseMaps"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<RpcV2CborDenseMapsResult> rpcV2CborDenseMapsAsync(RpcV2CborDenseMapsRequest rpcV2CborDenseMapsRequest,
            com.amazonaws.handlers.AsyncHandler<RpcV2CborDenseMapsRequest, RpcV2CborDenseMapsResult> asyncHandler);

    /**
     * <p>
     * This test case serializes JSON lists for the following cases for both input and output:
     * </p>
     * <ol>
     * <li>Normal lists.</li>
     * <li>Normal sets.</li>
     * <li>Lists of lists.</li>
     * <li>Lists of structures.</li>
     * </ol>
     * 
     * @param rpcV2CborListsRequest
     * @return A Java Future containing the result of the RpcV2CborLists operation returned by the service.
     * @sample RpcV2ProtocolAsync.RpcV2CborLists
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/RpcV2CborLists" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<RpcV2CborListsResult> rpcV2CborListsAsync(RpcV2CborListsRequest rpcV2CborListsRequest);

    /**
     * <p>
     * This test case serializes JSON lists for the following cases for both input and output:
     * </p>
     * <ol>
     * <li>Normal lists.</li>
     * <li>Normal sets.</li>
     * <li>Lists of lists.</li>
     * <li>Lists of structures.</li>
     * </ol>
     * 
     * @param rpcV2CborListsRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the RpcV2CborLists operation returned by the service.
     * @sample RpcV2ProtocolAsyncHandler.RpcV2CborLists
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/RpcV2CborLists" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<RpcV2CborListsResult> rpcV2CborListsAsync(RpcV2CborListsRequest rpcV2CborListsRequest,
            com.amazonaws.handlers.AsyncHandler<RpcV2CborListsRequest, RpcV2CborListsResult> asyncHandler);

    /**
     * @param simpleScalarPropertiesRequest
     * @return A Java Future containing the result of the SimpleScalarProperties operation returned by the service.
     * @sample RpcV2ProtocolAsync.SimpleScalarProperties
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/SimpleScalarProperties"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<SimpleScalarPropertiesResult> simpleScalarPropertiesAsync(SimpleScalarPropertiesRequest simpleScalarPropertiesRequest);

    /**
     * @param simpleScalarPropertiesRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the SimpleScalarProperties operation returned by the service.
     * @sample RpcV2ProtocolAsyncHandler.SimpleScalarProperties
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/SimpleScalarProperties"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<SimpleScalarPropertiesResult> simpleScalarPropertiesAsync(SimpleScalarPropertiesRequest simpleScalarPropertiesRequest,
            com.amazonaws.handlers.AsyncHandler<SimpleScalarPropertiesRequest, SimpleScalarPropertiesResult> asyncHandler);

}
